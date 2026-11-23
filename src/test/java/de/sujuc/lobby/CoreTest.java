package de.sujuc.lobby;

import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Integration checks with the actual SQLite driver; no Minecraft server or test framework needed. */
public final class CoreTest {
    static int checks;
    static void check(boolean result,String name){if(!result)throw new AssertionError(name);checks++;}
    interface Throwing {void run()throws Exception;}
    static void rejected(Throwing job,String name)throws Exception{try{job.run();}catch(Exception expected){checks++;return;}throw new AssertionError(name);}
    static <T> T await(CompletableFuture<T> f)throws Exception{return f.get(20,TimeUnit.SECONDS);}
    public static void main(String[] args)throws Exception {
        Instant now=Instant.parse("2026-11-22T12:00:00Z");String server=UUID.randomUUID().toString();KeyPair keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String token=License.issue(keys.getPrivate(),"Testkunde",server,1,now);
        check(License.verify(token,keys.getPublic(),server,now).customer().equals("Testkunde"),"valid signature");
        rejected(()->License.verify(token,keys.getPublic(),UUID.randomUUID().toString(),now),"wrong server");
        rejected(()->License.verify(token,keys.getPublic(),server,now.plusSeconds(86400)),"expiry boundary");
        rejected(()->License.verify("A"+token.substring(1),keys.getPublic(),server,now),"tampered payload");
        rejected(()->License.verify(token,KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic(),server,now),"forged issuer");
        rejected(()->License.issue(keys.getPrivate(),"Bad|name",server,1,now),"payload delimiter");
        String unlimited=License.issue(keys.getPrivate(),"Owner",server,0,now);
        check(License.verify(unlimited,keys.getPublic(),server,now.plusSeconds(86400L*10000)).expires()==0,"perpetual license");
        Path temp=Files.createTempDirectory("lobby-test-");Settings.Database cfg=new Settings.Database();UUID id=UUID.randomUUID();
        boolean mysql="mysql".equals(System.getenv("LOBBY_TEST_STORAGE"));
        if(mysql){cfg.type="mysql";cfg.host=System.getenv().getOrDefault("MYSQL_HOST","127.0.0.1");cfg.port=Integer.parseInt(System.getenv().getOrDefault("MYSQL_PORT","3306"));cfg.name=System.getenv().getOrDefault("MYSQL_DATABASE","lobby_runtime");cfg.user=System.getenv().getOrDefault("MYSQL_USER","lobby");cfg.password=System.getenv().getOrDefault("MYSQL_PASSWORD","lobby_ci");cfg.useSsl=true;}
        Settings defaults=Settings.load(temp.resolve("config.json"));
        check(defaults.permission("build").equals("lobby.build"),"permission prefix and suffix");
        defaults.permissions.put("prefix","custom");defaults.save(temp.resolve("config.json"));
        check(Settings.load(temp.resolve("config.json")).permission("build").equals("custom.build"),"custom permission persistence");
        defaults.navigatorItems.add("12;STONE;spawn;duplicate;invalid");
        rejected(defaults::validate,"duplicate navigator slot rejected");
        Files.writeString(temp.resolve("legacy-config.json"),"{\"permissions\":{\"prefix\":\"network\"},\"messages\":{\"saved\":\"Custom\"}}");
        Settings migrated=Settings.load(temp.resolve("legacy-config.json"));
        check(migrated.permission("pay").equals("network.pay"),"old config gains new permission keys");
        check(migrated.messages.get("saved").equals("Custom")&&migrated.messages.containsKey("transferSent"),"config upgrade preserves text customizations");
        check(migrated.menuTitles.containsKey("profile")&&migrated.transfers.enabled,"new feature defaults are available in old config");
        check(Files.isRegularFile(temp.resolve("config-v1-backup.json"))||Files.isRegularFile(temp.resolve("legacy-config-v1-backup.json")),"legacy configuration is backed up");
        check(VersionStyles.version("1.21.10-R0.1-SNAPSHOT").orElseThrow().supported(),"minimum server version supported");
        check(VersionStyles.version("26.3-R0.1-SNAPSHOT").orElseThrow().modern(),"year-based server version parsed");
        check(VersionStyles.version("1.21.9/1.21.10").orElseThrow().patch()==10,"ViaVersion aliases select highest compatible patch");
        check(!VersionStyles.version("1.21.8").orElseThrow().supported(),"older version is identified");
        check(!VersionStyles.version("27.1").orElseThrow().supported(),"untested future version is identified");
        check(VersionStyles.version("unknown").isEmpty(),"unknown protocol name handled");
        Settings visual=new Settings();
        check(VersionStyles.profile(visual.design,"1.21.10").equals("classic"),"classic profile for old client");
        check(VersionStyles.profile(visual.design,"26.3").equals("modern"),"modern profile for new client");
        visual.design.profile="classic";
        check(VersionStyles.profile(visual.design,"26.3").equals("classic"),"explicit style override respected");
        check(VersionStyles.font(visual.design,"modern").equals("minecraft:uniform"),"modern built-in font selected");
        visual.design.font="example:custom";
        check(VersionStyles.font(visual.design,"classic").equals("example:custom"),"custom pack font override preserved");
        check(VersionStyles.validFont("minecraft:default")&&!VersionStyles.validFont("Bad Font")&&!VersionStyles.validFont("custom:../escape"),"font IDs validated");
        check(VersionStyles.material("DOES_NOT_EXIST|COMPASS")==org.bukkit.Material.COMPASS,"new material falls back on older API");
        rejected(()->VersionStyles.material("DOES_NOT_EXIST|AIR"),"invalid icon choice rejected");
        check(VersionStyles.color("&#12ABEFtest").startsWith(net.md_5.bungee.api.ChatColor.of("#12ABEF").toString()),"RGB syntax converted");
        String brand=VersionStyles.expand(new Settings(),"{brand} &8{separator} &7Test");
        check(!brand.contains("{brand}")&&!brand.contains("{separator}")&&org.bukkit.ChatColor.stripColor(brand).contains("KekGames"),"brand placeholders and palette expanded");
        String already=VersionStyles.color("&#12ABEFtest");
        check(VersionStyles.expand(new Settings(),already).equals(already),"already formatted RGB remains intact");
        var components=VersionStyles.components("Test","minecraft:uniform");
        check(Arrays.stream(components).allMatch(c->"minecraft:uniform".equals(c.getFont())),"font attached to text components");
        check(Arrays.stream(components).noneMatch(c->c.isItalic()),"implicit item italics disabled");
        check(Arrays.stream(VersionStyles.components(VersionStyles.color("&oTest"),"minecraft:default")).anyMatch(c->c.isItalic()),"explicit italic formatting preserved");
        Settings oldDefaults=new Settings();oldDefaults.configVersion=1;oldDefaults.prefix="&8[&bLobby&8] &7";oldDefaults.license="existing-license";oldDefaults.permissions.put("prefix","custom-permission");oldDefaults.menuTitles.put("heads","My custom heads");
        oldDefaults.save(temp.resolve("old-defaults.json"));String previousId=oldDefaults.serverId;
        Settings upgraded=Settings.load(temp.resolve("old-defaults.json"));
        check(upgraded.configVersion==2&&upgraded.prefix.contains("{brand}"),"stock prefix upgraded to new design");
        check(upgraded.license.equals("existing-license")&&upgraded.serverId.equals(previousId),"license and installation ID survive migration");
        check(upgraded.permission("build").equals("custom-permission.build")&&upgraded.menuTitles.get("heads").equals("My custom heads"),"custom permission and menu survive migration");
        oldDefaults.prefix="&5My custom prefix";oldDefaults.save(temp.resolve("custom-old.json"));
        check(Settings.load(temp.resolve("custom-old.json")).prefix.equals("&5My custom prefix"),"custom prefix is not overwritten");
        Settings badDesign=new Settings();badDesign.design.primary="bad-color";
        rejected(badDesign::validate,"invalid palette rejected");
        badDesign.design.primary="ABCDEF";badDesign.design.font="bad font";
        rejected(badDesign::validate,"invalid configured font rejected");
        Settings invalid=new Settings();invalid.announcements.intervalSeconds=0;
        rejected(invalid::validate,"announcement interval validated");
        invalid.announcements.intervalSeconds=120;invalid.launchPads.horizontal=Double.NaN;
        rejected(invalid::validate,"non-finite launch strength rejected");
        invalid.launchPads.horizontal=1.6;invalid.transfers.maximumAmount=0;
        rejected(invalid::validate,"transfer limit validated");
        try(Store first=new Store(cfg,temp);Store second=new Store(cfg,temp)){
            await(first.init());await(second.init());await(first.load(id,"TestPlayer"));
            check(await(first.coins(id,1000)).profile().coins()==1000,"add coins");
            check(!await(first.coins(id,-1001)).success(),"negative balance prevented");
            long day=20000;
            var a=first.reward(id,false,day,200,1);var b=second.reward(id,false,day,200,1);
            check((await(a).success()?1:0)+(await(b).success()?1:0)==1,"cross-connection daily claim exactly once");
            check(!await(first.reward(id,false,day-1,200,1)).success(),"clock rollback cannot reclaim");
            check(await(first.reward(id,true,day,400,2)).success(),"separate premium reward");
            var buyA=first.select(id,"particle","FLAME",500);var buyB=second.select(id,"particle","FLAME",500);
            check(await(buyA).success()&&await(buyB).success(),"concurrent cosmetic selection");
            check(await(first.refresh(id)).coins()==1100,"cosmetic paid only once");
            check(await(first.select(id,"particle","",500)).profile().coins()==1100,"removal free");
            check(!await(first.select(id,"head","DRAGON_HEAD",5000)).success(),"purchase rollback");
            check(await(first.refresh(id)).coins()==1100,"rollback preserved balance");
            for(int i=0;i<3;i++)check(await(first.lottery(id,50)).success(),"ticket consumed");
            check(!await(first.lottery(id,50)).success(),"lottery cannot overdraw");
            check(await(first.setting(id,"visibility",2)).visibility()==2,"settings persisted");
            check(await(first.playtime(id,60)).playtime()==60,"playtime increment");
            rejected(()->await(first.setting(id,"coins",1)),"column allowlist");
            UUID alice=UUID.randomUUID(),bob=UUID.randomUUID(),full=UUID.randomUUID();
            await(first.load(alice,"Alice"));await(first.load(bob,"Bob"));await(first.load(full,"FullWallet"));
            await(first.coins(alice,1000));await(first.coins(full,Long.MAX_VALUE));
            var payA=first.transfer(alice,bob,700);var payB=second.transfer(alice,bob,700);
            check((await(payA).success()?1:0)+(await(payB).success()?1:0)==1,"concurrent transfers cannot double-spend");
            check(await(first.refresh(alice)).coins()==300&&await(first.refresh(bob)).coins()==700,"transfer updates both accounts");
            check(!await(first.transfer(alice,full,10)).success(),"recipient overflow rejects transfer");
            check(await(first.refresh(alice)).coins()==300,"failed transfer preserves sender balance");
            rejected(()->await(first.transfer(alice,alice,1)),"self-transfer rejected");
            rejected(()->await(first.transfer(alice,bob,0)),"zero transfer rejected");
            rejected(()->await(first.transfer(alice,bob,-1)),"negative transfer rejected");
            rejected(()->await(first.transfer(alice,UUID.randomUUID(),10)),"unknown recipient rolls back transfer");
            List<CompletableFuture<Store.Transfer>> transfers=new ArrayList<>();
            for(int i=0;i<20;i++){transfers.add(first.transfer(alice,bob,1));transfers.add(second.transfer(bob,alice,1));}
            check(transfers.stream().allMatch(f->{try{return await(f).success();}catch(Exception e){throw new RuntimeException(e);}}),"opposite concurrent transfers complete");
            check(await(first.refresh(alice)).coins()==300&&await(first.refresh(bob)).coins()==700,"transfers conserve total coins");
            await(first.playtime(bob,120));
            List<Store.LeaderboardEntry> top=await(first.top("playtime",2));
            check(top.size()==2&&top.get(0).id().equals(bob)&&top.get(1).id().equals(id),"playtime leaderboard ordered and limited");
            List<Store.LeaderboardEntry> richest=await(first.top("coins",1));
            check(richest.size()==1&&richest.get(0).id().equals(full),"coins leaderboard includes offline balances");
            rejected(()->await(first.top("coins DESC; DROP TABLE lobby_players",10)),"leaderboard column injection rejected");
            rejected(()->await(first.top("coins",0)),"leaderboard limit validated");
            check(await(first.coins(id,Long.MAX_VALUE-1250)).success(),"large balance supported");
            check(!await(first.coins(id,1)).success(),"coin overflow prevented");
        }
        try(Store reopened=new Store(cfg,temp)){await(reopened.init());check(await(reopened.refresh(id)).visibility()==2,"restart persistence");}
        try(var walk=Files.walk(temp)){for(Path item:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(item);}
        System.out.println("PASS: "+checks+" checks ("+(mysql?"MySQL":"SQLite")+", license, config migration, concurrent transactions, transfers, leaderboards, coins, rewards, cosmetics, lottery, settings, restart)");
    }
}
