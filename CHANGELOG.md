# Änderungen

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
