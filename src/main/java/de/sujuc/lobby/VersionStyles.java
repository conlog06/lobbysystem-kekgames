package de.sujuc.lobby;

import java.util.*;
import java.util.regex.*;
import org.bukkit.Material;
import net.md_5.bungee.api.chat.*;

/** Formatting and version selection without server internals or resource-pack requirements. */
public final class VersionStyles {
    private static final Pattern VERSION=Pattern.compile("(?<![0-9])([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?");
    private static final Pattern HEX=Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern PALETTE=Pattern.compile("(?i)&([bd78ace6])");
    public record Version(int major,int minor,int patch) implements Comparable<Version> {
        public int compareTo(Version other){int result=Integer.compare(major,other.major);if(result==0)result=Integer.compare(minor,other.minor);return result==0?Integer.compare(patch,other.patch):result;}
        public boolean modern(){return major>=26;}
        public boolean supported(){return major==1&&minor==21&&patch>=10||major==26&&minor>=1&&minor<=3;}
        public String toString(){return major+"."+minor+(patch==0?"":"."+patch);}
    }
    public static Optional<Version> version(String name){
        Matcher matcher=VERSION.matcher(name);Version latest=null;
        while(matcher.find()){
            Version value=new Version(Integer.parseInt(matcher.group(1)),Integer.parseInt(matcher.group(2)),matcher.group(3)==null?0:Integer.parseInt(matcher.group(3)));
            if(latest==null||value.compareTo(latest)>0)latest=value;
        }
        return Optional.ofNullable(latest);
    }
    public static String profile(Settings.Design design,String version){
        if(!design.profile.equals("auto"))return design.profile;
        return version(version).map(v->v.modern()?"modern":"classic").orElse("classic");
    }
    public static Settings.VisualProfile visual(Settings.Design design,String profile){return profile.equals("modern")?design.modern:design.classic;}
    public static String font(Settings.Design design,String profile){return design.font.isEmpty()?visual(design,profile).font:design.font;}
    public static boolean validFont(String font){return font.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&!font.contains("..");}
    public static String expand(Settings settings,String text){
        Settings.Design d=settings.design;
        String result=text.replace("{brand}",d.brand).replace("{separator}",d.separator)
            .replace("{primary}","&#"+d.primary).replace("{secondary}","&#"+d.secondary)
            .replace("{muted}","&#"+d.muted).replace("{text}","&#"+d.text)
            .replace("{success}","&#"+d.success).replace("{error}","&#"+d.error).replace("{highlight}","&#"+d.highlight);
        if(!d.palette)return color(result);
        Matcher matcher=PALETTE.matcher(result);StringBuffer out=new StringBuffer();
        while(matcher.find()){
            String hex=switch(Character.toLowerCase(matcher.group(1).charAt(0))){case 'b'->d.primary;case 'd'->d.secondary;case '7'->d.text;case '8'->d.muted;case 'a'->d.success;case 'c'->d.error;default->d.highlight;};
            matcher.appendReplacement(out,Matcher.quoteReplacement("&#"+hex));
        }
        matcher.appendTail(out);return color(out.toString());
    }
    public static String color(String value){
        Matcher matcher=HEX.matcher(value);StringBuffer out=new StringBuffer();
        while(matcher.find())matcher.appendReplacement(out,Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of("#"+matcher.group(1)).toString()));
        matcher.appendTail(out);return org.bukkit.ChatColor.translateAlternateColorCodes('&',out.toString());
    }
    public static BaseComponent[] components(String formatted,String font){
        BaseComponent[] values=TextComponent.fromLegacyText(formatted);
        if(values.length==0)values=new BaseComponent[]{new TextComponent("")};
        for(BaseComponent component:values){component.setFont(font);if(component.isItalicRaw()==null)component.setItalic(false);}
        return values;
    }
    public static Material material(String alternatives){
        for(String name:alternatives.split("\\|")){Material material=Material.matchMaterial(name.trim());if(material!=null&&!List.of("AIR","CAVE_AIR","VOID_AIR").contains(material.name()))return material;}
        throw new IllegalArgumentException("Kein unterstütztes Itemmaterial: "+alternatives);
    }
}
