import com.google.gson.*;
import com.mojang.serialization.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** Optional headless check with Mojang's actual unobfuscated 26.3 client codecs on the classpath. */
class ValidateResourcePackClient {
    static Codec<?> codec(String type,String field)throws Exception{Field value=Class.forName(type).getDeclaredField(field);value.setAccessible(true);return (Codec<?>)value.get(null);}
    public static void main(String[]args)throws Exception{
        Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        Object metadata=Class.forName("net.minecraft.server.packs.metadata.pack.PackMetadataSection").getField("CLIENT_TYPE").get(null);
        Codec<?> packCodec=(Codec<?>)metadata.getClass().getMethod("codec").invoke(metadata);
        Codec<?> fontCodec=codec("net.minecraft.client.gui.font.FontManager$FontDefinitionFile","CODEC");int count=0;
        try(ZipFile zip=new ZipFile(args[0])){
            JsonObject root=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("pack.mcmeta")).readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            packCodec.parse(JsonOps.INSTANCE,root.get("pack")).getOrThrow();
            var entries=zip.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();if(entry.getName().startsWith("assets/kekgames/font/")&&entry.getName().endsWith(".json")){
                JsonElement json=JsonParser.parseString(new String(zip.getInputStream(entry).readAllBytes(),StandardCharsets.UTF_8));fontCodec.parse(JsonOps.INSTANCE,json).getOrThrow();count++;
            }}
        }
        if(count!=5)throw new AssertionError("Expected five font definitions");
        System.out.println("PASS: actual Minecraft 26.3 client metadata codec and "+count+" font definition codecs (no graphical client)");
    }
}
