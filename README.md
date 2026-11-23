# lobbysystem-kekgames

**KekGames LobbySystem 1.2.0** für Spigot/Paper, entwickelt für Minecraft **1.21.10 bis 26.3**. Java-Bytecode 21; für Minecraft 26.x Java 25 verwenden. Keine NMS-, ProtocolLib- oder CloudNet-Abhängigkeit. Folia wird nicht unterstützt.

## Funktionen

- Navigator mit lokalen Warps und Proxy-Verbindungen, gesicherte Hotbar und konfigurierbare Inventarmenüs.
- Coins, atomare Überweisungen an bekannte Offline-Spieler, Kontostandsabfrage und Coins-/Spielzeit-Ranglisten.
- Profilmenü mit Kontostand, Spielzeit, Losen und Links zu Rewards, Cosmetics, Einstellungen und Ranglisten.
- Normale und zusätzliche Premium-Tagesbelohnung sowie Lottery mit konfigurierbaren Coin-Gewinnen.
- 9 Partikeleffekte, 8 Blockballone, 5 Gadgets und 6 Vanilla-Mobköpfe; einmalige Freischaltung mit Coins.
- Reward-Hexe und Einstellungs-Dorfbewohner, Spawn, Warps, Lobbyschutz und Creative-Baumodus.
- Doppelsprung, Sprungplatten, Sichtbarkeit (alle/VIPs/niemand) und optionaler Chatempfang.
- Begrüßungstitel, persönliche Willkommensnachricht und rotierende Ankündigungen mit Platzhaltern.
- Sidebar, Rangpräfixe, Tab-Header/-Footer und Datum/Uhrzeit in der Actionbar.
- Versionsabhängige Designprofile, RGB-Palette, KekGames-Branding und einstellbare Minecraft-Schriften. Optional erkennt ViaVersion die tatsächliche Clientversion.
- SQLite oder gemeinsame MySQL-Datenbank für mehrere Lobbys.
- Offline-Lizenzen mit Ed25519-Signaturen, Owner-Ausstellung und automatische Ergänzung neuer Config-Schlüssel.

Die ursprünglichen Bilder dienen als funktionale Orientierung. Clan-System, individuelle Kopftexturen, Webshop und Kundenkonto sind nicht enthalten. Das optionale [Ressourcenpaket](resourcepack/README.md) wird separat angeboten. Ballone verwenden unsichtbare ArmorStands mit Blockhelm; Gadgets arbeiten ohne Blockzerstörung.

## Installation

