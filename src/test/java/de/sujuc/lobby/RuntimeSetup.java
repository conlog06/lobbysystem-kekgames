package de.sujuc.lobby;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/** Builds an isolated, ephemeral test installation. Its JAR and key are never release assets. */
public final class RuntimeSetup {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath(),customer=Path.of(args[1]);
        Files.createDirectories(root.resolve("plugins/LobbySystem"));
        KeyPair key=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Files.write(root.resolve("runtime-test-private.der"),key.getPrivate().getEncoded());
        Settings cfg=new Settings();
        cfg.license=License.issue(key.getPrivate(),"RuntimeCI",cfg.serverId,1,Instant.now());
        cfg.database.type="mysql";cfg.database.host="127.0.0.1";cfg.database.name="lobby_runtime";cfg.database.user="lobby";cfg.database.password="lobby_ci";cfg.database.useSsl=true;
        cfg.spawn=new Settings.Spot();cfg.spawn.world="world";cfg.spawn.x=.5;cfg.spawn.y=65;cfg.spawn.z=.5;
        Settings.Spot reward=new Settings.Spot();reward.world="world";reward.x=2.5;reward.y=65;reward.z=.5;
        Settings.Spot settings=new Settings.Spot();settings.world="world";settings.x=-2.5;settings.y=65;settings.z=.5;
        cfg.npcs.put("reward",reward);cfg.npcs.put("settings",settings);
        cfg.transfers.cooldownSeconds=0;cfg.gadgetCooldownSeconds=1;
        cfg.announcements.intervalSeconds=10;cfg.announcements.messages=new ArrayList<>(List.of("RUNTIME_ANNOUNCEMENT {player}"));
        cfg.welcome.message="RUNTIME_WELCOME {player}";
        cfg.save(root.resolve("plugins/LobbySystem/config.json"));
        try(ZipFile source=new ZipFile(customer.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(root.resolve("plugins/LobbySystem.jar")))){
            for(ZipEntry entry:Collections.list(source.entries())){
                if(entry.isDirectory())continue;
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(entry.getName().equals("license-public.der")?key.getPublic().getEncoded():source.getInputStream(entry).readAllBytes());out.closeEntry();
            }
        }
        Path classes=Path.of("target/test-classes/de/sujuc/lobby");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(root.resolve("plugins/RuntimeProbe.jar")))){
            try(var files=Files.list(classes)){for(Path file:files.filter(f->f.getFileName().toString().startsWith("RuntimeProbe")&&f.toString().endsWith(".class")).toList()){
                out.putNextEntry(new ZipEntry("de/sujuc/lobby/"+file.getFileName()));out.write(Files.readAllBytes(file));out.closeEntry();
            }}
            out.putNextEntry(new ZipEntry("plugin.yml"));out.write(("name: RuntimeProbe\nversion: 1\nmain: de.sujuc.lobby.RuntimeProbe\napi-version: '1.21.10'\ndepend: [LobbySystem]\ncommands:\n  runtimeprobe:\n    description: CI-only server assertions\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();
        }
        Files.writeString(root.resolve("server.properties"),"server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\nenforce-secure-profile=false\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\nlevel-type=minecraft:flat\ngenerate-structures=false\nmax-players=5\nenable-rcon=false\n");
        Files.writeString(root.resolve("eula.txt"),"eula=true\n");
        System.out.println("Ephemeral runtime installation prepared; production public key remains unchanged.");
    }
}
