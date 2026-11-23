package de.sujuc.lobby;

import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public final class LobbyPlugin extends JavaPlugin implements CommandExecutor,TabCompleter,Listener {
    Settings cfg; Store store; Menus menus; Effects effects; CommunityFeatures community; UiPresentation ui; PublicKey publicKey;
    boolean databaseReady, licensed;
    volatile boolean stopping;
    NamespacedKey actionKey,npcKey;
    final Map<UUID,Session> sessions=new HashMap<>();
    final Set<UUID> builders=new HashSet<>();
    final Set<UUID> mutedChat=ConcurrentHashMap.newKeySet();
    final Map<String,LivingEntity> npcs=new HashMap<>();
    final List<Chunk> npcChunks=new ArrayList<>();
    List<Rank> ranks;
    record Rank(String permission,String display,int priority) {}
    static final class Session {
        final Player player; Store.Profile profile;
        final ItemStack[] contents,armor; final ItemStack offhand;
        final GameMode mode; final boolean flight,flying; final Scoreboard scoreboard;
        long playedAt=System.currentTimeMillis(); long gadgetAt;
        long lastPayNanos=System.nanoTime()-3_600_000_000_000L, lastLaunchNanos=lastPayNanos;
        String presentationKey="";
        Session(Player p){player=p;contents=p.getInventory().getStorageContents().clone();armor=p.getInventory().getArmorContents().clone();offhand=p.getInventory().getItemInOffHand().clone();mode=p.getGameMode();flight=p.getAllowFlight();flying=p.isFlying();scoreboard=p.getScoreboard();}
    }
    public void onEnable() {
        try {
            cfg=Settings.load(getDataFolder().toPath().resolve("config.json"));
            ui=new UiPresentation(this);
            if(!Material.matchMaterial(cfg.launchPads.material).isBlock())throw new IllegalArgumentException("launchPads.material muss ein Blockmaterial sein");
            for(Settings.VisualProfile profile:List.of(cfg.design.classic,cfg.design.modern)){
                List<String> materials=new ArrayList<>(profile.icons.values());materials.add(profile.background);materials.add(profile.accent);
                for(String choice:materials)if(!VersionStyles.material(choice).isItem())throw new IllegalArgumentException("Designmaterial ist kein Item: "+choice);
            }
            for(String item:cfg.navigatorItems)if(!VersionStyles.material(item.split(";",5)[1]).isItem())throw new IllegalArgumentException("Navigator-Material ist kein Item");
            Path ranksPath=getDataFolder().toPath().resolve("ranks.json");
            if(!Files.exists(ranksPath))Files.writeString(ranksPath,Settings.JSON.toJson(List.of(new Rank("lobby.rank.owner","&cLeitung",100),new Rank("lobby.rank.premium","&6Premium",50),new Rank("","&7Spieler",0))));
            try(Reader reader=Files.newBufferedReader(ranksPath)){ranks=Settings.JSON.fromJson(reader,new TypeToken<List<Rank>>(){}.getType());}
            if(ranks==null || ranks.isEmpty())throw new IllegalArgumentException("ranks.json darf nicht leer sein");
            ranks.sort(Comparator.comparingInt(Rank::priority).reversed());
            try(InputStream in=getResource("license-public.der")){if(in==null)throw new IOException("Lizenz-Public-Key fehlt");publicKey=License.publicKey(in.readAllBytes());}
            actionKey=new NamespacedKey(this,"action");npcKey=new NamespacedKey(this,"npc");
            checkLicense();store=new Store(cfg.database,getDataFolder().toPath());menus=new Menus(this);effects=new Effects(this);community=new CommunityFeatures(this);
            String payPermission=cfg.permission("pay");
            if(Bukkit.getPluginManager().getPermission(payPermission)==null)Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(payPermission,org.bukkit.permissions.PermissionDefault.TRUE));
            for(String cmd:getDescription().getCommands().keySet()){Objects.requireNonNull(getCommand(cmd)).setExecutor(this);getCommand(cmd).setTabCompleter(this);}
            Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getPluginManager().registerEvents(new Protection(this),this);
            Bukkit.getPluginManager().registerEvents(community,this);
            getServer().getMessenger().registerOutgoingPluginChannel(this,"BungeeCord");
            future(store.init(),unused->{databaseReady=true;if(licensed)startLobby();},null);
            Bukkit.getScheduler().runTaskTimer(this,()->{
                if(licensed&&!checkLicense()){for(Session s:new ArrayList<>(sessions.values()))leave(s.player);removeNpcs();}
                if(!active())return;
                community.tick();
                for(Session s:new ArrayList<>(sessions.values()))if(s.profile!=null){display(s);if(s.player.isOnGround()&&s.profile.jump()&&!builders.contains(s.player.getUniqueId()))s.player.setAllowFlight(true);}
            },20,20);
            Bukkit.getScheduler().runTaskTimer(this,()->{
                for(Session s:new ArrayList<>(sessions.values()))if(s.profile!=null)flushPlaytime(s,true);
            },200,200);
            Bukkit.getScheduler().runTaskTimer(this,()->{if(active())effects.tick();},5,5);
            getLogger().info("Server-ID: "+cfg.serverId+" | Lizenz: "+(licensed?"gültig":"nicht aktiviert"));
        } catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"Start fehlgeschlagen",e);Bukkit.getPluginManager().disablePlugin(this);}
    }
    boolean checkLicense(){try{License.verify(cfg.license,publicKey,cfg.serverId,Instant.now());return licensed=true;}catch(Exception e){return licensed=false;}}
    boolean active(){return databaseReady&&licensed&&!stopping;}
    boolean lobby(Player p){return active()&&p.getWorld().getName().equals(cfg.world);}
    static String color(String text){return VersionStyles.color(text);}
    void message(CommandSender p,String key){ui.chat(p,cfg.prefix+cfg.messages.getOrDefault(key,key));}
    void message(CommandSender p,String key,String...vars){String m=cfg.messages.getOrDefault(key,key);for(int i=0;i<vars.length;i+=2)m=m.replace("{"+vars[i]+"}",vars[i+1]);ui.chat(p,cfg.prefix+m);}
    boolean permission(CommandSender s,String suffix){if(s.hasPermission(cfg.permission(suffix)))return true;message(s,"noPermission");return false;}
    void save(){try{cfg.save(getDataFolder().toPath().resolve("config.json"));}catch(IOException e){throw new IllegalStateException("Config konnte nicht gespeichert werden",e);}}
    <T> void future(CompletableFuture<T> f,Consumer<T> done,CommandSender receiver){f.whenComplete((value,error)->{
        if(stopping)return;
        Bukkit.getScheduler().runTask(this,()->{if(error!=null){
            Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();
            if(cause instanceof IllegalArgumentException){if(receiver!=null)ui.chat(receiver,cfg.prefix+"&c"+cause.getMessage());}
            else{getLogger().log(java.util.logging.Level.SEVERE,"Datenbankoperation fehlgeschlagen",error);if(receiver!=null)message(receiver,"databaseError");}
        }else done.accept(value);});
    });}
    void profile(Session s,CompletableFuture<Store.Profile> job){future(job,p->{if(sessions.get(s.player.getUniqueId())==s){boolean first=s.profile==null;s.profile=p;apply(s);if(first)community.welcome(s);}},s.player);}
    void refreshLocal(Store.Profile profile){Session current=sessions.get(profile.id());if(current!=null&&current.profile!=null){current.profile=profile;apply(current);}}
    void result(Session s,CompletableFuture<Store.Result> job,String success,String failure,String...vars){future(job,r->{if(sessions.get(s.player.getUniqueId())==s){s.profile=r.profile();apply(s);message(s.player,r.success()?success:failure,vars);}},s.player);}
    void startLobby(){spawnNpcs();for(Player p:Bukkit.getOnlinePlayers())if(lobby(p))join(p);}
    void join(Player p){
        if(sessions.containsKey(p.getUniqueId()))return;
        Session s=new Session(p);sessions.put(p.getUniqueId(),s);
        p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);
        p.setGameMode(GameMode.ADVENTURE);p.setAllowFlight(false);hotbar(p);teleport(p,"spawn");
        profile(s,store.load(p.getUniqueId(),p.getName()));
    }
    void leave(Player p){
        Session s=sessions.remove(p.getUniqueId());builders.remove(p.getUniqueId());mutedChat.remove(p.getUniqueId());effects.remove(p.getUniqueId());
        if(s==null)return;
        if(s.profile!=null)flushPlaytime(s,false);
        p.closeInventory();p.getInventory().setStorageContents(s.contents);p.getInventory().setArmorContents(s.armor);p.getInventory().setItemInOffHand(s.offhand);
        p.setGameMode(s.mode);p.setAllowFlight(s.flight);if(s.flight)p.setFlying(s.flying);p.setScoreboard(s.scoreboard);p.setPlayerListName(p.getName());p.setPlayerListHeaderFooter("","");
        for(Player other:Bukkit.getOnlinePlayers()){p.showPlayer(this,other);other.showPlayer(this,p);}
        String teamName="p"+p.getUniqueId().toString().replace("-","").substring(0,14);
        for(Session viewer:sessions.values()){Team team=viewer.player.getScoreboard().getTeam(teamName);if(team!=null)team.unregister();}
    }
    void flushPlaytime(Session s,boolean refresh){long now=System.currentTimeMillis();long secs=Math.max(0,(now-s.playedAt)/1000);s.playedAt+=secs*1000;if(secs>0){if(refresh)profile(s,store.playtime(s.player.getUniqueId(),secs));else store.playtime(s.player.getUniqueId(),secs);}}
    void apply(Session s){
        Player p=s.player;if(!lobby(p))return;
        if(s.profile.messages())mutedChat.remove(p.getUniqueId());else mutedChat.add(p.getUniqueId());
        if(!builders.contains(p.getUniqueId())){p.getInventory().setHelmet(s.profile.head().isEmpty()?null:new ItemStack(Material.valueOf(s.profile.head())));p.setAllowFlight(s.profile.jump()&&p.isOnGround());}
        effects.balloon(s);visibility();display(s);
    }
    void hotbar(Player p){
        p.getInventory().setItem(0,menus.item(p,ui.icon(p,"navigator"),"&bNavigator","&7Dein nächstes Ziel wartet.\n&8Rechtsklick zum Öffnen","open:navigator"));
        p.getInventory().setItem(1,menus.item(p,Material.BLAZE_ROD,"&bDein Gadget","&7Wähle dein Gadget in den Extras.\n&8Rechtsklick zum Benutzen","gadget"));
        p.getInventory().setItem(2,menus.item(p,ui.icon(p,"lottery"),"&eLotterie","&7Nutze deine Lose und gewinne Coins.\n&8Rechtsklick zum Öffnen","open:lottery"));
        p.getInventory().setItem(4,menus.item(p,ui.icon(p,"visibility"),"&bSpielersichtbarkeit","&7Alle, VIPs oder niemand.\n&8Rechtsklick zum Einstellen","open:visibility"));
        p.getInventory().setItem(6,menus.item(p,ui.icon(p,"settings"),"&bEinstellungen","&7Deine Lobby. Deine Einstellungen.\n&8Rechtsklick zum Öffnen","open:settings"));
        p.getInventory().setItem(7,menus.item(p,ui.icon(p,"cosmetics"),"&dExtras","&7Partikel, Ballone, Gadgets und Köpfe.\n&8Rechtsklick zum Öffnen","open:cosmetics"));
        p.getInventory().setItem(8,menus.item(p,ui.icon(p,"profile"),"&bDein Profil","&7Statistiken, Belohnungen und Ranglisten.\n&8Rechtsklick zum Öffnen","open:profile"));
    }
    void visibility(){for(Session viewer:sessions.values())if(viewer.profile!=null)for(Session target:sessions.values()){
        if(viewer==target)continue;boolean show=viewer.profile.visibility()==0 || viewer.profile.visibility()==1&&target.player.hasPermission(cfg.permission("visibility"));
        if(show)viewer.player.showPlayer(this,target.player);else viewer.player.hidePlayer(this,target.player);
    }}
    String rank(Player p){for(Rank r:ranks)if(r.permission().isEmpty()||p.hasPermission(r.permission()))return ui.text(r.display());return ui.text("&7Spieler");}
    String placeholders(Session s,String str){ZonedDateTime now=ZonedDateTime.now(ZoneId.of(cfg.timeZone));return ui.text(str.replace("{player}",s.player.getName()).replace("{coins}",String.format(Locale.GERMANY,"%,d",s.profile.coins())).replace("{tickets}",""+s.profile.tickets()).replace("{hours}",""+(s.profile.playtime()/3600)).replace("{playtime}",CommunityFeatures.duration(s.profile.playtime())).replace("{rank}",rank(s.player)).replace("{online}",""+sessions.size()).replace("{server_version}",VersionStyles.version(Bukkit.getBukkitVersion()).map(Object::toString).orElse(Bukkit.getBukkitVersion())).replace("{client_version}",ui.clientVersion(s.player)).replace("{ui_profile}",ui.profile(s.player)).replace("{date}",now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).replace("{time}",now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));}
    void display(Session s){
        Player p=s.player;Scoreboard board=p.getScoreboard();
        String key=ui.profile(p)+":"+ui.font(p);
        if(!key.equals(s.presentationKey)){s.presentationKey=key;if(!builders.contains(p.getUniqueId()))hotbar(p);}
        if(board==s.scoreboard){board=Bukkit.getScoreboardManager().getNewScoreboard();p.setScoreboard(board);}
        Objective obj=board.getObjective("lobby");if(obj==null){obj=board.registerNewObjective("lobby",Criteria.DUMMY,ui.text(cfg.scoreboardTitle));obj.setDisplaySlot(DisplaySlot.SIDEBAR);ui.hideNumbers(obj);}
        ui.objective(p,obj,placeholders(s,cfg.scoreboardTitle));
        for(int i=0;i<cfg.scoreboardLines.size();i++){
            String entry=ChatColor.values()[i].toString()+ChatColor.RESET;String name="line"+i;Team team=board.getTeam(name);if(team==null){team=board.registerNewTeam(name);team.addEntry(entry);}
            ui.prefix(p,team,placeholders(s,cfg.scoreboardLines.get(i)));obj.getScore(entry).setScore(cfg.scoreboardLines.size()-i);
        }
        for(Session target:sessions.values()) {
            String name="p"+target.player.getUniqueId().toString().replace("-","").substring(0,14);Team team=board.getTeam(name);
            if(team==null)team=board.registerNewTeam(name);ui.prefix(p,team,rank(target.player)+" &8{separator} &7");team.addEntry(target.player.getName());
        }
        ui.tab(p,rank(p)+" &8{separator} &f"+p.getName(),placeholders(s,cfg.tabHeader),placeholders(s,cfg.tabFooter));
        ui.actionBar(p,placeholders(s,cfg.design.actionBar));
    }
    void teleport(Player p,String target){
        if(target.startsWith("connect:")){
            String server=target.substring(8);if(server.isBlank())return;
            try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeUTF("Connect");out.writeUTF(server);p.sendPluginMessage(this,"BungeeCord",bytes.toByteArray());message(p,"connecting","server",server);}catch(IOException e){getLogger().warning(e.toString());}return;
        }
        Settings.Spot spot=target.equalsIgnoreCase("spawn")?cfg.spawn:cfg.warps.get(target.toLowerCase(Locale.ROOT));
        Location l=spot==null?null:spot.location();if(l==null){message(p,"warpMissing");return;}p.teleport(l);
    }
    void spawnNpcs(){removeNpcs();Set<Chunk> cleaned=new HashSet<>();for(var entry:cfg.npcs.entrySet()){
        Location loc=entry.getValue().location();if(loc==null||!loc.getWorld().getName().equals(cfg.world))continue;
        Chunk chunk=loc.getChunk();
        if(cleaned.add(chunk)){
            chunk.addPluginChunkTicket(this);npcChunks.add(chunk);
            for(Entity e:chunk.getEntities())if(e.getPersistentDataContainer().has(npcKey,PersistentDataType.STRING))e.remove();
        }
        LivingEntity npc=(LivingEntity)loc.getWorld().spawnEntity(loc,entry.getKey().equals("reward")?EntityType.WITCH:EntityType.VILLAGER);
        npc.setAI(false);npc.setInvulnerable(true);npc.setSilent(true);npc.setCollidable(false);npc.setGravity(false);npc.setPersistent(false);npc.setRemoveWhenFarAway(false);
        npc.setCustomName(ui.text(entry.getKey().equals("reward")?cfg.design.rewardNpc:cfg.design.settingsNpc));npc.setCustomNameVisible(true);npc.getPersistentDataContainer().set(npcKey,PersistentDataType.STRING,entry.getKey());npcs.put(entry.getKey(),npc);
    }}
    void removeNpcs(){for(Entity e:npcs.values())e.remove();npcs.clear();for(Chunk c:npcChunks)c.removePluginChunkTicket(this);npcChunks.clear();}
    @EventHandler public void onJoin(PlayerJoinEvent e){if(lobby(e.getPlayer())){e.setJoinMessage(null);join(e.getPlayer());}}
    @EventHandler public void onQuit(PlayerQuitEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId())){e.setQuitMessage(null);leave(e.getPlayer());}}
    @EventHandler public void world(PlayerChangedWorldEvent e){if(lobby(e.getPlayer()))join(e.getPlayer());else leave(e.getPlayer());}
    @EventHandler public void load(WorldLoadEvent e){if(active()&&e.getWorld().getName().equals(cfg.world))spawnNpcs();}
    @EventHandler public void fly(PlayerToggleFlightEvent e){Player p=e.getPlayer();Session s=sessions.get(p.getUniqueId());if(lobby(p)&&!builders.contains(p.getUniqueId())){e.setCancelled(true);p.setFlying(false);p.setAllowFlight(false);if(s!=null&&s.profile!=null&&s.profile.jump())p.setVelocity(p.getLocation().getDirection().multiply(.9).setY(.8));}}
    @EventHandler public void respawn(PlayerRespawnEvent e){if(lobby(e.getPlayer())&&cfg.spawn!=null&&cfg.spawn.location()!=null)e.setRespawnLocation(cfg.spawn.location());}
    @Override public void onDisable(){stopping=true;if(effects!=null)for(Session s:new ArrayList<>(sessions.values()))leave(s.player);removeNpcs();if(store!=null)store.close();}

    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
        try {
            String name=cmd.getName();
            if(name.equals("lobbylicense")){licenseCommand(sender,args);return true;}
            if(name.equals("lobbyui")){ui.command(sender,args);return true;}
            if(!active()){message(sender,"unlicensed");return true;}
            if(name.equals("lobbycoins")){coinsCommand(sender,args);return true;}
            if(!(sender instanceof Player p)){sender.sendMessage("Nur als Spieler möglich.");return true;}
            if(!lobby(p)){ui.chat(p,cfg.prefix+"&cDieser Befehl ist nur in der Lobby-Welt verfügbar: "+cfg.world);return true;}
            switch(name){
                case "build" -> {if(!permission(p,"build"))break;boolean on=builders.add(p.getUniqueId());if(!on)builders.remove(p.getUniqueId());p.closeInventory();p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.setGameMode(on?GameMode.CREATIVE:GameMode.ADVENTURE);p.setAllowFlight(on);if(!on){hotbar(p);Session s=sessions.get(p.getUniqueId());if(s!=null&&s.profile!=null)apply(s);}message(p,"build","state",on?"aktiviert":"deaktiviert");}
                case "setspawn" -> {if(permission(p,"setup")){cfg.spawn=new Settings.Spot(p.getLocation());save();message(p,"saved");}}
                case "setwarp" -> {if(!permission(p,"setup"))break;if(args.length<1||args.length>2||!args[0].matches("[A-Za-z0-9_-]{1,40}")||args[0].equalsIgnoreCase("spawn")||args.length==2&&!args[1].equalsIgnoreCase("remove"))throw new IllegalArgumentException("/setwarp <Name> [remove] (Spawn mit /setspawn)");String warp=args[0].toLowerCase(Locale.ROOT);if(args.length==2)cfg.warps.remove(warp);else cfg.warps.put(warp,new Settings.Spot(p.getLocation()));save();message(p,"saved");}
                case "setreward","setsettings" -> {if(!permission(p,"setup"))break;if(args.length>1||args.length==1&&!args[0].equalsIgnoreCase("remove"))throw new IllegalArgumentException("/"+name+" [remove]");String type=name.equals("setreward")?"reward":"settings";if(args.length==1)cfg.npcs.remove(type);else cfg.npcs.put(type,new Settings.Spot(p.getLocation()));save();spawnNpcs();message(p,"saved");}
                case "tpcoord" -> {if(!permission(p,"teleport"))break;if(args.length!=3&&args.length!=4)throw new IllegalArgumentException("/tpcoord <x> <y> <z> [Welt]");World w=args.length==4?Bukkit.getWorld(args[3]):p.getWorld();if(w==null)throw new IllegalArgumentException("Welt nicht gefunden");double x=Double.parseDouble(args[0]),y=Double.parseDouble(args[1]),z=Double.parseDouble(args[2]);if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>29999984||Math.abs(z)>29999984||y<w.getMinHeight()||y>=w.getMaxHeight())throw new IllegalArgumentException("Koordinaten außerhalb der Weltgrenzen");p.teleport(new Location(w,x,y,z,p.getLocation().getYaw(),p.getLocation().getPitch()));}
                case "lobby" -> teleport(p,"spawn");
                case "coins","pay","lobbytop" -> community.command(p,name,args);
                case "profile" -> menus.open(p,"profile");
                default -> menus.open(p,name);
            }
        }catch(Exception e){ui.chat(sender,cfg.prefix+"&c"+(e.getMessage()==null?"Befehl fehlgeschlagen":e.getMessage()));}
        return true;
    }
    void coinsCommand(CommandSender sender,String[] a){
        if(!permission(sender,"coins"))return;
        if(a.length<2||!List.of("get","add","remove").contains(a[0].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("/lobbycoins add|remove|get <Spieler> [Coins]");
        String op=a[0].toLowerCase(Locale.ROOT);long amount=op.equals("get")?0:a.length==3?Long.parseLong(a[2]):0;
        if(op.equals("get")?a.length!=2:a.length!=3||amount<=0)throw new IllegalArgumentException("Betrag muss eine positive ganze Zahl sein");
        future(store.find(a[1]),found->{if(op.equals("get")){message(sender,"coins","player",found.name(),"coins",""+found.coins());return;}
            future(store.coins(found.id(),op.equals("add")?amount:-amount),r->{Session s=sessions.get(found.id());if(s!=null){s.profile=r.profile();apply(s);}if(!r.success())message(sender,"insufficient");else message(sender,"coins","player",found.name(),"coins",""+r.profile().coins());},sender);
        },sender);
    }
    void licenseCommand(CommandSender sender,String[] a)throws Exception {
        if(a.length==0||a[0].equalsIgnoreCase("status")||a[0].equalsIgnoreCase("request")){ui.chat(sender,cfg.prefix+"&7Server-ID: &f"+cfg.serverId+" &8| &7Lizenz: &f"+(licensed?"gültig":"nicht aktiviert"));return;}
        if(a[0].equalsIgnoreCase("activate")){
            if(!(sender instanceof ConsoleCommandSender)&&!permission(sender,"setup"))return;
            if(a.length!=2)throw new IllegalArgumentException("/lobbylicense activate <Lizenz>");
            License.verify(a[1],publicKey,cfg.serverId,Instant.now());cfg.license=a[1];save();boolean was=licensed;licensed=true;if(!was&&databaseReady)startLobby();message(sender,"saved");return;
        }
        if(a[0].equalsIgnoreCase("create")){
            // No OP, permission or console bypass. Creation is only allowed on a directly authenticated issuer server.
            if(!(sender instanceof Player p)||!getServer().getOnlineMode()||!p.getName().equals("SujucDerBreite"))throw new SecurityException("Nur SujucDerBreite auf einem Server mit online-mode=true kann Lizenzen erstellen.");
            if(a.length!=4)throw new IllegalArgumentException("/lobbylicense create <Kunde> <Tage:0=unbegrenzt> <Server-UUID>");
            Path privatePath=getDataFolder().toPath().resolve("owner/private-key.der");
            if(!Files.isRegularFile(privatePath))throw new IllegalArgumentException("Owner-Schlüssel fehlt. Nur auf dem privaten Aussteller-Server installieren.");
            Path ownerIdentity=privatePath.resolveSibling("owner-uuid.txt");
            // Pin UUID after the first authenticated login under the fixed owner name.
            if(!Files.exists(ownerIdentity))Files.writeString(ownerIdentity,p.getUniqueId().toString(),StandardOpenOption.CREATE_NEW);
            if(!UUID.fromString(Files.readString(ownerIdentity).trim()).equals(p.getUniqueId()))throw new SecurityException("Owner-UUID stimmt nicht überein.");
            String token=License.issue(License.privateKey(privatePath),a[1],a[3],Integer.parseInt(a[2]),Instant.now());
            Files.createDirectories(getDataFolder().toPath().resolve("issued"));
            Files.writeString(getDataFolder().toPath().resolve("issued/"+UUID.randomUUID()+".txt"),token);
            p.sendMessage("Lizenz (auch im issued-Ordner gespeichert):");p.sendMessage(token);return;
        }
        throw new IllegalArgumentException("/lobbylicense status|request|activate|create");
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args){
        List<String> choices=new ArrayList<>();String name=command.getName();
        if(name.equals("lobbycoins")&&sender.hasPermission(cfg.permission("coins"))){if(args.length==1)choices.addAll(List.of("get","add","remove"));if(args.length==2)Bukkit.getOnlinePlayers().forEach(p->choices.add(p.getName()));}
        if(name.equals("lobbytop")&&args.length==1)choices.addAll(List.of("coins","playtime"));
        if(name.equals("pay")&&args.length==1&&sender.hasPermission(cfg.permission("pay")))Bukkit.getOnlinePlayers().stream().filter(p->!p.getName().equals(sender.getName())&&(!(sender instanceof Player viewer)||viewer.canSee(p))).forEach(p->choices.add(p.getName()));
        if((name.equals("setreward")||name.equals("setsettings"))&&args.length==1&&sender.hasPermission(cfg.permission("setup")))choices.add("remove");
        if(name.equals("setwarp")&&args.length==2&&sender.hasPermission(cfg.permission("setup")))choices.add("remove");
        if(name.equals("lobbylicense")&&args.length==1)choices.addAll(List.of("status","request","activate","create"));
        if(name.equals("lobbyui")&&sender.hasPermission(cfg.permission("setup"))){if(args.length==1)choices.addAll(List.of("status","profile","font"));if(args.length==2&&args[0].equals("profile"))choices.addAll(List.of("auto","classic","modern"));if(args.length==2&&args[0].equals("font"))choices.addAll(List.of("auto","minecraft:default","minecraft:uniform"));}
        String partial=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(partial)).toList();
    }
}
