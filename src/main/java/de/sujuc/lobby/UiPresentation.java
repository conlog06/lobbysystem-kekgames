package de.sujuc.lobby;

import java.lang.reflect.*;
import java.time.Duration;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/** Uses public APIs when available and keeps a legacy Spigot fallback. No NMS or packets. */
final class UiPresentation {
    final LobbyPlugin plugin;
    private final Class<?> component,numberFormat;
    private final Object serializer;
    private final Method deserialize,metaName,metaLore,inventoryFactory,viaApi,viaVersion,versionName;
    private final Object via;
    private boolean reported;

    UiPresentation(LobbyPlugin plugin){
        this.plugin=plugin;
        Class<?> comp=null,nf=null;Object gson=null,api=null;Method decode=null,name=null,lore=null,inventory=null,apiMethod=null,versionMethod=null,protocolName=null;
        try{
            comp=Class.forName("net.kyori.adventure.text.Component");
            Class<?> type=Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            gson=type.getMethod("gson").invoke(null);try{decode=type.getMethod("deserialize",String.class);}catch(NoSuchMethodException erased){decode=type.getMethod("deserialize",Object.class);}
            inventory=org.bukkit.Server.class.getMethod("createInventory",InventoryHolder.class,int.class,comp);
        }catch(ReflectiveOperationException ignored){}
        try{name=ItemMeta.class.getMethod("setDisplayNameComponent",BaseComponent[].class);lore=ItemMeta.class.getMethod("setLoreComponents",List.class);}catch(NoSuchMethodException ignored){}
        try{nf=Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");}catch(ClassNotFoundException ignored){}
        try{
            org.bukkit.plugin.Plugin installed=Bukkit.getPluginManager().getPlugin("ViaVersion");
            if(installed!=null){ClassLoader loader=installed.getClass().getClassLoader();Class<?> bridge=Class.forName("com.viaversion.viaversion.api.Via",true,loader);apiMethod=bridge.getMethod("getAPI");api=apiMethod.invoke(null);versionMethod=Class.forName("com.viaversion.viaversion.api.ViaAPI",true,loader).getMethod("getPlayerProtocolVersion",UUID.class);protocolName=Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion",true,loader).getMethod("getName");}
        }catch(ReflectiveOperationException ignored){}
        component=comp;serializer=gson;deserialize=decode;metaName=name;metaLore=lore;inventoryFactory=inventory;numberFormat=nf;via=api;viaApi=apiMethod;viaVersion=versionMethod;versionName=protocolName;
    }
    String clientVersion(Player viewer){
        if(viewer!=null&&via!=null&&viaVersion!=null&&versionName!=null){try{Object version=viaVersion.invoke(via,viewer.getUniqueId());String name=(String)versionName.invoke(version);if(VersionStyles.version(name).isPresent())return name;}catch(ReflectiveOperationException ignored){}}
        return Bukkit.getBukkitVersion();
    }
    String profile(Player viewer){return VersionStyles.profile(plugin.cfg.design,clientVersion(viewer));}
    String font(Player viewer){return VersionStyles.font(plugin.cfg.design,profile(viewer));}
    Settings.VisualProfile visual(Player viewer){return VersionStyles.visual(plugin.cfg.design,profile(viewer));}
    Material icon(Player viewer,String role){return VersionStyles.material(visual(viewer).icons.get(role));}
    String text(String value){return VersionStyles.expand(plugin.cfg,value);}
    BaseComponent[] components(Player viewer,String value){return VersionStyles.components(text(value),font(viewer));}
    private Object adventure(Player viewer,String value)throws ReflectiveOperationException{return deserialize.invoke(serializer,ComponentSerializer.toString(components(viewer,value)));}
    private void unavailable(Exception error){if(!reported){reported=true;plugin.getLogger().warning("Komponenten-Darstellung nicht verfügbar, nutze kompatible Textdarstellung: "+error.getClass().getSimpleName());}}
    void chat(CommandSender target,String value){if(target instanceof Player player)player.spigot().sendMessage(components(player,value));else target.sendMessage(text(value));}
    void actionBar(Player player,String value){player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,components(player,value));}
    void item(Player viewer,ItemMeta meta,String name,String lore){
        List<String> lines=lore.isBlank()?List.of():Arrays.asList(lore.split("\n",-1));
        try{
            if(metaName!=null&&metaLore!=null){metaName.invoke(meta,(Object)components(viewer,"&r"+name));List<BaseComponent[]> entries=lines.stream().map(line->components(viewer,"&r"+line)).toList();metaLore.invoke(meta,entries);return;}
            if(component!=null&&deserialize!=null){ItemMeta.class.getMethod("displayName",component).invoke(meta,adventure(viewer,"&r"+name));List<Object> entries=new ArrayList<>();for(String line:lines)entries.add(adventure(viewer,"&r"+line));ItemMeta.class.getMethod("lore",List.class).invoke(meta,entries);return;}
        }catch(ReflectiveOperationException error){unavailable(error);}
        meta.setDisplayName(text("&r"+name));meta.setLore(lines.stream().map(line->text("&r"+line)).toList());
    }
    Inventory inventory(Player viewer,InventoryHolder holder,int size,String title){
        title=plugin.cfg.design.menuTitle.replace("{title}",title);
        if(inventoryFactory!=null&&deserialize!=null)try{return (Inventory)inventoryFactory.invoke(Bukkit.getServer(),holder,size,adventure(viewer,title));}catch(ReflectiveOperationException error){unavailable(error);}
        return Bukkit.createInventory(holder,size,text(title));
    }
    void objective(Player viewer,Objective objective,String title){
        if(component!=null&&deserialize!=null)try{Objective.class.getMethod("displayName",component).invoke(objective,adventure(viewer,title));return;}catch(ReflectiveOperationException error){unavailable(error);}
        objective.setDisplayName(text(title));
    }
    void prefix(Player viewer,Team team,String value){
        if(component!=null&&deserialize!=null)try{Team.class.getMethod("prefix",component).invoke(team,adventure(viewer,value));return;}catch(ReflectiveOperationException error){unavailable(error);}
        team.setPrefix(text(value));
    }
    void hideNumbers(Objective objective){
        if(plugin.cfg.design.hideScoreNumbers&&numberFormat!=null)try{Object blank=numberFormat.getMethod("blank").invoke(null);Objective.class.getMethod("numberFormat",numberFormat).invoke(objective,blank);}catch(ReflectiveOperationException error){unavailable(error);}
    }
    void tab(Player viewer,String name,String header,String footer){
        if(component!=null&&deserialize!=null)try{
            Player.class.getMethod("playerListName",component).invoke(viewer,adventure(viewer,name));
            Player.class.getMethod("sendPlayerListHeaderAndFooter",component,component).invoke(viewer,adventure(viewer,header),adventure(viewer,footer));return;
        }catch(ReflectiveOperationException error){unavailable(error);}
        viewer.setPlayerListName(text(name));viewer.setPlayerListHeaderFooter(text(header),text(footer));
    }
    void welcome(Player viewer,String title,String subtitle){
        if(component!=null&&deserialize!=null)try{
            Class<?> type=Class.forName("net.kyori.adventure.title.Title"),times=Class.forName("net.kyori.adventure.title.Title$Times");
            Object timing=times.getMethod("times",Duration.class,Duration.class,Duration.class).invoke(null,Duration.ofMillis(500),Duration.ofSeconds(3),Duration.ofSeconds(1));
            Object value=type.getMethod("title",component,component,times).invoke(null,adventure(viewer,title),adventure(viewer,subtitle),timing);
            Player.class.getMethod("showTitle",type).invoke(viewer,value);return;
        }catch(ReflectiveOperationException error){unavailable(error);}
        viewer.sendTitle(text(title),text(subtitle),10,60,20);
    }
    void status(CommandSender target){
        Player viewer=target instanceof Player p?p:null;
        chat(target,plugin.cfg.prefix+"&7Server: &f"+Bukkit.getBukkitVersion()+" &8| &7Client: &f"+clientVersion(viewer));
        chat(target,plugin.cfg.prefix+"&7Profil: &b"+profile(viewer)+" &8| &7Schrift: &f"+font(viewer));
        chat(target,plugin.cfg.prefix+"&7Komponenten-Menüs: &f"+(inventoryFactory!=null)+" &8| &7Zahlen ausblenden: &f"+(numberFormat!=null)+" &8| &7ViaVersion: &f"+(via!=null));
    }
    void refresh(){for(LobbyPlugin.Session s:plugin.sessions.values()){s.presentationKey="";s.player.closeInventory();if(!plugin.builders.contains(s.player.getUniqueId()))plugin.hotbar(s.player);if(s.profile!=null)plugin.display(s);}}
    void command(CommandSender sender,String[] args){
        if(!plugin.permission(sender,"setup"))return;
        if(args.length==0||args.length==1&&args[0].equals("status")){status(sender);return;}
        if(args.length!=2)throw new IllegalArgumentException("/lobbyui status | profile auto|classic|modern | font auto|minecraft:default|minecraft:uniform");
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "profile"->{if(!List.of("auto","classic","modern").contains(args[1]))throw new IllegalArgumentException("Profil: auto, classic oder modern");plugin.cfg.design.profile=args[1];}
            case "font"->{String font=args[1].equals("auto")?"":args[1];if(!font.isEmpty()&&!VersionStyles.validFont(font))throw new IllegalArgumentException("Schrift als namespace:name angeben");plugin.cfg.design.font=font;}
            default->throw new IllegalArgumentException("Unbekannte Design-Aktion");
        }
        plugin.save();refresh();plugin.message(sender,"saved");status(sender);
    }
}
