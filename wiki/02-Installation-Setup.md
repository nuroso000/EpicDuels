# Installation & Setup

In this guide, you'll learn how to install EpicDuels and get it set up.

## Installation

### Requirements
- **Minecraft Server:** Paper 1.21.1 or higher
- **Java:** Version 21 or higher

### Step 1: Download the Plugin

Download the latest JAR file:
- 📥 **[Releases Page](https://github.com/nuroso000/epicduels/releases)** — Official releases
- 🎮 **[Modrinth](https://modrinth.com/plugin/epicduels)** — Alternative source

### Step 2: Install the Plugin

1. Download `EpicDuels-x.x.x.jar`
2. Copy the file to your server's `plugins/` folder
3. Restart your server (or use a plugin reload tool)

```
server/
├── plugins/
│   └── EpicDuels-0.2.1.jar  ← Place here
├── bukkit.yml
└── server.properties
```

### Step 3: Start & Initial Configuration

After startup, EpicDuels will automatically create these files:

```
plugins/EpicDuels/
├── config.yml      # Lobby spawn location
├── arenas.yml      # Arena data
├── kits.yml        # Kit templates
└── stats.yml       # Player statistics
```

---

## Quick Setup (5 minutes)

### 1️⃣ Set Lobby Spawn

```bash
/duel setlobby
```

Teleport to the location where players should land when running `/duel`, then execute the command.

### 2️⃣ Create Your First Arena

```bash
/duel arena create pvp_1
```

You'll be teleported to an empty void world where you can build.

### 3️⃣ Build Your Arena

- Build your arena (e.g., a platform, landscape, etc.)
- Use Creative Mode for faster building

### 4️⃣ Set Spawn Points

Stand at the location where Player 1 should spawn:
```bash
/duel arena setspawn1
```

Go to Player 2's spawn location:
```bash
/duel arena setspawn2
```

### 5️⃣ Save the Arena

```bash
/duel arena save
```

The arena is saved and immediately ready for use!

### 6️⃣ Create a Kit

1. Equip yourself with the desired gear
   - Armor
   - Weapon
   - Inventory items
   - Offhand item

2. Execute:
```bash
/duel kit create survival_pvp
```

### 7️⃣ Play!

```bash
/duel
```

Opens the main menu with three options:
- **Challenge** — Challenge a player
- **Stats** — View your statistics
- **Queue** — Join a matchmaking queue

---

## Optional Configurations

### Void World Generator

To have a true void lobby, add to `bukkit.yml`:

```yaml
worlds:
  world:
    generator: EpicDuels
```

Then restart the server.

### Set Icons

**Arena Icon:**
```bash
# Hold an item in your hand (e.g., Grass Block)
/duel arena seticon pvp_1
```

**Kit Icon:**
```bash
# Hold an item in your hand (e.g., Diamond Sword)
/duel kit seticon survival_pvp
```

---

## Next Steps

✅ Plugin installed and configured?

- 📖 **Learn More:** [Admin Guide](./04-Admin-Guide.md)
- 🎮 **Start Playing:** [Player Guide](./03-Player-Guide.md)
- ⚙️ **Advanced Configuration:** [Configuration](./07-Configuration.md)

---

## Troubleshooting

### Plugin won't start
- Check your **Java version** (must be 21+)
- Check your **Minecraft version** (must be 1.21.1+)
- Look at **server logs** for error messages

### Arenas can't be created
- Make sure you have **OP permissions**
- Check the permission: `epicduels.admin`

### Players can't duel
- Verify that **at least one arena** exists
- Verify that **at least one kit** exists
- Check the permission: `epicduels.duel`

👉 **More Help:** [FAQ](./09-FAQ.md)
