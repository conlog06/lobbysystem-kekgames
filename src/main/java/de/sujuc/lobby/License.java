package de.sujuc.lobby;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;

/** Public-key verification only. No signing secret in customer distribution. */
public final class License {
    public record Grant(String customer, String server, long expires, String serial) {}
    private static final Base64.Encoder ENC=Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC=Base64.getUrlDecoder();
    public static String issue(PrivateKey key,String customer,String server,int days,Instant now) throws Exception {
        UUID.fromString(server);
        if(days<0 || days>36500 || !customer.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Kunde: 1–64 Buchstaben/Ziffern/_/-, Tage: 0–36500");
        String data="LobbySystem|1|"+customer+"|"+server+"|"+(days==0?0:now.plusSeconds(Math.multiplyExact((long)days,86400)).getEpochSecond())+"|"+UUID.randomUUID();
        byte[] payload=data.getBytes(StandardCharsets.UTF_8);
        Signature sig=Signature.getInstance("Ed25519"); sig.initSign(key); sig.update(payload);
        return ENC.encodeToString(payload)+"."+ENC.encodeToString(sig.sign());
    }
    public static Grant verify(String token,PublicKey key,String server,Instant now) throws Exception {
        if(token==null || token.length()>4096) throw new IllegalArgumentException("Lizenz ungültig");
        String[] parts=token.split("\\.",-1); if(parts.length!=2) throw new IllegalArgumentException("Lizenzformat ungültig");
        byte[] payload=DEC.decode(parts[0]); Signature sig=Signature.getInstance("Ed25519"); sig.initVerify(key); sig.update(payload);
        if(!sig.verify(DEC.decode(parts[1]))) throw new GeneralSecurityException("Signatur ungültig");
        String[] f=new String(payload,StandardCharsets.UTF_8).split("\\|",-1);
        if(f.length!=6 || !f[0].equals("LobbySystem") || !f[1].equals("1") || !f[3].equals(server)) throw new GeneralSecurityException("Lizenz gehört zu einer anderen Installation");
        long expiry=Long.parseLong(f[4]);
        if(expiry<0 || expiry!=0 && now.getEpochSecond()>=expiry) throw new GeneralSecurityException("Lizenz abgelaufen");
        UUID.fromString(f[5]); return new Grant(f[2],f[3],expiry,f[5]);
    }
    public static PublicKey publicKey(byte[] encoded) throws Exception { return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded)); }
    public static PrivateKey privateKey(Path path) throws Exception { return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(path))); }
}
