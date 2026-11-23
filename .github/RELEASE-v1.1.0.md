## KekGames LobbySystem 1.1.0

Plugin für Spigot/Paper 1.21.10 bis 26.3. Java 21 für 1.21.10, Java 25 für 26.x.

### Download und Installation

`LobbySystem-1.1.0.jar` herunterladen und auf den Lobby-Server in `plugins/` legen. Bei Updates die alte JAR ersetzen und Config/Datenbank behalten. Einrichtung und Owner-Lizenzvergabe stehen in der README. Den vorhandenen privaten Owner-Schlüssel weiterverwenden.

### Funktionen

- Navigator, Warps, Lobbyschutz, NPCs, Sidebar und Tablist.
- Coins, `/pay`, Ranglisten und Profilmenü.
- Cosmetics, tägliche Belohnungen, Lottery und persistente Einstellungen.
- Sprungplatten, Doppelsprung, Begrüßung und Ankündigungen.
- SQLite/MySQL und signierte Owner-Lizenzen.

### Automatisch geprüft

- API-Builds für Spigot 1.21.10 und 26.3, jeweils 50 Prüfungen auf SQLite.
- Dieselben Datenbankprüfungen gegen einen echten MySQL-8.4-Dienst.
- Echte Paper-Server 1.21.10 und 26.3 mit verbundenen Testclients: Menüklicks und Inventarschutz, Rewards, Käufe, Überweisungen, Sichtbarkeit, Sprungplatten, Restart-Persistenz, Lizenzablauf und Reaktivierung.
- Der 26.3-Server wird mit einem 1.21.10-Testclient über ViaVersion/ViaBackwards geprüft. Dies ist ein automatisierter Funktionstest; eine manuelle Prüfung der grafischen Darstellung ist darin nicht enthalten.

Die JSON-Testberichte und SHA-256-Prüfsumme liegen bei. Die Release-JAR enthält den originalen öffentlichen Prüfschlüssel. Temporäre Testschlüssel, Testplugins und Serverdateien sind keine Release-Bestandteile.
