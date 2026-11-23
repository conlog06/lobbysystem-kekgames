# Änderungen

## 1.2.0

- Zentrales KekGames-Branding mit RGB-Palette für Prefix, Menüs, Hotbar, Sidebar, Tablist und Actionbar.
- Automatische Designprofile für 1.21.x und 26.x, optional anhand der tatsächlichen ViaVersion-Clientversion.
- Wählbare eingebaute Schriften `minecraft:default` und `minecraft:uniform`; eigene Ressourcenpaket-Fonts konfigurierbar.
- Öffentliche Komponenten-APIs auf Paper, kompatible Textdarstellung bei fehlenden Spigot-APIs; keine NMS-Abhängigkeit.
- Modernes Profil mit dunklen Menüs und anderen Hotbar-Icons; Materialalternativen wie `TRIAL_KEY|TNT`.
- `/lobbyui status`, `profile` und `font` mit Setup-Permission und sofortiger Aktualisierung.
- Sidebar mit Onlinezahl, Serverversion und genauer Spielzeit; optional ausgeblendete Scorezahlen.
- Config-Migration mit Sicherung der alten Datei, Erhalt eigener Texte, Permissions, Lizenz und Installations-ID.
- 77 Integrationsprüfungen sowie erweiterte Live-Tests für Profile, Schriften, RGB, Sidebar und ViaVersion.

## 1.1.0

- `/coins` und `/balance` für den eigenen Kontostand.
- `/pay` mit atomaren Überweisungen, Offline-Empfängern, Betragslimit, Cooldown, Selbstzahlungs- und Überlaufschutz.
- `/lobbytop` und `/top` mit Coins-/Spielzeit-Ranglisten und Kategorieumschaltung.
- `/profile` und `/profil` sowie Profilzugriff über den Hotbar-Kopf.
- Konfigurierbare Sprungplatten, Begrüßungstitel und rotierende Ankündigungen.
- Alte Config-Dateien erhalten fehlende Permission-, Text- und Menüschlüssel automatisch.
- Entfernte Scoreboard-Teams werden beim Verlassen der Lobby bereinigt.
- Reward- und Einstellungs-NPC können im selben Chunk stehen, ohne sich beim Neuaufbau gegenseitig zu entfernen.
- Shift- und Zahlentasten-Klicks bleiben in Menüs blockiert und lösen keine Navigator-Aktion mehr aus.
- Erwartbare Eingabefehler asynchroner Datenbankaktionen erscheinen als verständlicher Spielerhinweis.
- Integrationstests laufen auch bei `mvn test`/`mvn verify`; 50 lokale Prüfungen.
- GitHub Actions baut gegen Spigot 1.21.10 und 26.3, prüft das Kundenpaket und stellt die JARs als Artefakte bereit.

## 1.0.0

- Navigator, Lobbyschutz, NPCs, Coins, Tagesbelohnungen, Lottery, Cosmetics, Einstellungen, Sidebar, Tablist und Owner-Lizenzen.
