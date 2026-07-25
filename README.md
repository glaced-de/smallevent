# DesertJungleCore

Eigenständiges Desert-vs-Jungle-Event-System für Paper/ShreddedPaper 26.1.2. Es nutzt die Paper-Adventure-API, MiniMessage und eine SQLite-Datenbank; LuckPerms ist nicht erforderlich.

## Voraussetzungen

- Paper oder ShreddedPaper **26.1.2**
- **Java 25** – Paper 26.1+ setzt Java 25 voraus. Java 21 kann daher für diese Servergeneration kein lauffähiges Plugin erzeugen. Der Build kann auch mit einem neueren JDK erfolgen und erzeugt weiterhin Java-25-Bytecode.

## Build und Installation

```powershell
./gradlew.bat build
```

Die installierbare Datei liegt danach unter `build/libs/DesertJungleCore-1.0.0.jar`. Paper lädt den in `plugin.yml` deklarierten SQLite-Treiber beim Start automatisch. Kopiere sie in den Serverordner `plugins/` und starte den Server.

## Befehle

| Befehl | Recht | Zweck |
|---|---|---|
| `/team desert`, `/team jungle`, `/team leave` | `desertjungle.team` | Event-Team wählen/verlassen |
| `/team set <spieler> <desert|jungle>` | `desertjungle.team.admin` | Team administrativ setzen |
| `/event start|stop|pause|resume` | `desertjungle.event.admin` | Event verwalten |
| `/group assign <spieler> <gruppe>` | `desertjungle.group.admin` | Lokale Gruppe setzen |

## Konfiguration und Daten

- `config.yml`: Texte, MiniMessage-Farben, Teamlimit, Friendly Fire, Countdown und HUD.
- `groups.yml`: Beispielgruppen mit Gewicht, Prefix, Vererbung und Permissions.
- `database.db`: wird beim ersten Start angelegt. Tabelle `players(uuid, team, group_name, kills, deaths, assists)`.

## Architektur

`SqliteRepository` kapselt die Persistenz und führt JDBC-Zugriffe auf einem einzelnen I/O-Thread aus. `TeamService`, `GroupService` und `EventService` sind voneinander getrennte Anwendungsdienste. `HudService` aktualisiert Scoreboard, Tablist und ActionBar ohne das Scoreboard bei jedem Tick neu anzulegen.

Die Adapter für PlaceholderAPI, Vault, DiscordSRV, LuckPerms-Import, bStats und konkrete Anti-Cheat-Plugins sind bewusst noch nicht implementiert: Sie erfordern jeweils eine explizite Zielversion und/oder eine externe Laufzeitintegration. Das Kernplugin bleibt dadurch vollständig ohne Fremdabhängigkeiten lauffähig.
