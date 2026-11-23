package de.sujuc.lobby;

import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.bukkit.*;

public final class Settings {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String license = "";
    public String serverId = UUID.randomUUID().toString();
    public int configVersion=2;
    public Design design=new Design();
    public String prefix = "{brand} &8{separator} &7";
    public String world = "world";
    public Database database = new Database();
    public Map<String,String> permissions = new LinkedHashMap<>(Map.ofEntries(
        Map.entry("prefix","lobby"), Map.entry("suffix_build","build"),
        Map.entry("suffix_coins","coins"), Map.entry("suffix_setup","setup"),
        Map.entry("suffix_teleport","teleport"), Map.entry("suffix_premium","premium"),
        Map.entry("suffix_visibility","visibility.vip"), Map.entry("suffix_pay","pay")));
    public Map<String,String> messages = new LinkedHashMap<>(Map.ofEntries(
        Map.entry("noPermission","&cDafür hast du keine Berechtigung."),
        Map.entry("saved","&aGespeichert."), Map.entry("loading","&eDeine Daten werden geladen."),
        Map.entry("databaseError","&cDatenbankfehler. Bitte versuche es später erneut."),
        Map.entry("unlicensed","&cKeine gültige Lizenz. Nutze /lobbylicense request."),
        Map.entry("claimed","&cDiese Belohnung hast du bereits abgeholt."),
        Map.entry("reward","&aBelohnung erhalten: {coins} Coins und {tickets} Lose."),
        Map.entry("insufficient","&cNicht genug Coins oder Lose."),
        Map.entry("purchased","&aCosmetic freigeschaltet."),
        Map.entry("selected","&aAuswahl gespeichert."),
        Map.entry("warpMissing","&cDieser Warp ist noch nicht eingerichtet."),
        Map.entry("connecting","&aVerbinde mit {server} …"),
        Map.entry("coins","&e{player}: &f{coins} Coins"),
        Map.entry("build","&7Baumodus: &e{state}"),
        Map.entry("lottery","&aGewinn: {coins} Coins!"),
        Map.entry("balance","&7Du hast &e{coins} Coins&7."),
        Map.entry("transferSent","&aDu hast {player} {coins} Coins gesendet."),
        Map.entry("transferReceived","&aDu hast von {player} {coins} Coins erhalten."),
        Map.entry("transferFailed","&cÜberweisung fehlgeschlagen: Guthaben zu niedrig oder Empfängerkonto voll."),
        Map.entry("transferCooldown","&eWarte noch einen Moment vor der nächsten Überweisung."),
        Map.entry("transferDisabled","&cCoin-Überweisungen sind deaktiviert."),
        Map.entry("topHeader","&bTop 10 &8– &f{category}"),
        Map.entry("topEntry","&e#{position} &f{player} &8» &b{value}"),
        Map.entry("topEmpty","&7Es gibt noch keine Einträge.")));
    public String scoreboardTitle = "{brand}";
    public List<String> scoreboardLines = defaultSidebar();
    public String tabHeader = "\n{brand}\n&7Dein Platz auf &fKekGames\n";
    public String tabFooter = "\n&7{online} Spieler &8{separator} &7{server_version}\n&8{date} &8| &7{time}\n";
    public String timeZone = "Europe/Berlin";
    public int navigatorSize = 54;
    public List<String> navigatorItems = new ArrayList<>(List.of(
        "12;IRON_AXE;connect:CityBuild-1;&7» &aCityBuild;&7Klicke zum Verbinden",
        "14;DIAMOND_PICKAXE;skyblock;&7» &bSkyBlock;&7Klicke zum Teleportieren",
        "22;CHEST;spawn;&7» &eSpawn;&7Zurück zum Spawn"));
    public Map<String,String> menuTitles = new LinkedHashMap<>(Map.ofEntries(
        Map.entry("navigator","&bNavigator"), Map.entry("settings","&bEinstellungen"),
        Map.entry("visibility","&bSpieler"), Map.entry("cosmetics","&dExtras"),
        Map.entry("particles","&bPartikel"), Map.entry("balloons","&bBallone"),
        Map.entry("gadgets","&bGadgets"), Map.entry("heads","&bKöpfe"),
        Map.entry("reward","&bBelohnungen"), Map.entry("lottery","&eLotterie"),
        Map.entry("profile","&bDein Profil"), Map.entry("leaderboard","&6Ranglisten")));
    public Map<String,ItemText> menuItemTexts = defaultItemTexts();
    public static final class ItemText {
        public String name="{default}", lore="{default}";
    }
    public static final class Design {
        public String profile="auto",font="",brand="&b&lKek&f&lGames",separator="»";
        public String primary="38BDF8",secondary="A78BFA",text="CBD5E1",muted="64748B",success="86EFAC",error="FCA5A5",highlight="FCD34D";
        public boolean palette=true,hideScoreNumbers=true;
        public String actionBar="&7{date} &8{separator} &b{time}",menuTitle="{brand} &8{separator} {title}";
        public String rewardNpc="&bTägliche Belohnung",settingsNpc="&bEinstellungen";
        public VisualProfile classic=new VisualProfile();
        public VisualProfile modern=VisualProfile.modern();
    }
    public static final class VisualProfile {
        public String font="minecraft:default",background="GRAY_STAINED_GLASS_PANE",accent="CYAN_STAINED_GLASS_PANE";
        public Map<String,String> icons=new LinkedHashMap<>(Map.of("navigator","COMPASS","cosmetics","NETHER_STAR","lottery","TNT","settings","NAME_TAG","visibility","BARRIER","profile","PLAYER_HEAD"));
        static VisualProfile modern(){VisualProfile p=new VisualProfile();p.font="minecraft:uniform";p.background="BLACK_STAINED_GLASS_PANE";p.accent="PURPLE_STAINED_GLASS_PANE";p.icons.putAll(Map.of("navigator","RECOVERY_COMPASS|COMPASS","cosmetics","AMETHYST_SHARD|NETHER_STAR","lottery","TRIAL_KEY|TNT","settings","COMPARATOR|NAME_TAG","visibility","ENDER_EYE|BARRIER"));return p;}
    }
    private static List<String> defaultSidebar(){return new ArrayList<>(List.of("&8&m--------------------","&7SPIELER","&f{player}"," ","&7COINS &8{separator} &e{coins}","&7LOSE &8{separator} &e{tickets}","  ","&7SPIELZEIT","&f{playtime}","   ","&7RANG","{rank}","    ","&8{server_version} &8{separator} &b{online} online"));}
    private static Map<String,ItemText> defaultItemTexts(){
        Map<String,ItemText> result=new LinkedHashMap<>();
        for(String id:List.of("open:navigator","open:lottery","open:visibility","open:settings","open:cosmetics","open:reward","open:particles","open:balloons","open:gadgets","open:heads","open:profile","top:coins","top:playtime","gadget","lottery","setting:messages","setting:jump","setting:visibility","reward:normal","reward:premium","select:particle","select:balloon","select:gadget","select:head","info:PAPER","info:CHEST","info:PLAYER_HEAD","info:GOLD_INGOT","info:CLOCK"))result.put(id,new ItemText());
        return result;
    }
    public int dailyCoins = 200, dailyTickets = 1, premiumCoins = 400, premiumTickets = 2;
    public int cosmeticPrice = 500, gadgetCooldownSeconds = 5;
    public List<Integer> lotteryPrizes = new ArrayList<>(List.of(25,50,100,200,500,1000));
    public Transfers transfers = new Transfers();
    public LaunchPads launchPads = new LaunchPads();
    public Welcome welcome = new Welcome();
    public Announcements announcements = new Announcements();
    public static final class Transfers {
        public boolean enabled=true;
        public long maximumAmount=1_000_000;
        public int cooldownSeconds=2;
    }
    public static final class LaunchPads {
        public boolean enabled=true;
        public String material="SLIME_BLOCK";
        public double horizontal=1.6, vertical=1.1;
        public int cooldownSeconds=2;
    }
    public static final class Welcome {
        public boolean enabled=true;
        public String title="&b&lKekGames", subtitle="&7Willkommen, &f{player}&7!";
        public String message="&7Schön, dass du da bist! Mit &b/profil &7öffnest du dein Profil.";
    }
    public static final class Announcements {
        public boolean enabled=true;
        public int intervalSeconds=120;
        public List<String> messages=new ArrayList<>(List.of(
            "&bTipp &8» &7Mit &e/reward &7holst du deine tägliche Belohnung ab.",
            "&bTipp &8» &7Mit &e/pay <Spieler> <Coins> &7verschenkst du Coins.",
            "&bTipp &8» &7Entdecke Cosmetics und Ranglisten in &e/profil&7."));
    }
    public Spot spawn;
    public Map<String,Spot> warps = new LinkedHashMap<>();
    public Map<String,Spot> npcs = new LinkedHashMap<>();
    public static final class Database {
        public String type="sqlite", host="localhost", name="lobby", user="root", password="";
        public int port=3306;
        public boolean useSsl=true;
    }
    public static final class Spot {
        public String world; public double x,y,z; public float yaw,pitch;
        public Spot() {}
        public Spot(Location l) { world=l.getWorld().getName(); x=l.getX(); y=l.getY(); z=l.getZ(); yaw=l.getYaw(); pitch=l.getPitch(); }
        public Location location() { World w=Bukkit.getWorld(world); return w==null?null:new Location(w,x,y,z,yaw,pitch); }
    }
    static Settings load(Path path) throws IOException {
        Settings s;
        int previousVersion=2;
        if (Files.exists(path)) {
            try (Reader r=Files.newBufferedReader(path)) { JsonObject data=JsonParser.parseReader(r).getAsJsonObject();previousVersion=data.has("configVersion")?data.get("configVersion").getAsInt():1;s=JSON.fromJson(data,Settings.class); }
            if(s==null) throw new IOException("config.json ist leer");
        } else s=new Settings();
        Settings defaults=new Settings();
        s.permissions=merge(defaults.permissions,s.permissions);
        s.messages=merge(defaults.messages,s.messages);
        s.menuTitles=merge(defaults.menuTitles,s.menuTitles);
        s.menuItemTexts=merge(defaults.menuItemTexts,s.menuItemTexts);
        s.design.classic.icons=merge(defaults.design.classic.icons,s.design.classic.icons);
        s.design.modern.icons=merge(defaults.design.modern.icons,s.design.modern.icons);
        if(previousVersion<2){
            Path backup=path.resolveSibling("config-v"+previousVersion+"-backup.json");if(!Files.exists(backup))Files.copy(path,backup);
            if(s.prefix.equals("&8[&bLobby&8] &7"))s.prefix=defaults.prefix;
            if(s.scoreboardTitle.equals("&b&lKekGames")||s.scoreboardTitle.equals("&b&lDein Netzwerk"))s.scoreboardTitle=defaults.scoreboardTitle;
            List<String> old=List.of("&8&m----------------", "&e✦ Coins", "&f{coins}", " ", "&b⌚ Spielzeit", "&f{hours} Stunden", "  ", "&d✦ Rang", "{rank}", "   ", "&a✦ Lose", "&f{tickets}", "&8&m-----------------");
            if(s.scoreboardLines.equals(old))s.scoreboardLines=defaults.scoreboardLines;
            if(s.tabHeader.equals("&b&lKekGames\n&7Willkommen, {player}!")||s.tabHeader.equals("&b&lDein Netzwerk\n&7Willkommen, {player}!"))s.tabHeader=defaults.tabHeader;
            if(s.tabFooter.equals("&7{date} &8| &e{time}"))s.tabFooter=defaults.tabFooter;
            Map<String,String> oldTitles=Map.of("visibility","&bSpielersichtbarkeit","cosmetics","&eCosmetics","particles","&eCosmetics &8| &bPartikel","balloons","&eCosmetics &8| &bBallone","gadgets","&eCosmetics &8| &bGadgets","heads","&eCosmetics &8| &bHeads","reward","&bTägliche Belohnung","lottery","&bLottery");
            oldTitles.forEach((key,value)->{if(value.equals(s.menuTitles.get(key)))s.menuTitles.put(key,defaults.menuTitles.get(key));});
            s.configVersion=2;
        }
        s.validate(); s.save(path); return s;
    }
    private static <T> Map<String,T> merge(Map<String,T> defaults,Map<String,T> custom){
        if(custom==null)throw new IllegalArgumentException("Konfigurationsabschnitte dürfen nicht null sein");
        defaults.putAll(custom);return defaults;
    }
    void validate() {
        UUID.fromString(serverId); java.time.ZoneId.of(timeZone);
        if(!List.of("sqlite","mysql").contains(database.type)) throw new IllegalArgumentException("database.type: sqlite oder mysql");
        if(navigatorSize<9 || navigatorSize>54 || navigatorSize%9!=0) throw new IllegalArgumentException("navigatorSize: 9 bis 54, Vielfaches von 9");
        if(dailyCoins<0 || dailyTickets<0 || premiumCoins<0 || premiumTickets<0 || cosmeticPrice<0 || gadgetCooldownSeconds<1) throw new IllegalArgumentException("Negative Belohnung/Preis oder ungültiger Cooldown");
        if(lotteryPrizes.isEmpty() || lotteryPrizes.stream().anyMatch(n->n==null||n<0)) throw new IllegalArgumentException("lotteryPrizes ungültig");
        if(scoreboardLines.size()>15) throw new IllegalArgumentException("Maximal 15 Scoreboardzeilen");
        if(configVersion>2)throw new IllegalArgumentException("Die Config stammt von einer neueren Plugin-Version");
        if(design==null||!List.of("auto","classic","modern").contains(design.profile))throw new IllegalArgumentException("design.profile: auto, classic oder modern");
        if(!design.font.isEmpty()&&!VersionStyles.validFont(design.font))throw new IllegalArgumentException("design.font: ungültige Schrift-ID");
        for(String color:List.of(design.primary,design.secondary,design.text,design.muted,design.success,design.error,design.highlight))if(!color.matches("(?i)[0-9a-f]{6}"))throw new IllegalArgumentException("Designfarben benötigen sechs Hex-Zeichen ohne #");
        for(VisualProfile profile:List.of(design.classic,design.modern)){
            if(!VersionStyles.validFont(profile.font))throw new IllegalArgumentException("Ungültige Profil-Schrift-ID");
            VersionStyles.material(profile.background);VersionStyles.material(profile.accent);
            for(String icon:profile.icons.values())VersionStyles.material(icon);
        }
        if(transfers.maximumAmount<1||transfers.cooldownSeconds<0||transfers.cooldownSeconds>3600)throw new IllegalArgumentException("transfers: ungültiges Limit oder Cooldown");
        if(Material.matchMaterial(launchPads.material)==null)throw new IllegalArgumentException("launchPads.material ist unbekannt");
        if(!Double.isFinite(launchPads.horizontal)||!Double.isFinite(launchPads.vertical)||launchPads.horizontal<0||launchPads.horizontal>4||launchPads.vertical<0||launchPads.vertical>4||launchPads.cooldownSeconds<1||launchPads.cooldownSeconds>3600)throw new IllegalArgumentException("launchPads: Stärke 0–4, Cooldown 1–3600 Sekunden");
        if(announcements.intervalSeconds<10||announcements.intervalSeconds>86400||announcements.messages==null||announcements.messages.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("announcements: Intervall 10–86400 Sekunden und gültige Nachrichten erforderlich");
        Set<Integer> slots=new HashSet<>();
        for(String row:navigatorItems) { String[] v=row.split(";",5); if(v.length!=5) throw new IllegalArgumentException("Navigator: fünf Felder erforderlich"); int slot=Integer.parseInt(v[0]); if(slot<0||slot>=navigatorSize||!slots.add(slot)) throw new IllegalArgumentException("Navigator: Slot ungültig: "+row);VersionStyles.material(v[1]); }
    }
    void save(Path path) throws IOException {
        Files.createDirectories(path.getParent()); Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(tmp,JSON.toJson(this));
        try { Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException ex) { Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING); }
    }
    String permission(String suffix) { return permissions.get("prefix")+"."+permissions.get("suffix_"+suffix); }
}
