package de.sujuc.lobby;

import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Menus {
    private final LobbyPlugin p;
    static final class Holder implements InventoryHolder {
        final UUID viewer; final String menu; Inventory inv;
        Holder(Player p,String menu){viewer=p.getUniqueId();this.menu=menu;}
        public Inventory getInventory(){return inv;}
    }
    static final Map<String,List<String>> OPTIONS=Map.of(
        "particles",List.of("SNOWFLAKE","NOTE","PORTAL","FLAME","HEART","ENCHANT","CLOUD","WITCH","SOUL_FIRE_FLAME"),
        "balloons",List.of("LIME_CONCRETE","RED_CONCRETE","BROWN_CONCRETE","BLUE_CONCRETE","YELLOW_CONCRETE","ORANGE_CONCRETE","PURPLE_CONCRETE","PINK_CONCRETE"),
        "gadgets",List.of("DASH","GRAPPLING","PEARL","FIREWORK","SNOW"),
        "heads",List.of("CREEPER_HEAD","ZOMBIE_HEAD","SKELETON_SKULL","WITHER_SKELETON_SKULL","DRAGON_HEAD","PIGLIN_HEAD"));
    static final Map<String,Material> GADGET_ICONS=Map.of("DASH",Material.FEATHER,"GRAPPLING",Material.FISHING_ROD,"PEARL",Material.ENDER_PEARL,"FIREWORK",Material.FIREWORK_ROCKET,"SNOW",Material.SNOWBALL);
    private static final int[] GRID={12,13,14,20,21,22,23,24,30,32};
    Menus(LobbyPlugin plugin){p=plugin;}
    ItemStack item(Player player,Material material,String name,String lore,String action){
        String textId=action.isEmpty()?"info:"+material.name():action;
        if(action.startsWith("setting:")||action.startsWith("select:"))textId=action.substring(0,action.lastIndexOf(':'));
        Settings.ItemText custom=p.cfg.menuItemTexts.get(textId);
        if(custom!=null){
            String value=action.contains(":")?action.substring(action.lastIndexOf(':')+1):"";
            name=custom.name.replace("{default}",name).replace("{value}",pretty(value)).replace("{price}",""+p.cfg.cosmeticPrice);
            lore=custom.lore.replace("{default}",lore).replace("{value}",pretty(value)).replace("{price}",""+p.cfg.cosmeticPrice);
        }
        ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();
        p.ui.item(player,meta,name,lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_ENCHANTS);
        if(!action.isEmpty())meta.getPersistentDataContainer().set(p.actionKey,PersistentDataType.STRING,action);item.setItemMeta(meta);return item;
    }
    String action(ItemStack item){return item==null||!item.hasItemMeta()?null:item.getItemMeta().getPersistentDataContainer().get(p.actionKey,PersistentDataType.STRING);}
    void open(Player player,String type){
        if(!p.lobby(player))return;
        if(type.equals("leaderboard")){leaderboard(player,"coins");return;}
        if(p.builders.contains(player.getUniqueId())){p.ui.chat(player,p.cfg.prefix+"&eDeaktiviere zunächst den Baumodus mit /build.");return;}
        LobbyPlugin.Session s=p.sessions.get(player.getUniqueId());if(s==null||s.profile==null){p.message(player,"loading");return;}
        int size=switch(type){case "navigator"->p.cfg.navigatorSize;case "lottery"->9;case "settings","visibility","reward"->27;default->45;};
        if(!p.cfg.menuTitles.containsKey(type))return;
        Holder h=new Holder(player,type);Inventory inv=p.ui.inventory(player,h,size,p.cfg.menuTitles.get(type));h.inv=inv;
        ItemStack filler=item(player,VersionStyles.material(p.ui.visual(player).background)," ","","");for(int i=0;i<size;i++)inv.setItem(i,filler);
        ItemStack accent=item(player,VersionStyles.material(p.ui.visual(player).accent)," ","","");inv.setItem(0,accent);inv.setItem(8,accent);
        switch(type){
            case "navigator" -> {for(String row:p.cfg.navigatorItems){String[] v=row.split(";",5);inv.setItem(Integer.parseInt(v[0]),item(player,VersionStyles.material(v[1]),v[3],v[4],"warp:"+v[2]));}}
            case "settings" -> {
                inv.setItem(10,item(player,Material.ENDER_PEARL,"&bSpielersichtbarkeit","&7Alle, VIPs oder niemand","open:visibility"));
                inv.setItem(13,item(player,Material.PAPER,"&bLobby-Nachrichten","&7Aktuell: &e"+(s.profile.messages()?"aktiviert":"deaktiviert"),"setting:messages:"+(s.profile.messages()?0:1)));
                inv.setItem(16,item(player,Material.IRON_BOOTS,"&bDoppelsprung","&7Aktuell: &e"+(s.profile.jump()?"aktiviert":"deaktiviert"),"setting:jump:"+(s.profile.jump()?0:1)));
            }
            case "visibility" -> {
                inv.setItem(11,item(player,Material.LIME_DYE,"&aAlle anzeigen","&7Aktuell: "+visibilityName(s.profile.visibility()),"setting:visibility:0"));
                inv.setItem(13,item(player,Material.ORANGE_DYE,"&6Nur VIPs anzeigen","&7Zeigt Spieler mit VIP-Sichtbarkeit-Permission","setting:visibility:1"));
                inv.setItem(15,item(player,Material.RED_DYE,"&cNiemanden anzeigen","&7Blendet andere Lobby-Spieler aus","setting:visibility:2"));
            }
            case "cosmetics" -> {
                inv.setItem(11,item(player,Material.GLISTERING_MELON_SLICE,"&bPartikel","&7Wähle deinen Partikeleffekt","open:particles"));
                inv.setItem(13,item(player,Material.LEAD,"&bBallone","&7Dein schwebender Begleiter","open:balloons"));
                inv.setItem(15,item(player,Material.ENDER_CHEST,"&bGadgets","&7Bewegung und Effekte","open:gadgets"));
                inv.setItem(22,item(player,Material.PLAYER_HEAD,"&6Heads","&7Wähle eine Kopfbedeckung","open:heads"));
            }
            case "profile" -> {
                inv.setItem(10,item(player,Material.PLAYER_HEAD,"&b"+player.getName(),"&7Rang: "+p.rank(player),""));
                inv.setItem(12,item(player,Material.GOLD_INGOT,"&eCoins","&f"+CommunityFeatures.format(s.profile.coins())+"\n&7Mit /pay kannst du Coins verschenken.",""));
                inv.setItem(14,item(player,Material.CLOCK,"&bSpielzeit","&f"+CommunityFeatures.duration(s.profile.playtime()),""));
                inv.setItem(16,item(player,Material.PAPER,"&eLose","&f"+s.profile.tickets(),"open:lottery"));
                inv.setItem(28,item(player,Material.CHEST,"&aTägliche Belohnung","&7Hole Coins und Lose ab.","open:reward"));
                inv.setItem(30,item(player,Material.NETHER_STAR,"&dCosmetics","&7Deine Effekte und Begleiter","open:cosmetics"));
                inv.setItem(32,item(player,Material.COMPARATOR,"&bEinstellungen","&7Passe deine Lobby an.","open:settings"));
                inv.setItem(34,item(player,Material.GOLD_BLOCK,"&6Ranglisten","&7Die reichsten und aktivsten Spieler","top:coins"));
            }
            case "reward" -> {
                inv.setItem(11,item(player,Material.PAPER,"&aTägliche Belohnung","&7+"+p.cfg.dailyCoins+" Coins &8| &7+"+p.cfg.dailyTickets+" Lose\n&8Einmal pro Kalendertag ("+p.cfg.timeZone+")","reward:normal"));
                inv.setItem(15,item(player,Material.PAPER,"&6Tägliche Belohnung (Premium)","&7+"+p.cfg.premiumCoins+" Coins &8| &7+"+p.cfg.premiumTickets+" Lose\n&8Zusätzliche Belohnung mit Premium-Permission","reward:premium"));
            }
            case "lottery" -> {
                inv.setItem(1,item(player,Material.PAPER,"&eDeine Lose: &f"+s.profile.tickets(),"&7Lose gibt es bei der täglichen Belohnung",""));
                inv.setItem(4,item(player,Material.CHEST,"&6Mögliche Gewinne","&7Coins: "+p.cfg.lotteryPrizes+"\n&7Jeder Eintrag hat dieselbe Chance",""));
                inv.setItem(7,item(player,Material.GOLD_INGOT,"&eLos benutzen","&7Kosten: 1 Los","lottery"));
            }
            default -> {
                List<String> options=OPTIONS.get(type);if(options==null)return;
                String field=field(type);
                String current=switch(field){case "particle"->s.profile.particle();case "balloon"->s.profile.balloon();case "gadget"->s.profile.gadget();default->s.profile.head();};
                for(int i=0;i<options.size();i++){
                    String value=options.get(i);Material icon=type.equals("particles")?particleIcon(value):type.equals("gadgets")?GADGET_ICONS.get(value):Material.valueOf(value);
                    inv.setItem(GRID[i],item(player,icon,"&b"+pretty(value),"&7"+(value.equals(current)?"Ausgewählt":"Anklicken zum Auswählen")+"\n&7Einmaliger Kauf: &e"+p.cfg.cosmeticPrice+" Coins\n&8Bereits freigeschaltet: keine erneute Zahlung","select:"+field+":"+value));
                }
                inv.setItem(31,item(player,Material.RED_DYE,"&cDeaktivieren","&7Aktuelles Cosmetic entfernen","select:"+field+":"));
                inv.setItem(40,item(player,Material.ARROW,"&7Zurück","","open:cosmetics"));
            }
        }
        player.openInventory(inv);
    }
    void leaderboard(Player player,String category){
        if(!List.of("coins","playtime").contains(category))throw new IllegalArgumentException("/lobbytop [coins|playtime]");
        if(!p.lobby(player)||p.builders.contains(player.getUniqueId()))return;
        LobbyPlugin.Session session=p.sessions.get(player.getUniqueId());
        if(session==null||session.profile==null){p.message(player,"loading");return;}
        Holder holder=new Holder(player,"leaderboard");
        Inventory inv=p.ui.inventory(player,holder,36,p.cfg.menuTitles.get("leaderboard"));holder.inv=inv;
        ItemStack filler=item(player,VersionStyles.material(p.ui.visual(player).background)," ","","");
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,filler);
        inv.setItem(0,item(player,Material.GOLD_INGOT,"&eTop Coins","&7Nach Kontostand sortieren","top:coins"));
        inv.setItem(8,item(player,Material.CLOCK,"&bTop Spielzeit","&7Nach Spielzeit sortieren","top:playtime"));
        inv.setItem(13,item(player,Material.PAPER,"&7Lade Rangliste …","",""));
        inv.setItem(31,item(player,Material.ARROW,"&7Zurück zum Profil","","open:profile"));
        player.openInventory(inv);
        p.future(p.store.top(category,10),rows->{
            if(p.sessions.get(player.getUniqueId())!=session||player.getOpenInventory().getTopInventory()!=inv)return;
            for(int i=9;i<27;i++)inv.setItem(i,filler);
            if(rows.isEmpty()){inv.setItem(13,item(player,Material.PAPER,p.cfg.messages.get("topEmpty"),"",""));return;}
            int[] slots={10,11,12,13,14,15,16,19,20,21};
            for(int i=0;i<rows.size();i++){
                Store.LeaderboardEntry row=rows.get(i);
                String value=category.equals("coins")?CommunityFeatures.format(row.value())+" Coins":CommunityFeatures.duration(row.value());
                String name=p.cfg.messages.get("topEntry").replace("{position}",""+(i+1)).replace("{player}",row.name()).replace("{value}",value);
                inv.setItem(slots[i],item(player,category.equals("coins")?Material.GOLD_INGOT:Material.CLOCK,name,"&7"+(category.equals("coins")?"Kontostand":"Gesamte Lobby-Spielzeit"),""));
            }
        },player);
    }
    static String field(String menu){return switch(menu){case "particles"->"particle";case "balloons"->"balloon";case "gadgets"->"gadget";case "heads"->"head";default->throw new IllegalArgumentException("Kategorie ungültig");};}
    private static String visibilityName(int v){return switch(v){case 1->"VIPs";case 2->"Niemand";default->"Alle";};}
    private static String pretty(String value){return value.toLowerCase(Locale.GERMAN).replace('_',' ');}
    private static Material particleIcon(String name){return switch(name){case "SNOWFLAKE"->Material.SNOWBALL;case "NOTE"->Material.MUSIC_DISC_CAT;case "PORTAL"->Material.ENDER_EYE;case "FLAME"->Material.FLINT_AND_STEEL;case "HEART"->Material.RED_DYE;case "ENCHANT"->Material.ENCHANTING_TABLE;case "WITCH"->Material.REDSTONE;case "SOUL_FIRE_FLAME"->Material.SOUL_TORCH;default->Material.BONE_MEAL;};}
    void click(Player player,String action){
        if(!p.lobby(player)||action==null||p.builders.contains(player.getUniqueId()))return;
        LobbyPlugin.Session s=p.sessions.get(player.getUniqueId());if(s==null||s.profile==null){p.message(player,"loading");return;}
        if(action.startsWith("open:")){open(player,action.substring(5));return;}
        if(action.startsWith("top:")){leaderboard(player,action.substring(4));return;}
        if(action.startsWith("warp:")){player.closeInventory();p.teleport(player,action.substring(5));return;}
        if(action.equals("gadget")){p.effects.gadget(s);return;}
        if(action.startsWith("setting:")){
            String[] a=action.split(":");p.profile(s,p.store.setting(player.getUniqueId(),a[1],Integer.parseInt(a[2])));player.closeInventory();return;
        }
        if(action.startsWith("select:")){
            String[] a=action.split(":",-1);boolean valid=a[2].isEmpty()||OPTIONS.entrySet().stream().anyMatch(e->field(e.getKey()).equals(a[1])&&e.getValue().contains(a[2]));
            if(valid)p.result(s,p.store.select(player.getUniqueId(),a[1],a[2],p.cfg.cosmeticPrice),"selected","insufficient");player.closeInventory();return;
        }
        if(action.startsWith("reward:")){
            boolean premium=action.endsWith("premium");if(premium&&!p.permission(player,"premium"))return;
            int coins=premium?p.cfg.premiumCoins:p.cfg.dailyCoins,tickets=premium?p.cfg.premiumTickets:p.cfg.dailyTickets;
            long day=LocalDate.now(ZoneId.of(p.cfg.timeZone)).toEpochDay();
            p.result(s,p.store.reward(player.getUniqueId(),premium,day,coins,tickets),"reward","claimed","coins",""+coins,"tickets",""+tickets);player.closeInventory();return;
        }
        if(action.equals("lottery")){
            int prize=p.cfg.lotteryPrizes.get(ThreadLocalRandom.current().nextInt(p.cfg.lotteryPrizes.size()));
            p.result(s,p.store.lottery(player.getUniqueId(),prize),"lottery","insufficient","coins",""+prize);player.closeInventory();
        }
    }
}
