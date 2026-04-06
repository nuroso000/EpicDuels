# Player Guide

Alles was Spieler über EpicDuels wissen müssen.

## Hauptmenü

```
/duel menu
```

Das Hauptmenü hat drei Bereiche:

### 1. Challenge
Fordere einen Spieler zu einem Duel heraus:
- Wähle einen **Online-Spieler**
- Wähle ein **Kit** (Ausrüstung)
- Wähle eine **Arena** (Map)

Der Gegenspieler erhält eine Anfrage mit Accept/Deny-Buttons (30 Sekunden Timeout).

### 2. Stats
Sehe deine Duel-Statistiken:
- **Wins** — Gewonnene Duels
- **Losses** — Verlorene Duels
- **Total** — Gesamt Duels
- **Win Rate** — Prozentsatz Wins

```
/duel stats [player]  # Deine oder Spielers Stats ansehen
```

### 3. Queue (Matchmaking)
Trete einer Matchmaking-Queue bei und werde automatisch gegen einen anderen Spieler gematcht:

```
/duel queue <kit>
```

**Wie es funktioniert:**
1. Du trittst der Queue für ein Kit bei
2. Das Action Bar zeigt die aktuelle Queue-Größe
3. Wenn ein anderer Spieler für das gleiche Kit queued, werdet ihr automatisch gematcht
4. Ihr werdet in eine zufällige Arena teleportiert
5. Das Duel beginnt nach einem 5-Sekunden Countdown

**Queue verlassen:**
```
/duel queue leave
```

---

## Duel-Ablauf

### Phase 1: Countdown
- 5 Sekunden vor dem Start
- Beide Spieler sind am Spawn eingefroren
- Title zeigt den Countdown an

### Phase 2: Kampf
- Du kannst dich bewegen und angreifen
- Der Gegner kann sich auch bewegen und angreifen
- Block-Placement ist möglich, aber:
  - ✅ Deine eigenen Blöcke kannst du zerstören
  - ❌ Original-Arena-Blöcke können nicht zerstört werden
  - ❌ Gegners Blöcke können nicht zerstört werden

### Phase 3: Ende
- **Gewinner:** Wer den anderen Spieler eliminiert oder der Gegner disconnectet
- 3-Sekunden Victory Screen
- Rückkehr zur Lobby

### Nach dem Duel
- **Stats werden aktualisiert** (Win/Loss)
- **Welt wird gelöscht** (Cleanup)
- Du kannst direkt ein neues Duel starten

---

## Commands für Spieler

| Befehl | Beschreibung |
|---|---|
| `/duel` | Öffne das Hauptmenü |
| `/duel menu` | Öffne das Hauptmenü (alternative) |
| `/duel challenge <player>` | Fordere einen Spieler heraus |
| `/duel accept [player]` | Akzeptiere eine Duel-Anfrage |
| `/duel deny [player]` | Lehne eine Duel-Anfrage ab |
| `/duel cancel` | Breche deine aktuelle Anfrage ab |
| `/duel stats [player]` | Sehe Statistiken |
| `/duel queue <kit>` | Trete einer Matchmaking-Queue bei |
| `/duel queue leave` | Verlasse die Queue |

**Shorthand:** Alle `/duel` Befehle funktionieren auch mit `/d`

```bash
/d              # Shorthand für /duel
/d challenge NotchMC    # Shorthand für /duel challenge NotchMC
/d queue pvp    # Shorthand für /duel queue pvp
```

---

## Strategien & Tipps

### Kit-Wahl
- 🗡️ **Sword-Kits** — Nahkampf, direkt im Kampf gut
- 🏹 **Ranged-Kits** — Fernkampf, Positionierung wichtig
- 🛡️ **Tank-Kits** — Viele Rüstung, länger halten
- ⚡ **Speed-Kits** — Schnelligkeit, Mobilität

### Arena-Dynamik
- Nutze die **Höhen und Tiefen** der Arena für Taktik
- Denke über deine **Positionen** nach
- Achte auf **Fall-Schaden**

### Queue-Tipps
- Die **gleiche Queue für alle** — Es spielt keine Rolle, welche Arena
- **Action Bar** zeigt die aktuelle Anzahl wartender Spieler
- Queue ist **Anonymous** — Du weißt nicht, gegen wen du spielst

---

## Berechtigungen

Mit diesen Berechtigungen kannst du was machen:

| Permission | Zugang |
|---|---|
| `epicduels.duel` | Challenges, Queue, Accept/Deny |
| `epicduels.stats` | Stats anschauen |

Standard: Alle Spieler haben diese Berechtigungen.

---

## Häufige Fragen

**F: Können mehrere Spieler gleichzeitig duellieren?**
A: Nein, EpicDuels ist 1v1 only. Jedes Duel läuft in einer separaten Welt-Kopie.

**F: Kann ich während eines Duels bauen?**
A: Ja! Du kannst Blöcke platzieren, aber:
   - Du kannst nur Blöcke brechen, die du selbst platziert hast
   - Original-Arena-Blöcke können nicht zerstört werden

**F: Was passiert, wenn ich disconnecte?**
A: Der andere Spieler gewinnt automatisch. Deine Stats werden aktualisiert.

**F: Wie oft kann ich duellieren?**
A: Unbegrenzt! Es gibt kein Limit für Anzahl oder Zeit.

**F: Werden meine Stats gelöscht?**
A: Nein, Stats sind permanent und werden lokal gespeichert.

👉 **Mehr Fragen:** [FAQ](./09-FAQ.md)

---

## Nächste Schritte

- 🏗️ **Arena-Details:** Frag deinen Admin, welche Arenen verfügbar sind
- 📊 **Stats verfolgen:** Nutze `/duel stats` um deine Fortschritte zu sehen
- 🎮 **Spielen:** Starte dein erstes Duel mit `/duel` oder `/d queue <kit>`
