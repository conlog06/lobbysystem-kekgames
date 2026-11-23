package de.sujuc.lobby;

import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** CI-only assertions against real server objects. This class is never included in the customer JAR. */
public final class RuntimeProbe extends JavaPlugin implements org.bukkit.event.Listener {
    private Plugin lobby;
    public void onEnable(){
        lobby=Bukkit.getPluginManager().getPlugin("LobbySystem");
        World world=Objects.requireNonNull(Bukkit.getWorld("world"));
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++){world.getBlockAt(x,64,z).setType(Material.STONE,false);for(int y=65;y<70;y++)world.getBlockAt(x,y,z).setType(Material.AIR,false);}
        world.getBlockAt(6,64,0).setType(Material.SLIME_BLOCK,false);world.setSpawnLocation(0,65,0);world.setStorm(false);world.setTime(6000);
        Objects.requireNonNull(getCommand("runtimeprobe")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getScheduler().runTaskTimer(this,()->{try{if(active())getLogger().info("RUNTIME READY");}catch(Exception error){throw new RuntimeException(error);}},20,100);
    }
    private Object field(Object target,String name)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
    private String uiString(Object ui,String method,Player player)throws Exception{Method call=ui.getClass().getDeclaredMethod(method,Player.class);call.setAccessible(true);return (String)call.invoke(ui,player);}
    private String jsonComponent(Object value)throws Exception{
        Class<?> type=Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");Object gson=type.getMethod("gson").invoke(null);
        Method serialize;try{serialize=type.getMethod("serialize",Class.forName("net.kyori.adventure.text.Component"));}catch(NoSuchMethodException erased){serialize=type.getMethod("serialize",Object.class);}
        return (String)serialize.invoke(gson,value);
    }
    private void presentation(Player player,String profile,String font)throws Exception{
        Object ui=field(lobby,"ui");check(uiString(ui,"profile",player).equals(profile),"correct viewer profile");check(uiString(ui,"font",player).equals(font),"correct viewer font");
        var item=player.getInventory().getItem(0);check(item.getType()==(profile.equals("modern")?Material.RECOVERY_COMPASS:Material.COMPASS),"profile hotbar icon");
        String name=jsonComponent(org.bukkit.inventory.meta.ItemMeta.class.getMethod("displayName").invoke(item.getItemMeta()));
        check(name.contains(font)&&name.contains("Navigator")&&name.toLowerCase(Locale.ROOT).contains("#38bdf8"),"RGB item component and font: "+name);
        String title=jsonComponent(org.bukkit.inventory.InventoryView.class.getMethod("title").invoke(player.getOpenInventory()));
        check(title.contains(font)&&title.contains("Kek")&&title.contains("Games")&&!title.contains("{brand}"),"branded inventory title and font: "+title);
        check(player.getOpenInventory().getTopInventory().getItem(1).getType()==(profile.equals("modern")?Material.BLACK_STAINED_GLASS_PANE:Material.GRAY_STAINED_GLASS_PANE),"profile inventory background");
        var objective=player.getScoreboard().getObjective("lobby");
        check(org.bukkit.scoreboard.Objective.class.getMethod("numberFormat").invoke(objective)!=null,"sidebar score numbers suppressed");
        check(jsonComponent(org.bukkit.scoreboard.Objective.class.getMethod("displayName").invoke(objective)).contains(font),"sidebar title font");
        check(jsonComponent(Player.class.getMethod("playerListHeader").invoke(player)).contains(font),"tab header font");
    }
    @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.MONITOR)
    public void interaction(PlayerInteractEntityEvent event){getLogger().info("RUNTIME INTERACT "+event.getPlayer().getName()+" target="+event.getRightClicked().getType()+" distance="+event.getPlayer().getLocation().distance(event.getRightClicked().getLocation())+" cancelled="+event.isCancelled()+" menu="+event.getPlayer().getOpenInventory().getTopInventory().getType());}
    private boolean active()throws Exception{return (boolean)field(lobby,"databaseReady")&&(boolean)field(lobby,"licensed");}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private long npcs(String type){NamespacedKey key=new NamespacedKey(lobby,"npc");return Bukkit.getWorld("world").getEntities().stream().filter(e->type.equals(e.getPersistentDataContainer().get(key,org.bukkit.persistence.PersistentDataType.STRING))).count();}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof ConsoleCommandSender)||args.length<2)return true;
        String id=args[0],test=args[1];
        try{
            Player player=args.length>2?Objects.requireNonNull(Bukkit.getPlayerExact(args[2])):null;
            switch(test){
                case "ready"->{check(lobby.isEnabled()&&active(),"plugin active");check(npcs("reward")==1&&npcs("settings")==1,"exactly one of each configured NPC");}
                case "session"->{check(player.getGameMode()==GameMode.ADVENTURE,"adventure mode");check(player.getInventory().getItem(0).getType()==Material.COMPASS,"navigator hotbar");check(player.getInventory().getItem(7).getType()==Material.NETHER_STAR,"cosmetics hotbar");check(player.getScoreboard().getObjective("lobby")!=null,"sidebar objective");}
                case "ui-classic"->{presentation(player,"classic","minecraft:default");check(uiString(field(lobby,"ui"),"clientVersion",player).contains("1.21.10"),"actual client version detected, including ViaVersion");}
                case "ui-modern"->presentation(player,"modern","minecraft:uniform");
                case "ui-font-default"->presentation(player,"modern","minecraft:default");
                case "reach"->{Entity witch=player.getWorld().getEntities().stream().filter(e->e instanceof Witch).findFirst().orElseThrow();getLogger().info("RUNTIME REACH player="+player.getLocation()+" npc="+witch.getLocation());check(player.getLocation().distance(witch.getLocation())<3,"NPC within server-side interaction reach");}
                case "locked"->{
                    BlockBreakEvent block=new BlockBreakEvent(player.getWorld().getBlockAt(0,64,0),player);Bukkit.getPluginManager().callEvent(block);check(block.isCancelled(),"block protection");
                    EntityDamageEvent damage=EntityDamageEvent.class.getConstructor(Entity.class,EntityDamageEvent.DamageCause.class,double.class).newInstance(player,EntityDamageEvent.DamageCause.FALL,5d);Bukkit.getPluginManager().callEvent(damage);check(damage.isCancelled(),"fall protection");
                    FoodLevelChangeEvent food=FoodLevelChangeEvent.class.getConstructor(HumanEntity.class,int.class).newInstance(player,1);Bukkit.getPluginManager().callEvent(food);check(food.isCancelled(),"hunger protection");
                }
                case "build"->{check(player.getGameMode()==GameMode.CREATIVE,"build mode creative");BlockBreakEvent block=new BlockBreakEvent(player.getWorld().getBlockAt(0,64,0),player);Bukkit.getPluginManager().callEvent(block);check(!block.isCancelled(),"build mode permits breaking");}
                case "jump"->{player.setAllowFlight(true);PlayerToggleFlightEvent event=new PlayerToggleFlightEvent(player,true);Bukkit.getPluginManager().callEvent(event);check(event.isCancelled(),"double jump consumes flight");check(!player.getAllowFlight()&&player.getVelocity().getY()>.7,"double jump impulse");}
                case "hidden"->check(!player.canSee(Objects.requireNonNull(Bukkit.getPlayerExact("BotBob"))),"hidden target");
                case "visible"->check(player.canSee(Objects.requireNonNull(Bukkit.getPlayerExact("BotBob"))),"visible target");
                case "cosmetics"->{check(player.getInventory().getHelmet()!=null&&player.getInventory().getHelmet().getType()==Material.CREEPER_HEAD,"selected head equipped");check(npcs("balloon")>=1,"balloon spawned");}
                case "gadget"->{Map<?,?> sessions=(Map<?,?>)field(lobby,"sessions");Object session=sessions.get(player.getUniqueId());check(System.currentTimeMillis()-(long)field(session,"gadgetAt")<3000,"gadget activation reached server");}
                case "launch"->{Map<?,?> sessions=(Map<?,?>)field(lobby,"sessions");Object session=sessions.get(player.getUniqueId());long elapsed=System.nanoTime()-(long)field(session,"lastLaunchNanos");getLogger().info("RUNTIME LAUNCH position="+player.getLocation()+" velocity="+player.getVelocity()+" elapsedNanos="+elapsed);check(elapsed<5_000_000_000L,"launch pad was triggered by actual movement");}
                case "expire"->{Settings cfg=(Settings)field(lobby,"cfg");Path key=getDataFolder().toPath().toAbsolutePath().getParent().getParent().resolve("runtime-test-private.der");cfg.license=License.issue(License.privateKey(key),"ExpiredCI",cfg.serverId,1,Instant.now().minusSeconds(172800));}
                case "inactive"->{check(!active(),"expired license disabled lobby");check(((Map<?,?>)field(lobby,"sessions")).isEmpty(),"sessions restored after expiry");check(npcs("reward")==0&&npcs("settings")==0,"NPC cleanup after expiry");}
                case "activate"->{Path path=lobby.getDataFolder().toPath().resolve("config.json");Settings cfg=new com.google.gson.Gson().fromJson(Files.readString(path),Settings.class);check(Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lobbylicense activate "+cfg.license),"activation command accepted");}
                default->throw new IllegalArgumentException("unknown probe: "+test);
            }
            getLogger().info("RUNTIME PASS "+id+" "+test);
        }catch(Throwable error){getLogger().severe("RUNTIME FAIL "+id+" "+test+" "+error);error.printStackTrace();}
        return true;
    }
}
