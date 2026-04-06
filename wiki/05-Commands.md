# Commands Reference

Vollständige Liste aller EpicDuels-Befehle.

## 📋 Übersicht

- **[Spieler-Befehle](#spieler-befehle)** — Für normale Spieler
- **[Admin-Befehle](#admin-befehle)** — Für Server-Administratoren
- **[Shorthand](#shorthand)** — Abkürzungen

---

## Spieler-Befehle

Alle Spieler können diese Befehle nutzen (wenn sie die Berechtigung haben).

### Menü

```bash
/duel
/duel menu
```
Öffnet das Hauptmenü mit drei Optionen:
- Challenge
- Stats
- Queue

**Permission:** `epicduels.duel`

---

### Challenges

#### Challenge starten
```bash
/duel challenge <player>
```

Öffnet eine GUI zum Herausfordern:
1. Spieler auswählen (bereits ausgewählt)
2. Kit auswählen
3. Map auswählen

Der Gegenspieler erhält eine Accept/Deny Anfrage.

**Permission:** `epicduels.duel`

**Beispiel:**
```bash
/duel challenge NotchMC
```

#### Challenge akzeptieren
```bash
/duel accept [player]
```

Akzeptiere eine eingegangene Challenge.

**Permission:** `epicduels.duel`

**Beispiel:**
```bash
/duel accept
/duel accept Steve
```

#### Challenge ablehnen
```bash
/duel deny [player]
```

Lehne eine eingegangene Challenge ab.

**Permission:** `epicduels.duel`

**Beispiel:**
```bash
/duel deny
/duel deny Steve
```

#### Challenge stornieren
```bash
/duel cancel
```

Bricht deine aktuelle ausgehende Challenge-Anfrage ab.

**Permission:** `epicduels.duel`

---

### Statistiken

```bash
/duel stats [player]
```

Zeigt Duel-Statistiken an:
- **Wins** — Gewonnene Duels
- **Losses** — Verlorene Duels
- **Total** — Gesamte Duels
- **Win Rate** — Prozentsatz Wins

**Permission:** `epicduels.stats`

**Beispiel:**
```bash
/duel stats              # Deine eigenen Stats
/duel stats NotchMC      # Stats von NotchMC
```

---

### Matchmaking Queue

#### Queue beitreten
```bash
/duel queue <kit>
```

Tritt der Matchmaking-Queue für ein Kit bei.

Das Plugin wartet auf einen anderen Spieler:
- Beide Spieler müssen das gleiche Kit wählen
- Wenn 2 Spieler queued sind → automatisches Match
- Beide werden zu einer zufälligen Arena teleportiert
- Duel startet nach 5-Sekunden Countdown

**Action Bar** zeigt aktuelle Queue-Größe.

**Permission:** `epicduels.duel`

**Beispiel:**
```bash
/duel queue pvp_sword
/duel queue speed_pvp
```

#### Queue verlassen
```bash
/duel queue leave
```

Verlasse die aktuelle Matchmaking-Queue.

**Permission:** `epicduels.duel`

---

## Admin-Befehle

Nur Administratoren mit `epicduels.admin` Permission können diese Befehle nutzen.

### Arena Management

#### Arena erstellen
```bash
/duel arena create <name>
```

Erstellt eine neue Arena.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
/duel arena create pvp_1
/duel arena create sky_arena
```

#### Spawn 1 setzen
```bash
/duel arena setspawn1
```

Setzt deinen aktuellen Standort als Spawn-Punkt 1 (muss in Arena-Welt sein).

**Permission:** `epicduels.admin`

#### Spawn 2 setzen
```bash
/duel arena setspawn2
```

Setzt deinen aktuellen Standort als Spawn-Punkt 2 (muss in Arena-Welt sein).

**Permission:** `epicduels.admin`

#### Arena speichern
```bash
/duel arena save
```

Speichert die aktuelle Arena und markiert sie als "ready".

**Permission:** `epicduels.admin`

#### Arena löschen
```bash
/duel arena delete <name>
```

Löscht eine Arena permanent.

**Permission:** `epicduels.admin`

**⚠️ Warnung:** Dies kann nicht rückgängig gemacht werden!

**Beispiel:**
```bash
/duel arena delete pvp_1
```

#### Arena-Liste
```bash
/duel arena list
```

Zeigt alle Arenen mit ihrem Status (ready/building).

**Permission:** `epicduels.admin`

#### Zu Arena teleportieren
```bash
/duel arena tp <name>
```

Teleportiert dich zur Arena zum Bearbeiten.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
/duel arena tp pvp_1
```

#### Arena-Icon setzen
```bash
/duel arena seticon <name>
```

Setzt das Icon der Arena auf das Item in deiner Hand.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
# Halten Sie einen Grass Block in der Hand
/duel arena seticon pvp_1
```

---

### Kit Management

#### Kit erstellen
```bash
/duel kit create <name>
```

Speichert deine aktuelle Inventar als Kit.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
/duel kit create survival_pvp
/duel kit create ranged_kit
```

#### Kit löschen
```bash
/duel kit delete <name>
```

Löscht ein Kit permanent.

**Permission:** `epicduels.admin`

**⚠️ Warnung:** Spieler können dieses Kit nicht mehr nutzen!

**Beispiel:**
```bash
/duel kit delete old_kit
```

#### Kit bearbeiten
```bash
/duel kit edit <name>
```

Öffnet eine Chest-GUI zum Bearbeiten des Kits.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
/duel kit edit survival_pvp
```

#### Kit anschauen
```bash
/duel kit preview <name>
```

Zeigt die Kit-Items in einer Read-Only Chest-GUI.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
/duel kit preview survival_pvp
```

#### Kit-Liste
```bash
/duel kit list
```

Zeigt alle verfügbaren Kits.

**Permission:** `epicduels.admin`

#### Kit-Icon setzen
```bash
/duel kit seticon <name>
```

Setzt das Icon des Kits auf das Item in deiner Hand.

**Permission:** `epicduels.admin`

**Beispiel:**
```bash
# Halten Sie einen Diamond Sword in der Hand
/duel kit seticon survival_pvp
```

---

### Lobby Management

#### Lobby-Spawn setzen
```bash
/duel setlobby
```

Setzt deinen aktuellen Standort als Lobby-Spawn.

**Permission:** `epicduels.admin`

---

## Shorthand

Alle `/duel` Befehle funktionieren auch mit der Abkürzung `/d`:

```bash
/d                       # = /duel
/d menu                  # = /duel menu
/d challenge Steve       # = /duel challenge Steve
/d accept                # = /duel accept
/d deny                  # = /duel deny
/d cancel                # = /duel cancel
/d stats                 # = /duel stats
/d queue pvp             # = /duel queue pvp
/d queue leave           # = /duel queue leave

# Admin-Befehle
/d arena create pvp_1    # = /duel arena create pvp_1
/d arena list            # = /duel arena list
/d kit list              # = /duel kit list
/d kit create my_kit     # = /duel kit create my_kit
```

---

## Permission-Übersicht

| Befehl | Permission | Default |
|---|---|---|
| `/duel` (Menu) | — | Alle |
| `/duel challenge` | `epicduels.duel` | Alle |
| `/duel accept/deny/cancel` | `epicduels.duel` | Alle |
| `/duel stats` | `epicduels.stats` | Alle |
| `/duel queue` | `epicduels.duel` | Alle |
| Alle Arena-Befehle | `epicduels.admin` | OP |
| Alle Kit-Befehle | `epicduels.admin` | OP |
| `/duel setlobby` | `epicduels.admin` | OP |

---

## Fehlerhafte Befehle

Wenn du einen Befehl falsch eingibst:

```bash
/duel invalid_command
# → Error: Unknown command
```

Überprüfe:
1. Rechtschreibung
2. Erforderliche Parameter
3. Berechtigungen
4. Gültige Kit/Arena-Namen

---

## Nächste Schritte

- 🔐 **Berechtigungen:** [Permissions Guide](./06-Permissions.md)
- 🎮 **Spielen:** [Player Guide](./03-Player-Guide.md)
- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
