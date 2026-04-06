# FAQ (Häufig Gestellte Fragen)

Die wichtigsten Fragen und Antworten zu EpicDuels.

---

## Installation & Setup

### F: Welche Minecraft-Version brauche ich?
**A:** EpicDuels benötigt **Paper 1.21.1 oder höher**. Ältere Versionen werden nicht unterstützt.

### F: Auf welcher Minecraft-Version läuft der Server?
**A:** Der Server muss **1.21.1+** sein. Du kannst die Version überprüfen mit:
```bash
/version
```

### F: Brauche ich Java 21?
**A:** Ja! EpicDuels benötigt **Java 21 oder höher**. Überprüfe deine Java-Version:
```bash
java -version
```

### F: Wo lade ich EpicDuels herunter?
**A:** Hier:
- 📥 [GitHub Releases](https://github.com/nuroso000/epicduels/releases)
- 🎮 [Modrinth](https://modrinth.com/plugin/epicduels)

### F: Wo platziere ich die JAR-Datei?
**A:** In den `plugins/` Ordner deines Servers:
```
server/
└── plugins/
    └── EpicDuels-0.2.1.jar
```

### F: Der Server startet nicht nach Installation
**A:** Überprüfe:
1. ✅ Java-Version ist 21+
2. ✅ JAR-Datei ist in `plugins/` 
3. ✅ JAR-Datei ist nicht beschädigt
4. ✅ Schau in die `logs/latest.log`

---

## Gameplay & Features

### F: Was ist ein "Duel"?
**A:** Ein 1v1 Kampf zwischen zwei Spielern. Einer gewinnt, einer verliert. Stats werden aktualisiert.

### F: Kann ich mit mehr Spielern duellieren?
**A:** Nein, EpicDuels ist nur 1v1. Multiplayer-Duels sind nicht unterstützt.

### F: Was ist die "Queue"?
**A:** Eine Warteschlange für automatisches Matchmaking. Du wartest, bis ein anderer Spieler das gleiche Kit wählt, dann werdet ihr automatisch gematcht.

### F: Wie lange dauert ein Duel?
**A:** So lange, bis ein Spieler stirbt oder disconnectet. Durchschnittlich 2-5 Minuten.

### F: Kann ich während eines Duels bauen?
**A:** Ja! Du kannst:
- ✅ Blöcke platzieren
- ✅ Deine eigenen Blöcke brechen
- ❌ Original-Arena-Blöcke nicht brechen
- ❌ Gegners Blöcke nicht brechen

### F: Was passiert, wenn beide Spieler sterben?
**A:** Der Spieler, der zuerst starb, verliert. Der andere gewinnt.

### F: Kann ich Items mitbringen?
**A:** Nein! Du bekommst die Items vom Kit. Deine Items werden nicht benutzt.

### F: Werden meine Stats gelöscht?
**A:** Nein, Stats sind permanent in `stats.yml`. Sie werden nie automatisch gelöscht.

---

## Kits & Arenen

### F: Wie erstelle ich ein Kit?
**A:** 
1. Rüste dich mit der gewünschten Ausrüstung aus
2. `/duel kit create my_kit`
3. Fertig!

### F: Kann ich ein Kit nach Erstellung ändern?
**A:** Ja! Mit `/duel kit edit my_kit` kannst du Items ändern.

### F: Wie viele Kits kann ich erstellen?
**A:** Unbegrenzt!

### F: Wie erstelle ich eine Arena?
**A:**
1. `/duel arena create my_arena` — Teleportiert dich ins Template
2. Baue deine Arena
3. Setze Spawns: `/duel arena setspawn1` und `/duel arena setspawn2`
4. `/duel arena save` — Speichert die Arena

### F: Kann ich eine Arena nach Erstellung bearbeiten?
**A:** Ja! Mit `/duel arena tp my_arena` kannst du zurück ins Template und bearbeiten.

### F: Wie viele Arenen kann ich erstellen?
**A:** Unbegrenzt, aber bedenke:
- Jede Arena braucht Speicherplatz
- Jedes Duel erstellt eine Welt-Kopie
- Zu viele Arenen = Performance-Probleme

### F: Werden Arenen-Welten gelöscht?
**A:** Ja! Nach jedem Duel wird die Welt-Kopie automatisch gelöscht. Sauberes System, kein Speicher-Problem.

### F: Können Spieler eine Arena nicht sehen?
**A:** Überprüfe:
1. Arena hat mindestens einen Spawn gesetzt
2. Arena hat Status `ready: true`
3. Spieler kann das Kit nutzen

### F: Kann ich das Arena-Icon ändern?
**A:** Ja! Halte ein Item und nutze `/duel arena seticon my_arena`.

---

## Statistiken

### F: Wo werden Statistiken gespeichert?
**A:** In `plugins/EpicDuels/stats.yml`

### F: Kann ich Statistiken anschauen?
**A:** Ja, mit `/duel stats` oder `/duel stats <player>`

### F: Kann ich Statistiken zurücksetzen?
**A:** Ja, aber du musst die `stats.yml` Datei manuell bearbeiten:
```yaml
stats:
  "player-uuid":
    wins: 0
    losses: 0
    totalGames: 0
```

### F: Wie wird Win Rate berechnet?
**A:**
```
Win Rate (%) = (Wins / Total Games) * 100
```

**Beispiel:** 15 Wins / 30 Duels = 50% Win Rate

### F: Werden Stats für jeden Spieler einzeln?
**A:** Ja, jeder Spieler hat eigene Stats.

---

## Administration

### F: Wie gebe ich Spielern Berechtigungen?
**A:** Mit einem Permission-Manager wie LuckPerms:
```bash
/lp user <player> permission set epicduels.duel true
```

### F: Welche Berechtigungen gibt es?
**A:**
- `epicduels.admin` — Admin-Befehle
- `epicduels.duel` — Duels spielen
- `epicduels.stats` — Stats ansehen

### F: Können Spieler Arenen selbst erstellen?
**A:** Nein, nur Admins mit `epicduels.admin` können Arenen erstellen.

### F: Was ist ein "Void World Generator"?
**A:** Ein Custom Generator, der leere Welten erzeugt. Füge zu `bukkit.yml` hinzu:
```yaml
worlds:
  world:
    generator: EpicDuels
```

### F: Muss ich den Server neustarten nach Konfiguration?
**A:** Ja, immer nach Änderungen in `bukkit.yml`. Für andere Dateien manchmal nicht (aber empfohlen).

---

## Probleme & Fehler

### F: Plugin lädt nicht
**A:** Überprüfe:
1. Java 21+
2. Paper 1.21.1+
3. Server-Logs für Fehlermeldungen
4. JAR-Datei nicht beschädigt

### F: Spieler können `/duel` nicht ausführen
**A:** Überprüfe Berechtigungen:
```bash
/lp user <player> permission check epicduels.duel
```

### F: Arenen-Welten werden nicht gelöscht
**A:** Das ist normal! Nach dem Duel wird die Welt gelöscht. Manchmal dauert es ein paar Sekunden.

### F: Stats werden nicht gespeichert
**A:** Überprüfe:
1. Datei-Berechtigungen
2. Server-Logs auf Fehler
3. `stats.yml` existiert und ist beschreibbar

### F: Duel startet nicht
**A:** Überprüfe:
1. ✅ Mindestens 1 Arena existiert
2. ✅ Arena hat 2 Spawns
3. ✅ Arena hat Status `ready`
4. ✅ Mindestens 1 Kit existiert

### F: Spieler teleportiert nicht zu Arena
**A:** Überprüfe:
1. Arena-Welt existiert
2. Spawn-Koordinaten sind gültig
3. Server-Logs auf Fehler

---

## Performance & Optimierung

### F: Das Plugin ist langsam
**A:** 
1. ✅ Stelle sicher, dass der Server genug RAM hat (mindestens 4GB)
2. ✅ Begrenzte Anzahl Arenen (z.B. 10)
3. ✅ Vereinfache Arena-Komplexität
4. ✅ Überprüfe andere Plugins

### F: Server crasht beim Duel
**A:**
1. Überprüfe RAM
2. Überprüfe Logs auf Out of Memory
3. Reduziere Anzahl Spieler gleichzeitig
4. Überprüfe andere Plugins auf Konflikt

### F: Spieler laggen während Duel
**A:**
1. Überprüfe Arena-Größe (nicht zu groß)
2. Überprüfe Partikel-Effekte
3. Überprüfe andere Plugins
4. Überprüfe Internet-Verbindung

---

## Lizenz & Rechtliches

### F: Kann ich EpicDuels kommerziell nutzen?
**A:** Ja, du darfst es auf einem Geld-verdienenden Server nutzen. **Aber:** Du darfst das Plugin selbst nicht verkaufen.

### F: Kann ich EpicDuels modifizieren?
**A:** Ja! Es ist Open Source (CC BY-NC-SA 4.0).

### F: Muss ich den Code offen lassen?
**A:** Ja, wenn du es weitergibst, muss der Code offen bleiben.

### F: Wer hat das Plugin entwickelt?
**A:** [nuroso000](https://github.com/nuroso000) auf GitHub.

---

## Weitere Hilfe

### F: Meine Frage ist nicht hier beantwortet
**A:** 
- 📖 Schau die [Dokumentation](./01-Home.md)
- 🎮 [Admin Guide](./04-Admin-Guide.md)
- 📋 [Commands Reference](./05-Commands.md)
- 🔐 [Permissions Guide](./06-Permissions.md)

### F: Ich möchte einen Bug melden
**A:** Öffne ein Issue auf [GitHub](https://github.com/nuroso000/epicduels/issues)

### F: Ich möchte eine Feature vorschlagen
**A:** Öffne ein Issue auf [GitHub](https://github.com/nuroso000/epicduels/issues) mit "Feature Request"

### F: Ich möchte beitragen
**A:** Siehe [Development Guide](./08-Development.md) und [CONTRIBUTING.md](../CONTRIBUTING.md)

---

## Checklisten

### Installation Checklist
- [ ] Java 21+ installiert
- [ ] Paper 1.21.1+ installiert
- [ ] EpicDuels JAR heruntergeladen
- [ ] JAR in `plugins/` platziert
- [ ] Server gestartet
- [ ] Plugin geladen (Logs überprüfen)

### Setup Checklist
- [ ] Lobby mit `/duel setlobby` gesetzt
- [ ] Mindestens 1 Arena erstellt
- [ ] Arena hat 2 Spawns gesetzt
- [ ] Arena gespeichert
- [ ] Mindestens 1 Kit erstellt
- [ ] Berechtigungen konfiguriert

### First Duel Checklist
- [ ] Spieler hat `epicduels.duel` Permission
- [ ] Spieler kennt `/duel` Befehl
- [ ] Arena und Kit existieren
- [ ] `/duel` öffnet GUI
- [ ] Spieler kann Challenge akzeptieren
- [ ] Duel startet erfolgreich

---

**Viel Spaß mit EpicDuels! 🎮**

---

*Zuletzt aktualisiert: 2026-04-06*
