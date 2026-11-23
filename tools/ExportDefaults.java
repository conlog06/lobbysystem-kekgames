import com.google.gson.GsonBuilder;
import de.sujuc.lobby.Settings;
import java.nio.file.*;

public class ExportDefaults {
  public static void main(String[] args)throws Exception {
    Path dir=Path.of(args[0]);Files.createDirectories(dir);
    Files.writeString(dir.resolve("config.example.json"),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(new Settings()));
    Files.writeString(dir.resolve("ranks.example.json"),"[{\"permission\":\"lobby.rank.owner\",\"display\":\"&cLeitung\",\"priority\":100},{\"permission\":\"lobby.rank.premium\",\"display\":\"&6Premium\",\"priority\":50},{\"permission\":\"\",\"display\":\"&7Spieler\",\"priority\":0}]");
  }
}
