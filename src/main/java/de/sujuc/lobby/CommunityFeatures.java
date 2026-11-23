package de.sujuc.lobby;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Player-facing community features; all Bukkit work runs on the main server thread. */
final class CommunityFeatures implements Listener {
    private final LobbyPlugin plugin;
    private int announcementSeconds, announcementIndex;

    CommunityFeatures(LobbyPlugin plugin) { this.plugin=plugin; }

    void welcome(LobbyPlugin.Session session) {
        Settings.Welcome cfg=plugin.cfg.welcome;
        if(!cfg.enabled)return;
        Player player=session.player;
        player.sendTitle(plugin.placeholders(session,cfg.title),plugin.placeholders(session,cfg.subtitle),10,60,20);
        if(session.profile.messages()&&!cfg.message.isBlank())player.sendMessage(plugin.placeholders(session,plugin.cfg.prefix+cfg.message));
    }

    void tick() {
        Settings.Announcements cfg=plugin.cfg.announcements;
        if(!cfg.enabled||cfg.messages.isEmpty())return;
        if(++announcementSeconds<cfg.intervalSeconds)return;
        announcementSeconds=0;
        String text=cfg.messages.get(announcementIndex++%cfg.messages.size());
        // Bound the index for long-running servers.
        if(announcementIndex>=cfg.messages.size())announcementIndex=0;
        for(LobbyPlugin.Session session:plugin.sessions.values()) {
            if(session.profile!=null&&session.profile.messages())session.player.sendMessage(plugin.placeholders(session,plugin.cfg.prefix+text));
        }
    }

    void command(Player player,String command,String[] args) {
        LobbyPlugin.Session session=plugin.sessions.get(player.getUniqueId());
        if(session==null||session.profile==null){plugin.message(player,"loading");return;}
        switch(command) {
            case "coins" -> {
                if(args.length!=0)throw new IllegalArgumentException("/coins");
                plugin.future(plugin.store.refresh(player.getUniqueId()),profile->{
                    if(plugin.sessions.get(player.getUniqueId())!=session)return;
                    plugin.refreshLocal(profile);plugin.message(player,"balance","coins",format(profile.coins()));
                },player);
            }
            case "pay" -> pay(session,args);
            case "lobbytop" -> {
                if(args.length>1)throw new IllegalArgumentException("/lobbytop [coins|playtime]");
                plugin.menus.leaderboard(player,args.length==0?"coins":args[0].toLowerCase(Locale.ROOT));
            }
            default -> throw new IllegalArgumentException("Unbekannter Befehl");
        }
    }

    private void pay(LobbyPlugin.Session session,String[] args) {
        Player sender=session.player;Settings.Transfers cfg=plugin.cfg.transfers;
        if(!cfg.enabled){plugin.message(sender,"transferDisabled");return;}
        if(!plugin.permission(sender,"pay"))return;
        if(args.length!=2||!args[0].matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("/pay <Spieler> <Coins>");
        long amount;
        try{amount=Long.parseLong(args[1]);}catch(NumberFormatException error){throw new IllegalArgumentException("Coins müssen als ganze Zahl angegeben werden.");}
        if(amount<1||amount>cfg.maximumAmount)throw new IllegalArgumentException("Der Betrag muss zwischen 1 und "+cfg.maximumAmount+" liegen.");
        if(args[0].equalsIgnoreCase(sender.getName()))throw new IllegalArgumentException("Du kannst dir selbst keine Coins überweisen.");
        long now=System.nanoTime();
        if(now-session.lastPayNanos<cfg.cooldownSeconds*1_000_000_000L){plugin.message(sender,"transferCooldown");return;}
        session.lastPayNanos=now;
        plugin.future(plugin.store.find(args[0]),recipient->{
            if(plugin.sessions.get(sender.getUniqueId())!=session||!plugin.lobby(sender))return;
            plugin.future(plugin.store.transfer(sender.getUniqueId(),recipient.id(),amount),result->{
                plugin.refreshLocal(result.sender());plugin.refreshLocal(result.recipient());
                if(!result.success()){
                    if(plugin.sessions.get(sender.getUniqueId())==session)plugin.message(sender,"transferFailed");
                    return;
                }
                if(plugin.sessions.get(sender.getUniqueId())==session)plugin.message(sender,"transferSent","player",result.recipient().name(),"coins",format(amount));
                LobbyPlugin.Session receiver=plugin.sessions.get(recipient.id());
                if(receiver!=null)plugin.message(receiver.player,"transferReceived","player",result.sender().name(),"coins",format(amount));
            },sender);
        },sender);
    }

    static String format(long value) { return String.format(Locale.GERMANY,"%,d",value); }
    static String duration(long seconds) { return (seconds/3600)+"h "+(seconds/60%60)+"min"; }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void launch(PlayerMoveEvent event) {
        Player player=event.getPlayer();Settings.LaunchPads cfg=plugin.cfg.launchPads;
        if(!cfg.enabled||!plugin.lobby(player)||plugin.builders.contains(player.getUniqueId())||event.getTo()==null)return;
        Location from=event.getFrom(),to=event.getTo();
        if(from.getX()==to.getX()&&from.getY()==to.getY()&&from.getZ()==to.getZ())return;
        if(to.getY()>from.getY()+.02||to.clone().subtract(0,.1,0).getBlock().getType()!=Material.matchMaterial(cfg.material))return;
        LobbyPlugin.Session session=plugin.sessions.get(player.getUniqueId());
        if(session==null||session.profile==null)return;
        long now=System.nanoTime();
        if(now-session.lastLaunchNanos<cfg.cooldownSeconds*1_000_000_000L)return;
        session.lastLaunchNanos=now;
        Vector direction=to.getDirection().setY(0);
        if(direction.lengthSquared()<.0001)direction=new Vector(0,0,1);
        Vector velocity=direction.normalize().multiply(cfg.horizontal).setY(cfg.vertical);
        // Apply after movement processing; avoid overwriting the current PlayerMoveEvent.
        Bukkit.getScheduler().runTask(plugin,()->{
            if(plugin.sessions.get(player.getUniqueId())!=session||!plugin.lobby(player)||plugin.builders.contains(player.getUniqueId())||player.getLocation().distanceSquared(to)>4)return;
            player.setAllowFlight(false);player.setVelocity(velocity);
            player.playSound(player.getLocation(),Sound.ENTITY_SLIME_JUMP,.7f,1.2f);
        });
    }
}
