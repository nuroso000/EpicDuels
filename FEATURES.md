# EpicDuels — Feature-Roadmap

Priorisierte Liste fehlender Features. Stand: Juli 2026, Basis: Branch `bugfixes` (v2.0.0).

---

## Priorität 1 — Gameplay-Grundlagen

### 1.1 Match-Zeitlimit & Unentschieden
Duelle können aktuell endlos laufen (Spieler können sich verstecken).
- Konfigurierbares Zeitlimit pro Duell (`config.yml`, z.B. `duel.time-limit-seconds: 300`, 0 = aus)
- Bossbar oder Actionbar-Countdown während des Duells
- Bei Ablauf: Unentschieden — kein Win/Loss für beide, Meldung + Teleport zur Lobby, Instanz-Welt aufräumen
- Gilt auch für Team-Duelle und Turnier-Matches (Turnier: bei Draw z.B. Sudden-Death-Verlängerung oder Münzwurf, konfigurierbar)

### 1.2 Forfeit-Command (`/duel forfeit` bzw. `/duel leave`)
- Spieler kann laufendes Duell/Team-Duell aufgeben → zählt als Niederlage
- Bestätigung per Klick-Nachricht (versehentliches Aufgeben vermeiden)
- Nutzt die bestehende `handleForfeit`-Logik aus `DuelManager`/`TeamDuelManager`

### 1.3 Rematch
- Nach Duell-Ende beiden Spielern eine klickbare `[REMATCH]`-Nachricht anzeigen (30s gültig)
- Bei beidseitigem Klick: gleiches Kit + gleiche Arena, neues Duell
- Command-Variante: `/duel rematch`

### 1.4 Best-of-N / Rundenmodus
- Challenge-Flow um Rundenauswahl erweitern (Bo1/Bo3/Bo5)
- Zwischen Runden: kurzer Reset (Teleport zu Spawns, Kit neu, Countdown), Instanz-Welt wiederverwenden; von Spielern gesetzte Blöcke zurücksetzen (die `playerPlacedBlocks`-Tracking-Struktur existiert bereits in `DuelInstance`)
- Punktestand-Anzeige (Titel/Actionbar: "Runde 2 — 1:1")

### 1.5 Duell mit eigenem Inventar (No-Kit)
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

### 3.3 Spectate-GUI
- `/duel spectate` ohne Argument öffnet GUI mit allen laufenden Duellen (Spieler-Köpfe, Kit, Dauer)
- Klick → zuschauen; Verlassen über Menü-Item in der Hotbar des Spectators

### 3.4 Challenge-Komfort
- `/duel toggle` — Duell-Anfragen an sich deaktivieren/aktivieren (persistent)
- Cooldown für Anfragen an denselben Spieler (Spam-Schutz, z.B. 30s nach Deny)

---

## Priorität 4 — Admin, Integration & Technik

### 4.1 Konfigurierbare Nachrichten / Lokalisierung
- Alle Spieler-Nachrichten (aktuell hartkodiert Englisch, verteilt über alle Klassen) in `messages.yml` auslagern, MiniMessage-Format
- Zentrale `Messages`-Klasse mit Platzhaltern (`<player>`, `<kit>`, `<arena>`, …)
- Mitgelieferte Übersetzungen: en, de

### 4.2 `/duel reload`
- Config + messages.yml zur Laufzeit neu laden (Permission `epicduels.admin`)
- Achtung: Stats-Backend-Wechsel zur Laufzeit sauber behandeln (Provider neu initialisieren)

### 4.3 PlaceholderAPI
- Placeholders: `%epicduels_wins%`, `%epicduels_losses%`, `%epicduels_winrate%`, `%epicduels_score%`, `%epicduels_elo%`, `%epicduels_streak%`, `%epicduels_in_duel%`
- softdepend PlaceholderAPI

### 4.4 GUI-Refactoring: PDC statt Titel/Displayname
- GUIs werden aktuell über den Fenstertitel erkannt und Klicks über den Item-Displaynamen aufgelöst (kollisionsanfällig, z.B. Kit namens "Random Map")
- Umbau auf `PersistentDataContainer`-Keys an den Items (`epicduels:action`, `epicduels:kit`, `epicduels:arena`) und/oder eigene `InventoryHolder`-Implementierung pro Menü
- Reine interne Verbesserung, kein Verhaltens-Change

### 4.5 Sonstiges
- bStats-Metriken (Plugin-Nutzung, Anzahl Duelle)
- Update-Checker (GitHub Releases / Modrinth API)
- API für andere Plugins: eigene Events (`DuelStartEvent`, `DuelEndEvent`, `TournamentEndEvent`, cancellable wo sinnvoll)
- Konfigurierbarer Countdown (Dauer, Sounds), konfigurierbare Nachricht-Broadcasts (Duell-Ergebnis global an/aus)
- README/Config-Hinweis oder Config-Toggle: Inventar-Wipe + Lobby-Teleport beim Join (`PlayerListener.onPlayerJoin`) setzt einen dedizierten Duels-Server voraus — optional abschaltbar machen

---

## Hinweise für die Umsetzung

- Architektur: Manager-Klassen werden in `EpicDuels.onEnable()` instanziiert und per Getter gereicht; neue Manager diesem Muster folgen. Überblick steht als Kommentar in `EpicDuels.java`.
- Arena-Instanzen: Jedes Duell kopiert die Template-Welt (`arena_template_<name>`) zu `arena_instance_<name>_<id>` — teure Operation; bei Best-of-N Instanz wiederverwenden statt neu kopieren.
- Alle Spieler-Interaktionen laufen über Adventure Components (kein Legacy-ChatColor).
- Nach jeder Änderung: `mvn -q -DskipTests package` muss grün sein (Java 21, Paper API 1.21.1).
