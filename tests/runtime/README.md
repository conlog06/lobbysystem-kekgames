# Live-Tests

Die Jobs in `.github/workflows/build.yml` prüfen die universelle Kunden-JAR auf echten Paper-Servern für 1.21.10 und 26.3, jeweils mit einem isolierten MySQL-8.4-Dienst.

## Umfang

- Zuerst laufen dieselben 77 Integrationsprüfungen wie beim SQLite-Build gegen MySQL: Versionen, Fontauswahl, RGB, Config-Migration, parallele Claims/Käufe und Überweisungen, Guthaben-/Überlaufschutz, Ranglisten, Einstellungen und Persistenz.
- Zwei Mineflayer-Clients verbinden sich mit dem laufenden Server. Sie senden echte Spiel-, Menü-, NPC- und Command-Pakete. Ein ausschließlich in Tests gebautes Probe-Plugin kontrolliert zusätzlich den Zustand des echten Bukkit-Servers.
- Die Clientfälle umfassen Hotbar, NPC-Menüs, Shift-/Zahlentastenschutz, Warps, Rewards, Lottery, Cosmetic-Käufe, Sichtbarkeit, Einstellungen, Baumodus, Sprungplatten, Profil und Ranglisten.
- Sprungplatten werden durch echte Clientbewegung ausgelöst. Geprüft werden der serverseitige Auslöser und das beim Client empfangene Aufwärts-Velocity-Paket.
- Der Server wird vollständig gestoppt und erneut gestartet; Kontostand, Cosmetics, NPC-Anzahl und bereits abgeholte Belohnungen werden danach erneut kontrolliert.
- Eine abgelaufene Testlizenz muss die Lobby sperren und Sitzungen/NPCs bereinigen. Anschließend wird eine gültige Lizenz über den normalen Befehl aktiviert und der Zustand erneut kontrolliert. Dabei wartet der Test auf Papers verschachtelte Command-Verarbeitung.
- `/lobbyui` schaltet auf dem laufenden Server zwischen automatischem, modernem und klassischem Profil sowie den eingebauten Schriften um. Geprüft werden tatsächliche Hotbar-/Menümaterialien, Item-/Inventartitel-Komponenten mit RGB und Font, Sidebar-/Tab-Fonts und ausgeblendete Scorezahlen. Die automatische Auswahl muss auch auf dem 26.3-Server den tatsächlichen 1.21.10-Client über ViaVersion erkennen.

## Grenzen und Reproduzierbarkeit

Die Bots sprechen Minecraft 1.21.10. Beim 26.3-Server übersetzen die festgelegten ViaVersion-/ViaBackwards-Versionen 5.12.2 die Pakete. Die JAR-Downloads dieser Abhängigkeiten werden mit SHA-256 geprüft. Mineflayer ist auf einen offiziellen Upstream-Commit mit dem erforderlichen Tick-End-Paket festgelegt. Die verwendete Paper-Buildnummer wird im Ergebnisbericht gespeichert.

Die Prüfung ist automatisiert und enthält keine manuelle Kontrolle von Grafik, Ressourcenpaketen, Proxy-Zielen oder einem echten Microsoft-Login des Owners. Die Lizenzsignaturen selbst werden in der Integrationssuite geprüft; zusätzlich darf ein Operator im Live-Test keine Owner-Lizenzen ausstellen.

## Trennung von Test und Release

`RuntimeSetup` erzeugt ein neues temporäres Signierschlüsselpaar und eine Kopie der Kunden-JAR mit dem zugehörigen Test-Prüfschlüssel. `RuntimeProbe` und der private Testschlüssel existieren nur im isolierten Serververzeichnis. Sie werden weder in der Kunden-JAR noch als Release-Dateien veröffentlicht.

Der Release-Job lädt die unveränderte, zuvor geprüfte Kunden-JAR aus dem Build-Job. Er benötigt erfolgreiche Live-Berichte für beide Versionen und kontrolliert, dass sich `main` seit dem getesteten Commit nicht verändert hat. Veröffentlicht werden Kunden-JAR, SHA-256-Prüfsumme und JSON-Testberichte.
