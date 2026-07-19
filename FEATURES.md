# EpicDuels — Feature-Roadmap

Priorisierte Liste fehlender Features. Stand: Juli 2026, Basis: Branch `bugfixes` (v2.0.0).

> **Update v3.0.0:** Priorität 1 ist komplett umgesetzt (1.1–1.5), dazu
> 3.4 (`/duel toggle`), 4.2 (`/duel reload`) und der Join-Handling-Toggle
> aus 4.5 (`lobby.handle-join`). Erledigte Punkte sind mit ✅ markiert.
> Ebenfalls Teil von v3.0.0: 3.3 (Spectate-GUI), 3.5 (Kit-Editor pro
> Spieler), 4.1 (Lokalisierung en/de, `lang/`-Dateien), 4.3 (PlaceholderAPI)
> und 4.4 (GUI-Refactoring auf InventoryHolder + PDC). `/duel reload` lädt
> jetzt auch die Sprachdateien neu.

---

## Priorität 1 — Gameplay-Grundlagen

### 1.1 Match-Zeitlimit & Unentschieden ✅ (v3.0.0)
Duelle können aktuell endlos laufen (Spieler können sich verstecken).
- Konfigurierbares Zeitlimit pro Duell (`config.yml`, z.B. `duel.time-limit-seconds: 300`, 0 = aus)
- Bossbar oder Actionbar-Countdown während des Duells
- Bei Ablauf: Unentschieden — kein Win/Loss für beide, Meldung + Teleport zur Lobby, Instanz-Welt aufräumen
- Gilt auch für Team-Duelle und Turnier-Matches (Turnier: bei Draw z.B. Sudden-Death-Verlängerung oder Münzwurf, konfigurierbar)

### 1.2 Forfeit-Command (`/duel forfeit` bzw. `/duel leave`) ✅ (v3.0.0)
- Spieler kann laufendes Duell/Team-Duell aufgeben → zählt als Niederlage
- Bestätigung per Klick-Nachricht (versehentliches Aufgeben vermeiden)
- Nutzt die bestehende `handleForfeit`-Logik aus `DuelManager`/`TeamDuelManager`

### 1.3 Rematch ✅ (v3.0.0)
- Nach Duell-Ende beiden Spielern eine klickbare `[REMATCH]`-Nachricht anzeigen (30s gültig)
- Bei beidseitigem Klick: gleiches Kit + gleiche Arena, neues Duell
- Command-Variante: `/duel rematch`

### 1.4 Best-of-N / Rundenmodus ✅ (v3.0.0)
- Challenge-Flow um Rundenauswahl erweitern (Bo1/Bo3/Bo5)
- Zwischen Runden: kurzer Reset (Teleport zu Spawns, Kit neu, Countdown), Instanz-Welt wiederverwenden; von Spielern gesetzte Blöcke zurücksetzen (die `playerPlacedBlocks`-Tracking-Struktur existiert bereits in `DuelInstance`)
- Punktestand-Anzeige (Titel/Actionbar: "Runde 2 — 1:1")

### 1.5 Duell mit eigenem Inventar (No-Kit) ✅ (v3.0.0)
- Im Kit-Select-GUI zusätzliche Option "Eigenes Inventar"
- Inventar wird beim Duell-Start gesichert und danach wiederhergestellt (wichtig: auch bei Disconnect/Crash — Persistenz z.B. als Datei)

---

## Priorität 2 — Kompetitiv & Statistiken

### 2.1 ELO-/Ranking-System
- ELO pro Spieler (Start z.B. 1000), Update nach jedem gewerteten Duell (klassische ELO-Formel, K-Faktor konfigurierbar)
- Getrennte Queues: `ranked` (ELO-Wertung, Kit vom System vorgegeben oder Kit-Pool) und `casual`
- ELO in Stats-GUI, `/duel stats`, Leaderboard (`/duel leaderboard elo`) und Hologramm-Typ `ELO` (HologramManager.Type erweitern)
- Matchmaking: bevorzugt Gegner mit ähnlicher ELO (Toleranz wächst mit Wartezeit)

