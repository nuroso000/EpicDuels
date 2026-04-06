# Installation & Setup

In diesem Guide lernst du, EpicDuels zu installieren und einzurichten.

## Installation

### Voraussetzungen
- **Minecraft Server:** Paper 1.21.1 oder höher
- **Java:** Version 21 oder höher

### Schritt 1: Plugin herunterladen

Lade die aktuellste JAR-Datei herunter:
- 📥 **[Release Seite](https://github.com/nuroso000/epicduels/releases)** — Offizielle Releases
- 🎮 **[Modrinth](https://modrinth.com/plugin/epicduels)** — Alternative Quelle

### Schritt 2: Plugin installieren

1. Lade `EpicDuels-x.x.x.jar` herunter
2. Kopiere die Datei in den `plugins/` Ordner deines Servers
3. Starte deinen Server neu (oder nutze ein Plugin-Reload-Tool)

```
server/
├── plugins/
│   └── EpicDuels-0.2.1.jar  ← Hier platzieren
├── bukkit.yml
└── server.properties
```

### Schritt 3: Starten & Erste Konfiguration

Nach dem Start erzeugt EpicDuels automatisch diese Dateien:

```
plugins/EpicDuels/
├── config.yml      # Lobby-Spawn-Position
├── arenas.yml      # Arena-Daten
├── kits.yml        # Kit-Vorlagen
└── stats.yml       # Spieler-Statistiken
```

---

## Erste Einrichtung (5 Minuten)

### 1️⃣ Lobby setzen

```bash
/duel setlobby
```

Teleportiere dich an den Ort, wo Spieler beim `/duel` Command landen sollen, und führe den Befehl aus.

### 2️⃣ Erste Arena erstellen

```bash
/duel arena create pvp_1
```

Du wirst in eine leere Void-World teleportiert, wo du bauen kannst.

### 3️⃣ Arena bauen

- Baue deine Arena (z.B. eine Plattform, Landschaft, etc.)
- Nutze Creative Mode zum schnellen Bauen

### 4️⃣ Spawn-Punkte setzen

Stehe an der Stelle, wo Spieler 1 spawnen soll:
```bash
/duel arena setspawn1
```

Gehe zu Spieler 2s Spawn-Punkt:
```bash
/duel arena setspawn2
```

### 5️⃣ Arena speichern

```bash
/duel arena save
```

Die Arena wird gespeichert und ist sofort einsatzbereit!

### 6️⃣ Kit erstellen

1. Rüste dich mit gewünschter Ausrüstung aus (Armor, Sword, etc.)
2. Führe aus:
```bash
/duel kit create survival_pvp
```

### 7️⃣ Spielen!

```bash
/duel
```

Öffnet das Hauptmenü mit drei Optionen:
- **Challenge** — Fordere einen Spieler heraus
- **Stats** — Sehe deine Statistiken
- **Queue** — Trete einer Matchmaking-Queue bei

---

## Optionale Konfigurationen

### Void-World Generator

Um einen echten Void-Lobby zu haben, füge zu `bukkit.yml` hinzu:

```yaml
worlds:
  world:
    generator: EpicDuels
```

Dann starte den Server neu.

### Icons setzen

**Arena-Icon:**
```bash
# Halte ein Item in der Hand (z.B. Grass Block)
/duel arena seticon pvp_1
```

**Kit-Icon:**
```bash
# Halte ein Item in der Hand (z.B. Diamond Sword)
/duel kit seticon survival_pvp
```

---

## Nächste Schritte

✅ Plugin installiert und konfiguriert?

- 📖 **Mehr erfahren:** [Admin Guide](./04-Admin-Guide.md)
- 🎮 **Spielen:** [Player Guide](./03-Player-Guide.md)
- ⚙️ **Erweiterte Konfiguration:** [Configuration](./07-Configuration.md)

---

## Fehlerbehebung

### Plugin startet nicht
- Überprüfe die **Java-Version** (muss 21+ sein)
- Überprüfe die **Minecraft-Version** (muss 1.21.1+ sein)
- Schau in die **Server-Logs** für Fehlermeldungen

### Arenen werden nicht erstellt
- Stelle sicher, dass du **Op-Berechtigungen** hast
- Überprüfe die Berechtigungen: `epicduels.admin`

### Spieler können nicht duellieren
- Überprüfe, dass **mindestens eine Arena** existiert
- Überprüfe, dass **mindestens ein Kit** existiert
- Überprüfe die Berechtigungen: `epicduels.duel`

👉 **Mehr Hilfe:** [FAQ](./09-FAQ.md)
