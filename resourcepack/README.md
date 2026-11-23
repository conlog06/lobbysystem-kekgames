# KekGames UI 1.0.0

Eigenes Ressourcenpaket für **Minecraft Java 1.21.10 bis 26.3**, passend zu LobbySystem **ab 1.2.0**. Enthalten sind 127 selbst gezeichnete Pixelzeichen, deutsche Umlaute/ß, 16 Lobby-Symbole, drei Font-IDs und ein Paket-Icon. Andere Unicode-Zeichen verwenden die Minecraft-Standardschrift. Das Paket enthält keine kopierten Minecraft-Texturen oder fremden Fontdateien und verändert keine Vanilla-Blöcke, Items oder Inventartexturen.

## Download und Aktivierung

1. [KekGames-UI-1.0.0.zip](https://github.com/conlog06/lobbysystem-kekgames/releases/download/v1.2.0/KekGames-UI-1.0.0.zip) herunterladen. ZIP **nicht entpacken**.
2. In Minecraft unter **Optionen → Ressourcenpakete → Paketordner öffnen** ablegen und aktivieren.
3. Als Admin mit Setup-Permission ausführen:

```text
/lobbyui font kekgames:ui
```

Dieser Befehl speichert die globale Schrift und aktualisiert die Lobby-Darstellung sofort. **Alle Spieler brauchen das geladene Paket**, sobald die eigene Schrift global aktiv ist. Ohne Paket kann Minecraft diese Schrift/Symbole nicht darstellen. Erst das Paket verteilen, danach die Schrift aktivieren.

Zurück zum versionsabhängigen Standard: `/lobbyui font auto`.

`kekgames:ui` ist die normale Pixelschrift, `kekgames:heading` eine etwas größere Variante und `kekgames:icons` die Symbolschrift. Auf Paper werden auch Inventartitel, Sidebar und Tablist mit der gewählten Schrift dargestellt; bei fehlenden Spigot-Komponenten-APIs gelten die Fallbacks des Plugins.

## Auf dem Server anbieten

Die [Installationsdatei](https://github.com/conlog06/lobbysystem-kekgames/releases/download/v1.2.0/KekGames-UI-Installation.txt) enthält die tatsächliche Download-URL und SHA-1 für `server.properties`. Diese Werte in die vorhandene Datei übernehmen, danach den Server vollständig neu starten:

```properties
resource-pack=https://github.com/conlog06/lobbysystem-kekgames/releases/download/v1.2.0/KekGames-UI-1.0.0.zip
resource-pack-sha1=SHA1_AUS_DER_INSTALLATIONSDATEI
require-resource-pack=true
resource-pack-prompt={"text":"KekGames: eigene Schrift und Lobby-Symbole","color":"aqua"}
```

`require-resource-pack=true` verlangt die Annahme des Pakets beim Beitritt. Damit ist sichergestellt, dass die serverweit gewählte Schrift bei den Spielern verfügbar ist. Bestehende Ressourcenpakete vorher zusammenführen: Eine neue `resource-pack`-URL ersetzt das bisher vom Server angebotene Paket. Bei jeder Änderung am ZIP dessen SHA-1 aktualisieren. CloudNet: Einstellungen in das Lobby-Template übernehmen.

Das Plugin v1.2.0 bietet selbst keinen HTTP-Server und keinen eigenen Pack-Downloader an. Die Verteilung übernimmt Minecraft über `server.properties` und die öffentliche GitHub-Download-URL.

## Symbole in der Hotbar verwenden

Die Schrift `kekgames:ui` enthält die Symbole bereits. [preset.json](preset.json) liefert fertige Itemtexte für die Lobby-Hotbar. Die enthaltenen Schlüssel in die bestehenden Abschnitte `design` und `menuItemTexts` der `plugins/LobbySystem/config.json` einfügen und vollständig neu starten. **Nicht die vollständige Config ersetzen**; Lizenz, Installations-ID, Datenbank und eigene Einstellungen behalten.

Die JSON-Escapes werden beim Laden zu den passenden Zeichen:

| JSON-Zeichen | Symbol | JSON-Zeichen | Symbol |
|---|---|---|---|
| `\ue000` | Navigator | `\ue008` | Doppelsprung/Gadget |
| `\ue001` | Coins | `\ue009` | Bestätigen |
| `\ue002` | Spielzeit | `\ue00a` | Zurück |
| `\ue003` | Rang | `\ue00b` | Weiter |
| `\ue004` | Belohnungen | `\ue00c` | Favorit/Herz |
| `\ue005` | Einstellungen | `\ue00d` | Lose |
| `\ue006` | Spieler | `\ue00e` | Gesperrt |
| `\ue007` | Extras | `\ue00f` | Netzwerk |

Beispiel für einen Hotbar-Eintrag:

```json
"open:navigator": {"name":"&b\ue000 Navigator","lore":"{default}"}
```

Die Symbole sind weiße Bitmap-Glyphen und übernehmen die Textfarbe aus der Plugin-Palette.

## Bauen und prüfen

```text
java tools/BuildResourcePack.java
python tools/validate_resourcepack.py target/resourcepack/KekGames-UI-1.0.0.zip
```

Der JDK-Build benötigt keine externen Bibliotheken und keine installierten Schriften. PNG-Atlanten, Fontdefinitionen, ZIP, Vorschau, SHA-1/SHA-256 und Installationsdatei entstehen unter `target/resourcepack/`. Der CI-Job baut zweimal und prüft die Reproduzierbarkeit der ZIP-Datei.

Der unabhängige Validator kontrolliert ZIP-Struktur/CRC, Paketformate, Referenzen/Zyklen, echte PNG-Pixel, Glyphraster/Baselines, Zeichenabdeckung, Preset und Prüfsummen. Optional prüft `ValidateResourcePackClient.java` die JSON-Dateien mit den tatsächlichen Minecraft-26.3-Client-Codecs, wenn dessen JAR und Bibliotheken im Klassenpfad liegen. Das startet keinen grafischen Client.

Die Metadaten verwenden die seit [1.21.9 geltenden Felder `min_format`/`max_format`](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-9), mit dem Bereich 69.0 bis [97.1 für 26.3](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3). Verwendet werden nur Bitmap-, Space- und Reference-Fontprovider. Die Vorlage enthält keine versionsabhängigen Itemmodelle oder Shader. Die Vorschau wird direkt aus den Glyphquellen gerendert und ist kein Ingame-Screenshot; eine grafische Prüfung beider Clients ist nicht automatisiert.

Die ursprünglichen Lobbyfunktionen und das Lizenzsystem ändern sich durch dieses Paket nicht. Lizenz der Pixelgrafiken: [MIT](LICENSE.txt).
