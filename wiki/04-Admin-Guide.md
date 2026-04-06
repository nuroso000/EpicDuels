# Admin Guide

Server-Administrator Guide für EpicDuels Verwaltung.

## Anforderungen

- **OP-Berechtigungen** oder `epicduels.admin` Permission
- **Zugriff auf die Server-Konsole**
- Minecraft **1.21.1+** und **Java 21+**

---

## Arena Management

### Arena erstellen

```bash
/duel arena create myarena
```

Das Plugin:
1. Erstellt eine neue Void-World (`epicduels_myarena`)
2. Teleportiert dich in Creative Mode
3. Du bist jetzt im Arena-Template

### Arena bauen

Im Arena-Template kannst du frei bauen:
- ✅ Blöcke platzieren & brechen
- ✅ Creative Mode verwenden
- ✅ Gelande formen nach Belieben

**Tipps zum Arena-Design:**
- Schaffe faire Spawn-Punkte (nicht zu nah beieinander)
- Nutze Höhen und Tiefen für taktisches Gameplay
- Vermeide zu enge Räume für Sword-Kits
- Weite Räume für Ranged-Kits ideal

### Spawn-Punkte setzen

Stehe an Position 1 (z.B. links):
```bash
/duel arena setspawn1
```

Gehe zu Position 2 (z.B. rechts):
```bash
/duel arena setspawn2
```

**Wichtig:**
- Beide Spawns sollten **gleiche Y-Koordinate** haben (falls möglich)
- **Mindestens 10 Blöcke Abstand** zwischen den Spawns
- **Keine Wasser oder Lava** an Spawn-Positionen

### Arena speichern

```bash
/duel arena save
```

Die Arena wird:
1. Gespeichert in `arenas.yml`
2. Markiert als "ready" (spielbar)
3. Zum Template gemacht (kann wieder bearbeitet werden)
4. Du wirst zur Lobby teleportiert

### Arena-Icon setzen

Das Icon wird im GUI angezeigt:

```bash
# Halte ein Item in der Hand (z.B. Grass Block, Cobblestone)
/duel arena seticon myarena
```

Das Item wird als Icon für die Arena gespeichert.

### Arena Liste anschauen

```bash
/duel arena list
```

Zeigt alle Arenen mit Status:
- ✅ **ready** — Spielbar
- ⏳ **building** — Wird noch gebaut

### Zu Arena-Template gehen

```bash
/duel arena tp myarena
```

Teleportiert dich ins Template zum Bearbeiten.

### Arena löschen

```bash
/duel arena delete myarena
```

**Vorsicht:** Löscht Arena und alle Duels-Daten für diese Arena!

---

## Kit Management

### Kit erstellen

1. Rüste dich mit der gewünschten Ausrüstung aus
   - Armor
   - Weapon
   - Items in Inventory
   - Offhand Item

2. Führe aus:
```bash
/duel kit create pvp_sword
```

Das Plugin speichert deine aktuelle Inventar als Kit.

### Kit-Vorlagen

**Beispiel-Kits:**

```
pvp_sword:
  - Full Diamond Armor
  - Diamond Sword
  - 32x Blocks

speed_pvp:
  - Leather Armor
  - Diamond Sword
  - Speed Potion
  - Blocks

ranged:
  - Full Iron Armor
  - Bow + 64x Arrows
  - Blocks
```

### Kit bearbeiten

```bash
/duel kit edit pvp_sword
```

Öffnet die Kit-Items in einer Chest-GUI:
- Nimm Items raus oder rein
- Ordne Items an
- Schließe die Chest

Das Kit wird automatisch aktualisiert.

### Kit anschauen (Read-Only)

```bash
/duel kit preview pvp_sword
```

Zeigt die Kit-Items in einer Chest (kann nicht bearbeitet werden).

### Kit-Icon setzen

```bash
# Halte ein Item in der Hand (z.B. Diamond Sword)
/duel kit seticon pvp_sword
```