### 2.2 Erweiterte Statistiken
- Win-Streak (aktuell + best), Kills insgesamt, per-Kit-Stats (Wins/Losses je Kit)
- Anzeige im Stats-GUI (`GUIManager.openStatsMenu`) und in `/duel stats <player>`

### 2.3 Datenbank-Backends: SQLite & MySQL
- `StatsProvider`-Interface existiert bereits (Supabase/Firebase) — zwei neue Implementierungen: `SQLiteProvider` (Default-Ersatz für stats.yml, Datei im Plugin-Ordner) und `MySQLProvider` (HikariCP)
- Migration: bestehendes stats.yml beim ersten Start importieren
- Alle neuen Stat-Felder (ELO, Streaks, per-Kit) über alle Backends unterstützen

### 2.4 Belohnungen & Economy (Vault)
- Optionale Vault-Integration (softdepend in plugin.yml)
- Konfigurierbare Rewards: Geld/Commands bei Sieg, Turnier-Sieg, Streak-Meilensteinen
- Wetteinsätze: `/duel challenge <player> bet <betrag>` — Einsatz wird eingefroren, Gewinner bekommt den Pot

---

## Priorität 3 — Social & Modi

### 3.1 Party vs. Party
- `/party duel <anderer Party-Owner>` — Challenge-Flow zwischen zwei Parties (Kit + Arena wählt der Herausforderer, Gegner-Owner akzeptiert)
- Nutzt die bestehende `TeamDuelInstance` mit festen Teams (Party A vs Party B) statt Shuffle-Split

### 3.2 Offene / server-weite Turniere
- Admin startet Turnier: `/duel tournament create <kit> [maxPlayers] [startDelay]` — Broadcast mit klickbarem `[JOIN]`
- Nicht mehr an Party (max. 8) gebunden; Bracket-Logik aus `TournamentManager` wiederverwenden
- Parallel laufende Matches auf mehrere Arenen verteilen (existiert), Zuschauer-Routing (existiert)
- Turnier-Rewards (siehe 2.4), Bracket-Anzeige als Buch oder GUI

### 3.3 Spectate-GUI ✅
- ✅ `/duel spectate` ohne Argument (und neuer Hauptmenü-Eintrag) öffnet ein paginiertes GUI mit allen laufenden 1v1-Duellen: Spieler-Köpfe ("A vs B"), Kit, Map, Runde, Dauer
- ✅ Klick → zuschauen (mit Re-Validierung, falls das Duell inzwischen endete); Verlassen wie bisher per `/duel spectate`
- Offen: Team-Duelle erscheinen nicht in der Liste (TeamDuelManager hat keinen Außenstehenden-Spectate-Mechanismus — eigenes Folge-Feature)

### 3.4 Challenge-Komfort (teilweise ✅)
- ✅ `/duel toggle` — Duell-Anfragen an sich deaktivieren/aktivieren (persistent, toggles.yml; v3.0.0)
- Cooldown für Anfragen an denselben Spieler (Spam-Schutz, z.B. 30s nach Deny)

### 3.5 Kit-Editor (pro Spieler) ✅
- `/duel kits [kit]` bzw. "Kit Editor"-Eintrag im Hauptmenü — jeder Spieler ordnet die Items jedes Kits in seine bevorzugten Slots (Hotbar, Rüstung, Offhand)
- Reines Umsortieren (nichts hinzufügen/entfernen/ändern), Speicherung pro Spieler in `playerkits.yml`, automatische Anwendung in 1v1, Matchmaking, Team-Duellen und Turnieren
- Veraltete Layouts (Kit wurde vom Admin geändert) fallen still auf das Standard-Layout zurück; Feature-Toggle `features.kit-editor`

---

## Priorität 4 — Admin, Integration & Technik

