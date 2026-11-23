package de.sujuc.lobby;

import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** All calls execute on one dedicated worker; SQL transactions protect shared MySQL accounts. */
public final class Store implements AutoCloseable {
    public record Profile(UUID id,String name,long coins,int tickets,long playtime,int visibility,boolean jump,boolean messages,String particle,String balloon,String gadget,String head) {}
    public record Result(boolean success,Profile profile) {}
    public record Transfer(boolean success,Profile sender,Profile recipient) {}
    public record LeaderboardEntry(UUID id,String name,long value) {}
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"LobbySystem-SQL");t.setDaemon(true);return t;});
    private final String url,user,password;
    private Connection connection;
    public Store(Settings.Database cfg,Path data) {
        boolean mysql=cfg.type.equals("mysql");
        url=mysql?"jdbc:mysql://"+cfg.host+":"+cfg.port+"/"+cfg.name+"?useSSL="+cfg.useSsl+"&connectTimeout=5000&socketTimeout=10000&characterEncoding=UTF-8":"jdbc:sqlite:"+data.resolve("players.db");
        user=mysql?cfg.user:""; password=mysql?cfg.password:"";
    }
    private Connection db() throws SQLException {
        if(connection==null || connection.isClosed() || !connection.isValid(2)) {
            connection=DriverManager.getConnection(url,user,password);
            if(url.startsWith("jdbc:sqlite")) try(Statement st=connection.createStatement()) { st.execute("PRAGMA busy_timeout=10000"); st.execute("PRAGMA journal_mode=WAL"); }
        }
        return connection;
    }
    interface Job<T> { T run(Connection c) throws Exception; }
    private <T> CompletableFuture<T> task(Job<T> job) {
        return CompletableFuture.supplyAsync(()->{ try{return job.run(db());}catch(Exception e){throw new CompletionException(e);} },worker);
    }
    public CompletableFuture<Void> init() { return task(c->{
        try(Statement s=c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS lobby_players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) NOT NULL, coins BIGINT NOT NULL DEFAULT 0, tickets INTEGER NOT NULL DEFAULT 0, playtime BIGINT NOT NULL DEFAULT 0, visibility INTEGER NOT NULL DEFAULT 0, jump INTEGER NOT NULL DEFAULT 1, messages INTEGER NOT NULL DEFAULT 1, particle VARCHAR(40) NOT NULL DEFAULT '', balloon VARCHAR(40) NOT NULL DEFAULT '', gadget VARCHAR(40) NOT NULL DEFAULT '', head VARCHAR(40) NOT NULL DEFAULT '', daily_normal BIGINT NOT NULL DEFAULT -1, daily_premium BIGINT NOT NULL DEFAULT -1)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS lobby_unlocks (uuid VARCHAR(36) NOT NULL, cosmetic VARCHAR(80) NOT NULL, PRIMARY KEY(uuid,cosmetic))");
        } return null;
    }); }
    private int update(Connection c,String sql,Object...args) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]); return s.executeUpdate(); }
    }
    public CompletableFuture<Profile> load(UUID id,String name) { return task(c->{
        try { update(c,"INSERT INTO lobby_players(uuid,name) VALUES(?,?)",id.toString(),name); }
        catch(SQLException e) { if(!exists(c,id)) throw e; }
        update(c,"UPDATE lobby_players SET name=? WHERE uuid=?",name,id.toString()); return read(c,id);
    }); }
    private boolean exists(Connection c,UUID id) throws SQLException { try(PreparedStatement s=c.prepareStatement("SELECT uuid FROM lobby_players WHERE uuid=?")){s.setString(1,id.toString());try(ResultSet r=s.executeQuery()){return r.next();}} }
    private Profile read(Connection c,UUID id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM lobby_players WHERE uuid=?")) {
            s.setString(1,id.toString()); try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new SQLException("Spieler unbekannt");
                return new Profile(id,r.getString("name"),r.getLong("coins"),r.getInt("tickets"),r.getLong("playtime"),r.getInt("visibility"),r.getInt("jump")!=0,r.getInt("messages")!=0,r.getString("particle"),r.getString("balloon"),r.getString("gadget"),r.getString("head"));
            }
        }
    }
    public CompletableFuture<Profile> refresh(UUID id) { return task(c->read(c,id)); }
    public CompletableFuture<Profile> find(String name) { return task(c->{
        try(PreparedStatement s=c.prepareStatement("SELECT uuid FROM lobby_players WHERE LOWER(name)=LOWER(?)")){s.setString(1,name);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Spieler muss mindestens einmal beigetreten sein.");return read(c,UUID.fromString(r.getString(1)));}}
    }); }
    public CompletableFuture<Result> coins(UUID id,long delta) { return task(c->{
        if(delta==Long.MIN_VALUE)throw new IllegalArgumentException("Betrag ungültig");
        int n=delta>=0?update(c,"UPDATE lobby_players SET coins=coins+? WHERE uuid=? AND coins<=?",delta,id.toString(),Long.MAX_VALUE-delta):update(c,"UPDATE lobby_players SET coins=coins+? WHERE uuid=? AND coins>=?",delta,id.toString(),-delta);
        return new Result(n==1,read(c,id));
    }); }
    public CompletableFuture<Transfer> transfer(UUID sender,UUID recipient,long amount) { return task(c->{
        if(sender.equals(recipient)||amount<=0)throw new IllegalArgumentException("Eine Überweisung benötigt zwei verschiedene Spieler und einen positiven Betrag.");
        c.setAutoCommit(false);
        try {
            // Every server locks the two accounts in the same order, avoiding opposite-transfer deadlocks.
            List<String> accounts=new ArrayList<>(List.of(sender.toString(),recipient.toString()));Collections.sort(accounts);
            for(String account:accounts)update(c,"UPDATE lobby_players SET coins=coins WHERE uuid=?",account);
            Profile from=read(c,sender),to=read(c,recipient);
            if(from.coins()<amount||to.coins()>Long.MAX_VALUE-amount){c.rollback();return new Transfer(false,from,to);}
            int debit=update(c,"UPDATE lobby_players SET coins=coins-? WHERE uuid=? AND coins>=?",amount,sender.toString(),amount);
            int credit=update(c,"UPDATE lobby_players SET coins=coins+? WHERE uuid=? AND coins<=?",amount,recipient.toString(),Long.MAX_VALUE-amount);
            if(debit!=1||credit!=1)throw new SQLException("Überweisung konnte nicht vollständig verbucht werden");
            c.commit();return new Transfer(true,read(c,sender),read(c,recipient));
        }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
    }); }
    public CompletableFuture<List<LeaderboardEntry>> top(String category,int limit){return task(c->{
        if(!List.of("coins","playtime").contains(category)||limit<1||limit>100)throw new IllegalArgumentException("Rangliste: coins oder playtime, Limit 1–100");
        try(PreparedStatement s=c.prepareStatement("SELECT uuid,name,"+category+" FROM lobby_players ORDER BY "+category+" DESC, LOWER(name) ASC, uuid ASC LIMIT ?")){
            s.setInt(1,limit);List<LeaderboardEntry> rows=new ArrayList<>();
            try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new LeaderboardEntry(UUID.fromString(r.getString(1)),r.getString(2),r.getLong(3)));}
            return List.copyOf(rows);
        }
    });}
    public CompletableFuture<Result> reward(UUID id,boolean premium,long day,int coins,int tickets) { return task(c->{
        String col=premium?"daily_premium":"daily_normal";
        int n=update(c,"UPDATE lobby_players SET coins=coins+?,tickets=tickets+?,"+col+"=? WHERE uuid=? AND "+col+"<? AND coins<=? AND tickets<=?",coins,tickets,day,id.toString(),day,Long.MAX_VALUE-coins,Integer.MAX_VALUE-tickets);
        return new Result(n==1,read(c,id));
    }); }
    public CompletableFuture<Result> lottery(UUID id,int prize) { return task(c->{
        int n=update(c,"UPDATE lobby_players SET tickets=tickets-1,coins=coins+? WHERE uuid=? AND tickets>0 AND coins<=?",prize,id.toString(),Long.MAX_VALUE-prize);return new Result(n==1,read(c,id));
    }); }
    public CompletableFuture<Result> select(UUID id,String category,String value,int price) { return task(c->{
        if(!List.of("particle","balloon","gadget","head").contains(category))throw new IllegalArgumentException("Kategorie ungültig");
        c.setAutoCommit(false);
        try {
            // An UPDATE acquires the player row lock before ownership and balance are inspected.
            update(c,"UPDATE lobby_players SET coins=coins WHERE uuid=?",id.toString());
            boolean owned=value.isEmpty();
            try(PreparedStatement s=c.prepareStatement("SELECT cosmetic FROM lobby_unlocks WHERE uuid=? AND cosmetic=?")) {s.setString(1,id.toString());s.setString(2,category+":"+value);try(ResultSet r=s.executeQuery()){owned|=r.next();}}
            if(!owned) {
                if(update(c,"UPDATE lobby_players SET coins=coins-? WHERE uuid=? AND coins>=?",price,id.toString(),price)!=1){c.rollback();return new Result(false,read(c,id));}
                update(c,"INSERT INTO lobby_unlocks(uuid,cosmetic) VALUES(?,?)",id.toString(),category+":"+value);
            }
            update(c,"UPDATE lobby_players SET "+category+"=? WHERE uuid=?",value,id.toString());c.commit();return new Result(true,read(c,id));
        } catch(Exception e){c.rollback();throw e;} finally{c.setAutoCommit(true);}
    }); }
    public CompletableFuture<Profile> setting(UUID id,String field,int value) { return task(c->{
        if(!List.of("visibility","jump","messages").contains(field))throw new IllegalArgumentException("Einstellung ungültig");
        if(value<0||value>(field.equals("visibility")?2:1))throw new IllegalArgumentException("Wert ungültig");
        update(c,"UPDATE lobby_players SET "+field+"=? WHERE uuid=?",value,id.toString()); return read(c,id);
    }); }
    public CompletableFuture<Profile> playtime(UUID id,long seconds) { return task(c->{ update(c,"UPDATE lobby_players SET playtime=playtime+? WHERE uuid=?",Math.max(0,seconds),id.toString());return read(c,id); }); }
    public void close() {
        worker.execute(()->{try{if(connection!=null)connection.close();}catch(SQLException ignored){}});worker.shutdown();
        try { if(!worker.awaitTermination(15,TimeUnit.SECONDS))worker.shutdownNow(); } catch(InterruptedException e){Thread.currentThread().interrupt();}
    }
}
