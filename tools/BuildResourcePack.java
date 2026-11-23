import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Original pixel assets and reproducible ZIP; JDK 21+, no fonts, downloads or external libraries. */
public class BuildResourcePack {
    static final int CYAN=0x38BDF8,PURPLE=0xA78BFA,TEXT=0xCBD5E1,MUTED=0x64748B;
    static final Map<String,byte[]> entries=new TreeMap<>();
    static Map<Integer,int[]> glyphs,icons;
    static Map<Integer,int[]> readGlyphs(Path file,int rows,int width)throws IOException {
        Map<Integer,int[]> result=new TreeMap<>();
        for(String line:Files.readAllLines(file)){
            line=line.strip();if(line.isEmpty()||line.startsWith("#"))continue;
            String[] parts=line.split("\\s+");if(parts.length!=rows+1)throw new IOException("Invalid row count: "+line);
            int code=Integer.parseInt(parts[0],16);if(code<33||code>0xFFFF||Character.isSurrogate((char)code))throw new IOException("Invalid code point: "+line);
            int[] values=new int[rows];int ink=0;for(int y=0;y<rows;y++){values[y]=Integer.parseInt(parts[y+1],16);if(values[y]<0||values[y]>=(1<<width))throw new IOException("Invalid glyph width: "+line);ink|=values[y];}
            if(ink==0||result.put(code,values)!=null)throw new IOException("Empty or duplicate glyph: "+line);
        }
        return result;
    }
    static int left(int[] rows,int width){int result=width;for(int row:rows)for(int x=0;x<width;x++)if((row&(1<<(width-1-x)))!=0)result=Math.min(result,x);return result;}
    static int width(int[] rows,int bits){int min=left(rows,bits),max=0;for(int row:rows)for(int x=0;x<bits;x++)if((row&(1<<(bits-1-x)))!=0)max=Math.max(max,x);return max-min+1;}
    static BufferedImage atlas(Map<Integer,int[]> source,int bits,boolean trim){
        BufferedImage image=new BufferedImage(128,((source.size()+15)/16)*8,BufferedImage.TYPE_INT_ARGB);int index=0;
        for(int[] rows:source.values()){
            int offset=trim?left(rows,bits):0;
            for(int y=0;y<rows.length;y++)for(int x=0;x<bits;x++)if((rows[y]&(1<<(bits-1-x)))!=0)image.setRGB(index%16*8+x-offset,index/16*8+y,0xFFFFFFFF);
            index++;
        }
        return image;
    }
    static byte[] png(BufferedImage image)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();if(!ImageIO.write(image,"png",out))throw new IOException("No PNG writer");return out.toByteArray();}
    static String quote(String value){StringBuilder out=new StringBuilder("\"");for(char c:value.toCharArray()){if(c=='"'||c=='\\')out.append('\\').append(c);else if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}return out.append('"').toString();}
    static byte[] font(Map<Integer,int[]> source,String texture,int height,int ascent){
        List<Integer> codes=new ArrayList<>(source.keySet());StringJoiner rows=new StringJoiner(",\n      ");
        for(int i=0;i<codes.size();i+=16){StringBuilder row=new StringBuilder();for(int col=0;col<16;col++)row.append((char)(i+col<codes.size()?codes.get(i+col):0));rows.add(quote(row.toString()));}
        String json="{\n  \"providers\": [{\"type\":\"bitmap\",\"file\":"+quote(texture)+",\"height\":"+height+",\"ascent\":"+ascent+",\"chars\":[\n      "+rows+"\n  ]}]\n}\n";
        return json.getBytes(StandardCharsets.UTF_8);
    }
    static void text(Graphics2D g,String text,int x,int y,int scale,int rgb){
        g.setColor(new Color(rgb));for(int code:text.codePoints().toArray()){
            if(code==32){x+=4*scale;continue;}
            int[] rows=glyphs.get(code);boolean icon=false;if(rows==null){rows=icons.get(code);icon=rows!=null;}if(rows==null)throw new IllegalArgumentException("Preview glyph missing: "+code);
            int bits=icon?8:5,offset=icon?0:left(rows,bits);
            for(int gy=0;gy<rows.length;gy++)for(int gx=0;gx<bits;gx++)if((rows[gy]&(1<<(bits-1-gx)))!=0)g.fillRect(x+(gx-offset)*scale,y+gy*scale,scale,scale);
            x+=(icon?9:width(rows,bits)+1)*scale;
        }
    }
    static BufferedImage packIcon(){
        BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(0x0B1220));g.fillRect(0,0,128,128);g.setColor(new Color(CYAN));g.fillRect(8,8,112,4);g.fillRect(8,8,4,34);g.setColor(new Color(PURPLE));g.fillRect(8,116,112,4);g.fillRect(116,86,4,34);
        text(g,"K",21,32,7,CYAN);text(g,"G",68,32,7,0xFFFFFF);text(g,"UI",54,97,2,PURPLE);g.dispose();return image;
    }
    static BufferedImage preview(){
        BufferedImage image=new BufferedImage(1000,700,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(0x080E1A));g.fillRect(0,0,1000,700);g.setColor(new Color(0x101B2D));g.fillRoundRect(36,30,928,640,18,18);
        text(g,"Kek",66,62,5,CYAN);text(g,"Games",156,62,5,0xFFFFFF);text(g,"UI",340,70,3,PURPLE);
        text(g,"SCHRIFT- UND SYMBOLVORSCHAU",66,120,2,MUTED);g.setColor(new Color(0x263449));g.fillRect(66,153,868,2);
        text(g,"KekGames » Navigator",66,181,3,CYAN);text(g,"Dein nächstes Ziel wartet.",66,217,2,TEXT);
        String[] labels={"Navigator","Coins","Spielzeit","Rang","Belohnung","Einstellungen","Spieler","Extras","Doppelsprung","Bestätigen","Zurück","Weiter","Favoriten","Lose","Gesperrt","Netzwerk"};
        for(int i=0;i<16;i++){
            int x=66+i%4*220,y=270+i/4*62;g.setColor(new Color(0x17243A));g.fillRoundRect(x,y,202,50,7,7);text(g,String.valueOf((char)(0xE000+i)),x+12,y+12,3,i%2==0?CYAN:PURPLE);text(g,labels[i],x+48,y+19,1,TEXT);
        }
        text(g,"ÄÖÜ äöü ß | 0123456789 | + 250 Coins",66,551,2,TEXT);text(g,"Font: kekgames:ui",66,593,2,PURPLE);
        text(g,"Designvorschau aus den Paketdateien, kein Ingame-Screenshot.",66,638,1,MUTED);g.dispose();return image;
    }
    static String hash(byte[] bytes,String algorithm)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));}
    public static void main(String[]args)throws Exception{
        System.setProperty("java.awt.headless","true");Path source=Path.of(args.length>0?args[0]:"resourcepack"),out=Path.of(args.length>1?args[1]:"target/resourcepack");Files.createDirectories(out);
        String version=Files.readString(source.resolve("version.txt")).strip();if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IllegalArgumentException("Invalid version");
        glyphs=readGlyphs(source.resolve("glyphs.txt"),7,5);icons=readGlyphs(source.resolve("icons.txt"),8,8);
        for(int code=33;code<=126;code++)if(!glyphs.containsKey(code))throw new IllegalArgumentException("Missing ASCII glyph "+code);
        for(int code=0xE000;code<=0xE00F;code++)if(!icons.containsKey(code))throw new IllegalArgumentException("Missing UI symbol "+code);
        entries.put("pack.mcmeta",Files.readAllBytes(source.resolve("pack.mcmeta")));entries.put("LICENSE.txt",Files.readAllBytes(source.resolve("LICENSE.txt")));entries.put("pack.png",png(packIcon()));
        try(var files=Files.walk(source.resolve("assets"))){for(Path file:files.filter(Files::isRegularFile).sorted().toList())entries.put(source.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file));}
        entries.put("assets/kekgames/font/glyphs.json",font(glyphs,"kekgames:font/ui.png",8,7));
        entries.put("assets/kekgames/font/heading_glyphs.json",font(glyphs,"kekgames:font/ui.png",9,8));
        entries.put("assets/kekgames/font/icons.json",font(icons,"kekgames:font/icons.png",8,7));
        entries.put("assets/kekgames/textures/font/ui.png",png(atlas(glyphs,5,true)));entries.put("assets/kekgames/textures/font/icons.png",png(atlas(icons,8,false)));
        entries.put("LIESMICH.txt",("KekGames UI "+version+" | Minecraft Java 1.21.10 - 26.3\n\nZIP nicht entpacken. In Minecraft unter Optionen > Ressourcenpakete laden.\nAnschließend als Lobby-Admin: /lobbyui font kekgames:ui\nZurück zur Standardschrift: /lobbyui font auto\n\nSchriften: kekgames:ui, kekgames:heading, kekgames:icons\nAlle Spieler benötigen das Paket, wenn die eigene Schrift serverweit aktiv ist.\nServer-Einrichtung und Symbol-Preset: resourcepack/README.md im GitHub-Projekt.\n").getBytes(StandardCharsets.UTF_8));
        Path zip=out.resolve("KekGames-UI-"+version+".zip");
        try(ZipOutputStream stream=new ZipOutputStream(Files.newOutputStream(zip),StandardCharsets.UTF_8)){stream.setLevel(Deflater.BEST_COMPRESSION);for(var entry:entries.entrySet()){ZipEntry z=new ZipEntry(entry.getKey());z.setTimeLocal(LocalDateTime.of(1980,1,1,0,0));stream.putNextEntry(z);stream.write(entry.getValue());stream.closeEntry();}}
        byte[] bytes=Files.readAllBytes(zip);String sha1=hash(bytes,"SHA-1"),sha256=hash(bytes,"SHA-256");
        Files.writeString(out.resolve("KekGames-UI-"+version+".sha1"),sha1+"\n");Files.writeString(out.resolve("resourcepack-SHA256SUMS.txt"),sha256+"  "+zip.getFileName()+"\n");
        Files.write(out.resolve("KekGames-UI-Vorschau.png"),png(preview()));
        Files.writeString(out.resolve("KekGames-UI-Installation.txt"),"KekGames UI "+version+"\n\n1. ZIP beim Spieler als Ressourcenpaket laden oder folgende Werte in server.properties zusammenführen.\n2. Server vollständig neu starten und beim Beitritt das Paket annehmen.\n3. Als Admin: /lobbyui font kekgames:ui\n\nresource-pack=https://github.com/conlog06/lobbysystem-kekgames/releases/download/v1.2.0/"+zip.getFileName()+"\nresource-pack-sha1="+sha1+"\nrequire-resource-pack=true\nresource-pack-prompt={\"text\":\"KekGames: eigene Schrift und Lobby-Symbole\",\"color\":\"aqua\"}\n\nrequire-resource-pack=true verlangt die Zustimmung beim Beitritt. Die globale eigene Schrift benötigt das Paket bei allen Spielern.\nBestehende Resourcepacks zuerst zusammenführen; eine weitere resource-pack-Zeile ersetzt das bisher angebotene Paket.\nCloudNet: Einstellungen ins Lobby-Template übernehmen.\nRückgängig: /lobbyui font auto und die eigene resource-pack-Konfiguration entfernen.\nSymbol-Preset: https://github.com/conlog06/lobbysystem-kekgames/blob/main/resourcepack/preset.json\n",StandardCharsets.UTF_8);
        System.out.println("Resource pack: "+zip+" ("+bytes.length+" bytes, "+glyphs.size()+" glyphs, "+icons.size()+" symbols)");System.out.println("SHA-1: "+sha1);System.out.println("SHA-256: "+sha256);
    }
}