### 4.1 Konfigurierbare Nachrichten / Lokalisierung ✅
- ✅ Alle ~290 Spieler-Nachrichten (Chat, Actionbars, Titles, klickbare Buttons) in `lang/messages_<code>.yml` ausgelagert, MiniMessage-Format
- ✅ Zentrale `Messages`-Klasse (`dev.epicduels.i18n`) mit Platzhaltern (`<player>`, `<kit>`, `<arena>`, …); Klick-Kommandos über `{token}`-Ersetzung mit Tag-Escaping
- ✅ Mitgeliefert: en + de; Auswahl über Config-Key `language`, Fallback auf eingebautes Englisch, Reload via `/duel reload`
- Offen: GUI-Item-Namen/-Lore bleiben vorerst im Code (eigener Pass); wenige Admin-Fehlertexte aus `renameArena`/`renameKit` sind noch Englisch

### 4.2 `/duel reload` (teilweise ✅)
- ✅ Config zur Laufzeit neu laden (Permission `epicduels.admin`; v3.0.0)
- Offen: messages.yml (existiert noch nicht, siehe 4.1) und Stats-Backend-Wechsel zur Laufzeit (Provider neu initialisieren — aktuell Neustart nötig)

### 4.3 PlaceholderAPI ✅
- ✅ Placeholders: `%epicduels_wins%`, `%epicduels_losses%`, `%epicduels_winrate%`, `%epicduels_score%`, `%epicduels_in_duel%` (`dev.epicduels.hook.EpicDuelsExpansion`)
- ✅ softdepend PlaceholderAPI, Registrierung nur wenn installiert
- Offen: `%epicduels_elo%` / `%epicduels_streak%` folgen mit 2.1/2.2 (aktuell keine Daten dahinter — bewusst keine Fake-Werte)

### 4.4 GUI-Refactoring: PDC statt Titel/Displayname ✅
- ✅ Eigener `MenuHolder` (`dev.epicduels.gui`) mit `MenuType`-Enum, Kit-Kontext und Seite identifiziert jedes Menü — Titel sind nur noch Kosmetik
- ✅ Klick-Auflösung über PDC-Keys an den Button-Items (`epicduels:action/kit/arena/player`) statt Displayname-Parsing; Kit-Items in den editierbaren GUIs bleiben bewusst ungetaggt
- ✅ Nebeneffekte behoben: keine Titel-Kollisionen mit Fremd-Plugins mehr, Klicks ins eigene Inventar können keine Menü-Aktionen mehr auslösen, Arena-Liste hat funktionierende Pagination, Drag in die Kontrollzeile des Admin-Kit-Editors ist blockiert

### 4.5 Sonstiges
- bStats-Metriken (Plugin-Nutzung, Anzahl Duelle)
- Update-Checker (GitHub Releases / Modrinth API)
- API für andere Plugins: eigene Events (`DuelStartEvent`, `DuelEndEvent`, `TournamentEndEvent`, cancellable wo sinnvoll)
- Konfigurierbarer Countdown (Dauer, Sounds), konfigurierbare Nachricht-Broadcasts (Duell-Ergebnis global an/aus)
- ✅ Config-Toggle für Inventar-Wipe + Lobby-Teleport beim Join: `lobby.handle-join` (v3.0.0)

---

## Hinweise für die Umsetzung

- Architektur: Manager-Klassen werden in `EpicDuels.onEnable()` instanziiert und per Getter gereicht; neue Manager diesem Muster folgen. Überblick steht als Kommentar in `EpicDuels.java`.
- Arena-Instanzen: Jedes Duell kopiert die Template-Welt (`arena_template_<name>`) zu `arena_instance_<name>_<id>` — teure Operation; bei Best-of-N Instanz wiederverwenden statt neu kopieren.
- Alle Spieler-Interaktionen laufen über Adventure Components (kein Legacy-ChatColor).
- Nach jeder Änderung: `mvn -q -DskipTests package` muss grün sein (Java 21, Paper API 1.21.1).
