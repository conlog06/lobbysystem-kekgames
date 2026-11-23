import java.nio.file.*;
import java.security.*;
/** Run once. Never regenerate keys for an existing customer installation. */
public class Keygen {
  public static void main(String[] args) throws Exception {
    Path pub=Path.of(args[0]), secret=Path.of(args[1]);
    if(Files.exists(pub)||Files.exists(secret)) throw new IllegalStateException("Schlüssel existieren bereits. Kein Überschreiben.");
    KeyPair pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    Files.createDirectories(pub.getParent()); Files.createDirectories(secret.getParent());
    Files.write(secret,pair.getPrivate().getEncoded(),StandardOpenOption.CREATE_NEW);
    Files.write(pub,pair.getPublic().getEncoded(),StandardOpenOption.CREATE_NEW);
    System.out.println("Schlüsselpaar erstellt. Private Datei niemals an Kunden weitergeben.");
  }
}