Das Item wird als Icon im GUI angezeigt.

### Kit Liste anschauen

```bash
/duel kit list
```

Zeigt alle verfügbaren Kits.

### Kit löschen

```bash
/duel kit delete pvp_sword
```

**Vorsicht:** Spieler können dieses Kit nicht mehr nutzen!

---

## Lobby Setup

### Lobby-Spawn setzen

Teleportiere dich an den Ort, wo Spieler landen sollen:

```bash
/duel setlobby
```

Das ist der Punkt, wo Spieler mit `/duel` landen und das Hauptmenü sehen.

---

## Dateistruktur

```
plugins/EpicDuels/
├── config.yml          # Lobby-Position
│   └── lobbySpawn:
│       location: "world,x,y,z,yaw,pitch"
│
├── arenas.yml          # Alle Arenen
│   ├── Arena-Name
│   │   ├── spawn1: {x, y, z}
│   │   ├── spawn2: {x, y, z}
│   │   ├── icon: <Item>
│   │   └── worldName: epicduels_<name>
│
├── kits.yml            # Alle Kits
│   ├── Kit-Name
│   │   ├── inventory: <Base64>
│   │   ├── armor: <Base64>
│   │   └── icon: <Item>
│
└── stats.yml           # Spieler-Statistiken
    └── PlayerUUID
        ├── wins: <int>
        ├── losses: <int>
        └── totalGames: <int>
```

---

## Best Practices

### Arena-Design
- ✅ Mehrere unterschiedliche Arenen erstellen
- ✅ Spawn-Punkte testen vor Speichern
- ✅ Icons für bessere Übersicht setzen
- ✅ Regelmäßig Arenen updaten/überarbeiten

### Kit-Management
- ✅ Verschiedene Play-Styles abdecken (Sword, Ranged, Tank, Speed)
- ✅ Kits balancieren (nicht zu overpowered)
- ✅ Icons setzen für Spieler-Übersicht
- ✅ Kits regelmäßig überprüfen und updaten

### Performance
- ✅ Arenen-Welten werden automatisch gelöscht nach Duels
- ✅ Kein speicher-Problem durch alte Welten
- ✅ Stats sind optimiert (per YAML)

---

## Commands Referenz

| Command | Beschreibung | Permission |
|---|---|---|
| `/duel arena create <name>` | Neue Arena erstellen | `epicduels.admin` |
| `/duel arena setspawn1` | Spawn 1 setzen | `epicduels.admin` |
| `/duel arena setspawn2` | Spawn 2 setzen | `epicduels.admin` |
| `/duel arena save` | Arena speichern | `epicduels.admin` |
| `/duel arena delete <name>` | Arena löschen | `epicduels.admin` |
| `/duel arena list` | Alle Arenen anzeigen | `epicduels.admin` |
| `/duel arena tp <name>` | Zu Arena teleportieren | `epicduels.admin` |
| `/duel arena seticon <name>` | Arena-Icon setzen | `epicduels.admin` |
| `/duel kit create <name>` | Kit erstellen | `epicduels.admin` |
| `/duel kit delete <name>` | Kit löschen | `epicduels.admin` |
| `/duel kit edit <name>` | Kit bearbeiten | `epicduels.admin` |
| `/duel kit preview <name>` | Kit anschauen | `epicduels.admin` |
| `/duel kit list` | Alle Kits anzeigen | `epicduels.admin` |
| `/duel kit seticon <name>` | Kit-Icon setzen | `epicduels.admin` |
| `/duel setlobby` | Lobby-Spawn setzen | `epicduels.admin` |

---

## Nächste Schritte

- 📖 **Berechtigungen:** [Permissions Guide](./06-Permissions.md)
- ⚙️ **Konfiguration:** [Configuration Guide](./07-Configuration.md)
- 🎮 **Spieler:** [Player Guide](./03-Player-Guide.md)
