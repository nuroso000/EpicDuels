# Configuration Guide

Konfiguration von EpicDuels.

## Dateistruktur

EpicDuels erstellt automatisch folgende Dateien im `plugins/EpicDuels/` Verzeichnis:

```
plugins/EpicDuels/
├── config.yml      # Lobby-Konfiguration
├── arenas.yml      # Arena-Daten
├── kits.yml        # Kit-Daten
└── stats.yml       # Spieler-Statistiken (autom. erzeugt)
```

---

## config.yml

Die Hauptkonfigurationsdatei.

### Struktur

```yaml
lobbySpawn:
  worldName: "world"
  x: 0.0
  y: 100.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0
```

### Einstellen

**Automatisch:** Nutze `/duel setlobby` - schreibt die aktuelle Position automatisch.

**Manuell:** Bearbeite die Datei und starte den Server neu.

### Beispiel

```yaml
lobbySpawn:
  worldName: "world"
  x: 500.5
  y: 80.0
  z: -200.5
  yaw: 45.0
  pitch: 0.0
```

---

## arenas.yml

Speichert alle Arena-Daten.

### Format

```yaml
arenas:
  <arena-name>:
    spawn1:
      x: 100
      y: 65
      z: 200
    spawn2:
      x: 100
      y: 65
      z: 220
    icon: <ItemStack>
    worldName: epicduels_<arena-name>
    ready: true
```

### Beispiel

```yaml
arenas:
  pvp_arena:
    spawn1:
      x: 0.5
      y: 100.0
      z: 0.5
    spawn2:
      x: 0.5
      y: 100.0
      z: 20.5
    icon: GRASS_BLOCK
    worldName: epicduels_pvp_arena
    ready: true
  
  sky_arena:
    spawn1:
      x: 50.5
      y: 150.0
      z: 50.5
    spawn2:
      x: 50.5
      y: 150.0
      z: 70.5
    icon: STONE
    worldName: epicduels_sky_arena
    ready: true
```

### Felder

| Feld | Beschreibung | Beispiel |
|---|---|---|
| `spawn1` | Koordinaten Spawn 1 | `{x: 0, y: 100, z: 0}` |
| `spawn2` | Koordinaten Spawn 2 | `{x: 0, y: 100, z: 20}` |
| `icon` | Display-Icon | `GRASS_BLOCK` |
| `worldName` | Welt-Verzeichnis | `epicduels_pvp_arena` |
| `ready` | Spielbar ja/nein | `true` oder `false` |

### Manuell bearbeiten

⚠️ **Vorsicht:** Fehler in dieser Datei können zu Problemen führen. Nutze stattdessen Befehle!

```bash
/duel arena create myarena
/duel arena setspawn1
/duel arena setspawn2
/duel arena save
```

---

## kits.yml

Speichert alle Kit-Daten.

### Format

```yaml
kits:
  <kit-name>:
    inventory: <Base64-Encoded-Inventory>
    armor: <Base64-Encoded-Armor>
    icon: <ItemStack>
```

### Beispiel

```yaml
kits:
  survival_pvp:
    inventory: "AAAAHgEAAfv+/v4BAAAAAA..."
    armor: "AAAAEAEAAf7+/v4BAAAA..."
    icon: DIAMOND_SWORD
  
  ranged:
    inventory: "AAAAHgEAAfv+/v4BAAAAAA..."
    armor: "AAAAEAEAAf7+/v4BAAAA..."
    icon: BOW
```

### Manuell bearbeiten

⚠️ **Nicht empfohlen:** Nutze stattdessen die Befehle:

```bash
/duel kit create my_kit
/duel kit edit my_kit
/duel kit preview my_kit
/duel kit seticon my_kit
```

---

## stats.yml

Speichert Spieler-Statistiken (automatisch generiert).

### Format

```yaml
stats:
  <player-uuid>:
    wins: <int>
    losses: <int>
    totalGames: <int>
```

### Beispiel

```yaml
stats:
  "550e8400-e29b-41d4-a716-446655440000":
    wins: 15
    losses: 8
    totalGames: 23
  
  "6ba7b810-9dad-11d1-80b4-00c04fd430c8":
    wins: 3
    losses: 2
    totalGames: 5
```

### Automatische Updates

Stats werden automatisch aktualisiert wenn:
- ✅ Ein Duel endet
- ✅ Ein Spieler gewinnt oder verliert
- ✅ Ein Spieler einen Queue-Duel gewinnt

### Win Rate Berechnung

```
Win Rate = (Wins / Total Games) * 100
```

**Beispiel:**
```
15 Wins / 23 Total = 0.652 * 100 = 65.2%
```

---

## Bukkit.yml Integration

### Void World Generator

Um einen Void-Lobby mit dem Custom-Generator zu haben:

**bukkit.yml:**
```yaml
worlds:
  world:
    generator: EpicDuels
```

Dies sorgt dafür, dass die Lobby als Void-Welt generiert wird.

### Nach Bearbeitung

```bash
# Server neustarten
/stop
# Starten Sie den Server erneut
```

---

## File Berechtigungen

```bash
# Linux/Mac: Richtige Berechtigungen setzen
chmod 644 plugins/EpicDuels/*.yml

# Sicherstellen, dass der Server die Dateien lesen/schreiben kann
chmod 755 plugins/EpicDuels/
```

---

## Backup & Recovery

### Backup erstellen

```bash
# Alle Daten sichern
cp -r plugins/EpicDuels/ plugins/EpicDuels_backup/
```

### Wiederherstellen

```bash
# Aus Backup zurückstellen
rm -r plugins/EpicDuels/
cp -r plugins/EpicDuels_backup/ plugins/EpicDuels/
```

---

## Häufige Probleme

### Stats werden nicht gespeichert

**Lösung:**
- Überprüfe, dass die Datei `stats.yml` beschreibbar ist
- Nutze `/duel stats` um zu überprüfen, ob der Wert aktualisiert wird
- Starte den Server neu

### Arenen funktionieren nicht nach Bearbeitung

**Lösung:**
- Stelle sicher, dass beide Spawn-Punkte gesetzt sind
- Überprüfe, dass `ready: true` in der Arena eingestellt ist
- Nutze `/duel arena tp <name>` um die Arena zu überprüfen

### Config-Fehler nach manueller Bearbeitung

**Lösung:**
- Überprüfe YAML-Syntax (Spaces, nicht Tabs)
- Nutze einen YAML-Validator: [yamllint.com](https://www.yamllint.com)
- Starte den Server neu
- Überprüfe die Server-Logs

---

## Erweiterte Konfiguration

### Logs

Aktiviere Debug-Logs für Fehlersuche (falls vorhanden):

**bukkit.yml:**
```yaml
plugins:
  EpicDuels:
    debug: true
```

### Performance-Tipps

- ✅ Begrenzte Anzahl Arenen (z.B. 10)
- ✅ Kit-Inventory nicht zu komplex
- ✅ Regelmäßig Stats-Datei aufräumen
- ✅ Server mit ausreichend RAM betreiben

---

## Nächste Schritte

- 🎮 **Spielen:** [Player Guide](./03-Player-Guide.md)
- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
- 🔐 **Berechtigungen:** [Permissions Guide](./06-Permissions.md)
