KekGames LobbySystem 1.2.0 für Minecraft **1.21.10 bis 26.3**.

- Einheitliches KekGames-Branding, RGB-Palette und überarbeitete Menüs, Hotbar, Sidebar, Tablist und Actionbar.
- Automatische Profile für 1.21.x und 26.x; optionale Erkennung der tatsächlichen Clientversion über ViaVersion.
- Eingebaute Schriften `minecraft:default` und `minecraft:uniform`, eigene Ressourcenpaket-Fonts konfigurierbar.
- `/lobbyui status`, `/lobbyui profile auto|classic|modern` und `/lobbyui font auto|minecraft:default|minecraft:uniform` mit Setup-Permission.
- Config-Update mit Sicherung der alten Datei; eigene Texte, Lizenz, Owner-Schlüssel und Spielerdaten bleiben erhalten.

Die Kunden-JAR wird erst nach erfolgreichen API-Builds für Spigot 1.21.10/26.3, 77 SQLite-Prüfungen je Build, 77 Prüfungen mit echtem MySQL je Laufzeitjob und Live-Tests auf beiden Paper-Versionen veröffentlicht. JSON-Berichte der Live-Tests und eine SHA-256-Prüfsumme sind beigefügt. Die Bots verbinden sich als 1.21.10-Clients, auf 26.3 über ViaVersion/ViaBackwards. Eine manuelle grafische Kontrolle ist nicht enthalten.

**Download:** `LobbySystem-1.2.0.jar` in `plugins/` legen und die alte JAR ersetzen. Config und Datenbank behalten. Java 21 oder neuer, für Minecraft 26.x Java 25. Auf Paper stehen zusätzliche Komponenten-/Zahlenformat-APIs zur Verfügung; Spigot verwendet kompatible Darstellung, wo diese fehlen. Eigene Schriften benötigen ein passendes Ressourcenpaket.

Das persönliche Owner-Paket gehört ausschließlich dem Owner und wird nicht öffentlich bereitgestellt. Der vorhandene private Schlüssel gilt weiter; Kunden erhalten ausschließlich die Plugin-JAR und ihre Lizenz.
