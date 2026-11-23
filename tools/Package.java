import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Offline jar merger. No server API or signing key is packaged. */
public class Package {
    public static void main(String[] args)throws Exception {
        Set<String> seen=new HashSet<>();Map<String,Set<String>> services=new TreeMap<>();
        Path target=Path.of("target/LobbySystem-"+System.getenv("LOBBY_VERSION")+".jar");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(target))){
            for(String dependency:System.getenv("LOBBY_BUNDLE_JARS").split(java.util.regex.Pattern.quote(File.pathSeparator))){
                try(ZipFile zip=new ZipFile(dependency)){
                    var entries=zip.entries();while(entries.hasMoreElements()){
                        ZipEntry entry=entries.nextElement();String name=entry.getName();String upper=name.toUpperCase(Locale.ROOT);
                        if(entry.isDirectory()||upper.endsWith(".SF")||upper.endsWith(".RSA")||upper.endsWith(".DSA")||name.endsWith("module-info.class")||name.equals("META-INF/MANIFEST.MF"))continue;
                        byte[] bytes=zip.getInputStream(entry).readAllBytes();
                        if(name.startsWith("META-INF/services/"))services.computeIfAbsent(name,n->new TreeSet<>()).addAll(Arrays.asList(new String(bytes,java.nio.charset.StandardCharsets.UTF_8).split("\\R")));
                        else if(seen.add(name)){out.putNextEntry(new ZipEntry(name));out.write(bytes);out.closeEntry();}
                    }
                }
            }
            for(var service:services.entrySet()){out.putNextEntry(new ZipEntry(service.getKey()));out.write((String.join("\n",service.getValue())+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();}
            try(var files=Files.walk(Path.of("target/classes"))){for(Path file:files.filter(Files::isRegularFile).toList()){
                String name=Path.of("target/classes").relativize(file).toString().replace('\\','/');if(!seen.add(name))throw new IllegalStateException("Duplicate resource: "+name);
                out.putNextEntry(new ZipEntry(name));out.write(Files.readAllBytes(file));out.closeEntry();
            }}
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));out.write("Manifest-Version: 1.0\nMulti-Release: true\n\n".getBytes());out.closeEntry();
        }
        System.out.println(target+": "+Files.size(target)+" bytes");
    }
}