1. Unter [Releases](https://github.com/conlog06/lobbysystem-kekgames/releases/latest) die Kunden-JAR herunterladen; alternativ unter Actions einen erfolgreichen Build herunterladen oder selbst mit Maven bauen. Nur `LobbySystem-1.2.0.jar` in den `plugins/`-Ordner eines separaten Lobby-Servers legen.
2. Server starten und stoppen. Unter `plugins/LobbySystem/` entstehen `config.json` und `ranks.json`. Ohne gültige Lizenz bleiben die Lobbyfunktionen inaktiv; Lizenzbefehle funktionieren bereits.
3. `world`, Datenbank und Texte anpassen. Standard: Welt `world` und SQLite-Datei `players.db`.
4. `/lobbylicense request` zeigt die Installations-ID. Dafür eine Owner-Lizenz ausstellen und als `license` in der Config eintragen; danach neu starten. Alternativ `/lobbylicense activate <Lizenz>` als Konsole oder mit Setup-Permission.
5. `/setspawn`, `/setreward`, `/setsettings` und die benötigten `/setwarp <Name>` ausführen.
6. Ränge in `ranks.json` einrichten und Permissions z. B. über LuckPerms vergeben.

Bei Updates die alte JAR ersetzen, Config und Datenbank behalten. Neue Map-Schlüssel werden ergänzt; eigene Texte und Permission-Namen bleiben erhalten. Vor Produktivänderungen eine Sicherung anlegen. Kein `/reload` verwenden; Config-Änderungen erfordern einen vollständigen Neustart.

Der Server ist als Lobby vorgesehen. Beim Eintritt werden Inventar und Spielmodus für die laufende Sitzung gesichert und beim Verlassen oder Stoppen wiederhergestellt. Ein harter Prozessabbruch kann diese im Arbeitsspeicher liegende Sicherung verlieren. Kein Survival-Inventar auf demselben Server verwenden.

## Commands und Permissions

| Befehl | Funktion | Standard-Permission |
|---|---|---|
| `/build` | Baumodus ein/aus | `lobby.build` |
| `/lobbycoins get <Spieler>` | Fremden Kontostand anzeigen | `lobby.coins` |
| `/lobbycoins add\|remove <Spieler> <Coins>` | Coins verwalten | `lobby.coins` |
| `/setspawn` | Spawn setzen | `lobby.setup` |
| `/setwarp <Name> [remove]` | Warp setzen/entfernen | `lobby.setup` |
| `/setreward [remove]` | Reward-Hexe setzen/entfernen | `lobby.setup` |
| `/setsettings [remove]` | Einstellungs-NPC setzen/entfernen | `lobby.setup` |
| `/tpcoord <x> <y> <z> [Welt]` | Zu Koordinaten teleportieren | `lobby.teleport` |
| `/lobbyui [status]` | Server-/Clientversion, Designprofil und Schrift anzeigen | `lobby.setup` |
| `/lobbyui profile auto\|classic\|modern` | Design umschalten und direkt speichern | `lobby.setup` |
| `/lobbyui font auto\|minecraft:default\|minecraft:uniform` | Schrift umschalten; auch eigene `namespace:name` möglich | `lobby.setup` |
| `/coins` oder `/balance` | Eigenen Kontostand anzeigen | alle |
| `/pay <Spieler> <Coins>` | Coins übertragen, auch an bekannte Offline-Spieler | `lobby.pay` (standardmäßig alle) |
| `/lobbytop [coins\|playtime]` oder `/top` | Ranglisten öffnen | alle |
| `/profile` oder `/profil` | Profilmenü öffnen | alle |
| `/lobby` | Zum Spawn | alle |
| `/navigator`, `/cosmetics`, `/settings`, `/reward`, `/lottery` | Menü öffnen | alle |
| `/lobbylicense status` oder `request` | Lizenzstatus und Installations-ID | alle |
| `/lobbylicense activate <Lizenz>` | Lizenz prüfen und speichern | `lobby.setup` oder Konsole |
| `/lobbylicense create <Kunde> <Tage> <Server-ID>` | Lizenz ausstellen | ausschließlich Owner, siehe unten |

Premium-Belohnung: `lobby.premium`. Im VIP-Sichtbarkeitsmodus sichtbare Spieler: `lobby.visibility.vip`. Coins-Verwaltung und Überweisungen benötigen einen bekannten Spieler. Kontostände können nicht negativ werden; Überweisungen und Käufe prüfen zusätzlich Überläufe.

Normale Permission-Namen stehen in `config.json` unter `permissions`: `prefix + "." + suffix_*`. Beispiel: Prefix `netzwerk`, `suffix_build` = `admin.build` ergibt `netzwerk.admin.build`. Die Owner-Ausstellung hat keinen Permission-, OP- oder Konsolen-Bypass.

## Darstellung für 1.21.10 bis 26.3

`design.profile: "auto"` wählt für 1.21.x das klassische Profil und für 26.x das moderne Profil. Ist ViaVersion installiert, entscheidet die tatsächliche Clientversion; andernfalls die Serverversion. Die Auswahl verändert die Darstellung, die Spielfunktionen bleiben identisch. Beide Profile lassen sich auch mit `/lobbyui profile` fest einstellen.

| Profil | Schrift | Menü | Hotbar-Beispiele |
|---|---|---|---|
| `classic` | `minecraft:default` | grauer Hintergrund, cyanfarbene Akzente | Kompass, Netherstern, TNT |
| `modern` | `minecraft:uniform` | schwarzer Hintergrund, violette Akzente | Bergungskompass, Amethystscherbe, Prüfungsschlüssel |

Beide Schriften sind im Minecraft-Client enthalten und benötigen kein zusätzliches Ressourcenpaket. Zusätzlich gibt es das [KekGames-UI-Ressourcenpaket](resourcepack/README.md) mit eigener Pixelschrift, Umlauten und 16 Lobby-Symbolen. Nach dem Laden mit `/lobbyui font kekgames:ui` aktivieren. Alle Spieler benötigen das Paket, sobald diese eigene Schrift global aktiv ist; die Anleitung enthält die Server-Verteilung per `server.properties`. Das Plugin bietet das ZIP nicht selbst an. `design.font: ""` verwendet die Schrift des jeweiligen Profils; `/lobbyui font auto` setzt dies zurück.

```json
"prefix": "{brand} &8{separator} &7",
"design": {
  "profile": "auto",
  "font": "",
  "brand": "&b&lKek&f&lGames",
  "separator": "»",
  "primary": "38BDF8",
  "secondary": "A78BFA",
  "text": "CBD5E1",
  "muted": "64748B",
  "success": "86EFAC",
  "error": "FCA5A5",
  "highlight": "FCD34D",
  "palette": true,
  "hideScoreNumbers": true,
  "menuTitle": "{brand} &8{separator} {title}",
  "actionBar": "&7{date} &8{separator} &b{time}"
}
```

Weitere Profileinstellungen stehen in [config.example.json](examples/config.example.json) unter `design.classic` und `design.modern`: `font`, `background`, `accent` und `icons`. Materialalternativen wie `TRIAL_KEY|TNT` wählen das erste auf dem Server verfügbare Material. Sie sind auch im Navigator erlaubt.

Die Palette ordnet `&b`, `&d`, `&7`, `&8`, `&a`, `&c` und `&e`/`&6` den Designfarben zu. Mit `palette: false` bleiben deren originale Minecraft-Farben erhalten. Unabhängig davon funktionieren `&#38BDF8` und die Tokens `{primary}`, `{secondary}`, `{text}`, `{muted}`, `{success}`, `{error}`, `{highlight}`, `{brand}` und `{separator}`. `{title}` gilt nur für `design.menuTitle`.

Auf Paper erhalten Items, Menüfenster, Sidebar, Rangpräfixe, Tablist und Titel Textkomponenten mit der gewählten Schrift; Sidebar-Zahlen werden ausgeblendet. Auf reinem Spigot nutzt das Plugin die verfügbaren öffentlichen APIs und fällt für fehlende Komponenten- oder Zahlenformat-APIs auf kompatible Texte zurück. `/lobbyui status` zeigt die verfügbaren Funktionen an. Die grafische Anzeige eigener Ressourcenpakete muss im Spiel geprüft werden.

Beim ersten Update einer alten Config entsteht `config-v1-backup.json`. Anschließend ergänzt das Plugin die neuen Einstellungen. Unveränderte frühere Standardtexte werden aktualisiert; eigene Texte, Permissions, Lizenz, Installations-ID und Datenbank bleiben erhalten. Bestehende Coins und Cosmetics werden nicht zurückgesetzt. Der bisherige Owner-Schlüssel gilt weiter.

Zusätzliche Platzhalter für Sidebar, Tablist, Begrüßung und Ankündigungen: `{playtime}`, `{online}`, `{server_version}`, `{client_version}`, `{ui_profile}`. Die bisherigen Platzhalter bleiben verfügbar.

## Community-Einstellungen

```json
"transfers": {"enabled": true, "maximumAmount": 1000000, "cooldownSeconds": 2},
"launchPads": {"enabled": true, "material": "SLIME_BLOCK", "horizontal": 1.6, "vertical": 1.1, "cooldownSeconds": 2},
"welcome": {
  "enabled": true,
  "title": "&b&lKekGames",
  "subtitle": "&7Willkommen, &f{player}&7!",
  "message": "&7Öffne mit &b/profil &7dein Profil."
},
"announcements": {
  "enabled": true,
  "intervalSeconds": 120,
  "messages": ["&bTipp &8» &7Mit &e/reward &7gibt es tägliche Belohnungen."]
}
```

Sprungplatten sind alle Blöcke des eingestellten Materials in der Lobby-Welt. Darauf laufen oder landen löst sie aus; im Baumodus bleiben sie aus. Horizontale/vertikale Stärke: 0–4. Cooldown: mindestens eine Sekunde.

Überweisungen haben ein Betragslimit und einen lokalen Cooldown. Konten werden in einer Transaktion in fester Reihenfolge gesperrt, damit auch parallele Zahlungen verschiedener Lobbys keine Coins erzeugen oder Konten überziehen. Bei zu wenig Guthaben oder vollem Empfängerkonto wird nichts abgebucht.

Ankündigungen erscheinen nur in aktiven Lobby-Sitzungen und respektieren die Einstellung zum Nachrichtenempfang. Platzhalter für Willkommen, Ankündigungen, Sidebar und Tablist: `{player}`, `{coins}`, `{tickets}`, `{hours}`, `{rank}`, `{date}`, `{time}`.

## Datenbank und CloudNet

```json
"database": {
  "type": "mysql", "host": "localhost", "name": "lobby", "user": "lobby",
  "password": "DEIN_PASSWORT", "port": 3306, "useSsl": true
}
```

MySQL-Datenbank vorher erstellen. Benötigte Rechte: CREATE, SELECT, INSERT und UPDATE. Mehrere Lobbys verwenden dieselbe Datenbank und Zeitzone. Tabellen: `lobby_players`, `lobby_unlocks`. Datenbankarbeit läuft außerhalb des Minecraft-Hauptthreads. Kontostände anderer Instanzen werden etwa alle zehn Sekunden aktualisiert; Ranglisten werden bei jedem Öffnen neu gelesen.

Der Wechsel von SQLite zu MySQL migriert keine Daten. Dieselbe Datenbank wie bei ModularBungee kann genutzt werden; eine direkte Coins-/Clan-Anbindung an dessen fremdes Schema oder API ist nicht enthalten.

Bei CloudNet v3 die JAR im Plugin-Ordner des Lobby-Templates ablegen. Die Config einer gestarteten Instanz nach Einrichtung zurück ins Template übernehmen und Dienste neu starten. MySQL verwenden, da temporäre SQLite-Dateien keine zentrale Datenhaltung liefern. Instanzen desselben Templates teilen bewusst dieselbe lizenzierte `serverId`. Welt, Spawn und NPC-Positionen müssen zum Template passen. Keine spezielle CloudNet-Serviceerkennung oder automatische Proxyregistrierung.

## Navigator, Texte und Ränge

Navigator-Einträge enthalten fünf durch Semikolon getrennte Felder. Slots beginnen bei **0**:

```text
12;IRON_AXE;connect:CityBuild-1;&7» &aCityBuild;&7Klicke zum Verbinden
14;DIAMOND_PICKAXE;skyblock;&7» &bSkyBlock;&7Klicke zum Teleportieren
```

Materialnamen in Großbuchstaben. Lokale Ziele mit `/setwarp skyblock` setzen, `spawn` mit `/setspawn`. `connect:<Servername>` nutzt eine BungeeCord-Connect-Nachricht: Der Proxy muss den Kanal unterstützen und den Zielserver kennen. Bei Velocity Bungee-Plugin-Messaging aktivieren. Ohne Proxy nur lokale Warps nutzen. Eine Zustellbestätigung wird nicht geprüft.

`navigatorSize`: 9–54, Vielfache von 9. `menuTitles` steuert Fenstertitel, `menuItemTexts` die Namen und Beschreibungen der Aktionsgruppen. `{default}` übernimmt den eingebauten Text, `{value}` den Auswahlwert, `{price}` den Cosmetic-Preis:

```json
"open:profile": {"name": "&aDein Spielerprofil", "lore": "&7Deine Statistiken und Extras"},
"select:particle": {"name": "&b{value}", "lore": "&7Freischalten für &e{price} Coins\n{default}"}
```

Farbcodes: `&`, `§` oder RGB mit `&#RRGGBB`. Normale Meldungen stehen unter `messages`; technische Validierungs- und Setup-Hinweise sind teilweise fest im Code. Sidebar/Tablist: `scoreboardTitle`, `scoreboardLines`, `tabHeader`, `tabFooter`. Maximal 15 Sidebar-Zeilen. Daily-Reset/Uhrzeit verwenden `timeZone`, standardmäßig `Europe/Berlin`. Normale und Premium-Rewards lassen sich getrennt einmal pro Kalendertag abholen.

In `ranks.json` gewinnt der höchste passende `priority`-Wert; eine leere Permission dient als Fallback:

```json
[
  {"permission":"lobby.rank.owner","display":"&cLeitung","priority":100},
  {"permission":"lobby.rank.premium","display":"&6Premium","priority":50},
  {"permission":"","display":"&7Spieler","priority":0}
]
```

## Owner-Lizenzen

**Der private Schlüssel gehört niemals in dieses öffentliche Repository, GitHub-Artefakte oder Kundenpakete.** Der öffentliche Prüfschlüssel in `src/main/resources/license-public.der` ist unkritisch. Den vorhandenen privaten Schlüssel aus dem persönlichen Owner-Paket weiterverwenden; bei normalen Builds keinen neuen erzeugen.

1. JAR auf einem privaten Aussteller-Server installieren, der direkt mit `online-mode=true` authentifiziert.
2. Privaten Schlüssel nach `plugins/LobbySystem/owner/private-key.der` kopieren.
3. Als Minecraft-Konto **SujucDerBreite** direkt anmelden. Die erste erfolgreiche Ausstellung fixiert zusätzlich die authentifizierte UUID in `owner/owner-uuid.txt`.
4. `/lobbylicense create Kundenname 30 <Server-ID>` ausführen; `0` Tage bedeutet unbegrenzt. Kundennamen: Buchstaben, Ziffern, `_` und `-`.
5. Lizenztext aus Chat oder `plugins/LobbySystem/issued/` an den Kunden geben.

Ausstellung funktioniert auch ohne aktive Lobby-Lizenz des Aussteller-Servers. Andere Spieler, OPs und die Konsole können keine Lizenzen erstellen. Für die eigene Lizenz die eigene Installations-ID verwenden.

Offline-Prüfung: Produkt, Signatur, Installations-ID und Ablaufdatum. Ablauf schaltet die Lobbyfunktionen spätestens bei der nächsten Sekundenprüfung ab. Es gibt keinen Lizenzserver, keine Sperrliste und kein Instanzlimit. Das Kopieren der vollständigen Config inklusive Installations-ID ermöglicht einen Klon; Cloud-Templates nutzen dies bewusst. Veränderte JARs lassen sich nicht vollständig vor Umgehungen schützen.

`tools/Keygen.java` nur zur Einrichtung einer **neuen** Produktlinie verwenden. Ein neuer Schlüssel passt nicht zu alten Lizenzen; vorhandene Schlüssel werden nicht überschrieben.

## Builds und Tests

```powershell
mvn clean verify
```

Maven baut gegen Spigot 1.21.10 mit Java-21-Bytecode und führt `CoreTest` automatisch in der Testphase aus. Zusätzlicher API-Build mit JDK 25:

```powershell
mvn -Dspigot.version=26.3-R0.1-SNAPSHOT clean verify
```

Ergebnis: `target/LobbySystem-1.2.0.jar`. GitHub Actions führt beide API-Builds aus und stellt Kunden-JARs als Artefakte bereit. Ein Audit prüft, dass der Prüfschlüssel vorhanden ist, kein privater Schlüssel und keine Server-API gebündelt werden und der Plugin-Bytecode Java 21 entspricht. Nur der Release-Job erhält Schreibzugriff zum Veröffentlichen eines Releases.

Offline-Alternative mit JDK 25 und vorhandenen Paper-Bibliotheken:

```powershell
.\build-offline.ps1 -ServerLibraries 'C:\Pfad\zum\PaperServer\libraries'
```

Lokal geprüft: Build gegen Paper **26.2** und **77 Prüfungen** mit echten SQLite-Verbindungen. GitHub Actions baut zusätzlich gegen beide Ziel-APIs und führt dieselben 77 Prüfungen mit einem echten **MySQL-8.4-Dienst** aus. Die Prüfungen umfassen Versions-/Fontauswahl, RGB-Komponenten, Config-Migration, signierte Lizenzen, parallele Claims/Käufe und Überweisungen, Überlauf-/Guthabenschutz, Ranglisten und Persistenz.

Die Live-Jobs starten echte **Paper-Server 1.21.10 und 26.3** mit verbundenen Testclients. Sie prüfen NPC-Klicks, Menü- und Inventarschutz, Warps, Rewards, Käufe, Überweisungen, Sichtbarkeit, Sprungplatten, Spielereinstellungen, einen vollständigen Serverneustart sowie Lizenzablauf und Reaktivierung. Hinzu kommen Profilwechsel, Hotbar-/Menüaktualisierung, RGB- und Fontkomponenten, Tab-/Sidebar-Schriften, ausgeblendete Scorezahlen sowie die Clientversionserkennung über ViaVersion. Der 26.3-Server wird über ViaVersion/ViaBackwards mit einem 1.21.10-Testclient angesprochen; eine manuelle grafische Prüfung ist darin nicht enthalten. Details: [Runtime-Tests](tests/runtime/README.md).

Der [Freigabelauf für v1.1.0](https://github.com/conlog06/lobbysystem-kekgames/actions/runs/45213097513) hat auf beiden Serverversionen jeweils **46 Live-Prüfungen und 50 MySQL-Prüfungen** erfolgreich ausgeführt. Ein Release wird erst nach erfolgreichen Build- und Live-Jobs freigegeben. Die tatsächlich verwendeten Serverbuilds und Einzelergebnisse stehen in den [beigefügten JSON-Berichten](https://github.com/conlog06/lobbysystem-kekgames/releases/tag/v1.1.0). Im eigenen Netzwerk zusätzlich die konkrete Welt, andere Plugins und Proxy-Ziele prüfen; die CI verwendet eine isolierte Testinstallation.

Änderungen: [CHANGELOG.md](CHANGELOG.md). Drittanbieter: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
