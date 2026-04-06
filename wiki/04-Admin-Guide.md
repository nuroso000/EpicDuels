# Admin Guide

Server administrator guide for managing EpicDuels.

## Requirements

- **OP Permissions** or `epicduels.admin` permission
- **Server Console Access**
- Minecraft **1.21.1+** and **Java 21+**

---

## Arena Management

### Create an Arena

```bash
/duel arena create myarena
```

The plugin will:
1. Create a new void world (`epicduels_myarena`)
2. Teleport you to Creative Mode
3. You're now in the arena template

### Building the Arena

In the arena template, you can build freely:
- ✅ Place & break blocks
- ✅ Use Creative Mode
- ✅ Shape the landscape however you want

**Arena Design Tips:**
- Create fair spawn points (not too close)
- Use heights and depths for tactical gameplay
- Avoid tight spaces for sword kits
- Wide spaces ideal for ranged kits

### Set Spawn Points

Stand at Position 1 (e.g., left side):
```bash
/duel arena setspawn1
```

Go to Position 2 (e.g., right side):
```bash
/duel arena setspawn2
```

**Important:**
- Both spawns should have **the same Y coordinate** if possible
- **At least 10 blocks apart**
- **No water or lava** at spawn positions

### Save the Arena

```bash
/duel arena save
```

The arena will:
1. Be saved to `arenas.yml`
2. Be marked as "ready" (playable)
3. Become the template (can be edited again)
4. Teleport you back to the lobby

### Set Arena Icon

The icon displays in the GUI:

```bash
# Hold an item in your hand (e.g., Grass Block, Cobblestone)
/duel arena seticon myarena
```

The item becomes the arena's icon.

### List All Arenas

```bash
/duel arena list
```

Shows all arenas with their status:
- ✅ **ready** — Playable
- ⏳ **building** — Still being built

### Go to Arena Template

```bash
/duel arena tp myarena
```

Teleports you to the template for editing.

### Delete an Arena

```bash
/duel arena delete myarena
```

**Warning:** Deletes the arena and all its duel data!

---

## Kit Management

### Create a Kit

1. Equip yourself with the desired gear
   - Armor
   - Weapon
   - Inventory items
   - Offhand item

2. Execute:
```bash
/duel kit create pvp_sword
```

The plugin saves your current inventory as a kit.

### Kit Templates

**Example Kits:**

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

### Edit a Kit

```bash
/duel kit edit pvp_sword
```

Opens the kit items in a chest GUI:
- Remove or add items
- Arrange items
- Close the chest

The kit is automatically updated.

### Preview a Kit (Read-Only)

```bash
/duel kit preview pvp_sword
```

Shows kit items in a chest (cannot be edited).

### Set Kit Icon

```bash
# Hold an item in your hand (e.g., Diamond Sword)
/duel kit seticon pvp_sword
```

The item becomes the kit's icon in the GUI.

### List All Kits

```bash
/duel kit list
```

Shows all available kits.

### Delete a Kit

```bash
/duel kit delete pvp_sword
```

**Warning:** Players can no longer use this kit!

---

## Lobby Setup

### Set Lobby Spawn

Teleport to the location where players should land:

```bash
/duel setlobby
```

This is where players land with `/duel` and see the main menu.

---

## File Structure

```
plugins/EpicDuels/
├── config.yml          # Lobby position
│   └── lobbySpawn:
│       location: "world,x,y,z,yaw,pitch"
│
├── arenas.yml          # All arenas
│   ├── Arena-Name
│   │   ├── spawn1: {x, y, z}
│   │   ├── spawn2: {x, y, z}
│   │   ├── icon: <Item>
│   │   └── worldName: epicduels_<name>
│
├── kits.yml            # All kits
│   ├── Kit-Name
│   │   ├── inventory: <Base64>
│   │   ├── armor: <Base64>
│   │   └── icon: <Item>
│
└── stats.yml           # Player statistics
    └── PlayerUUID
        ├── wins: <int>
        ├── losses: <int>
        └── totalGames: <int>
```

---

## Best Practices

### Arena Design
- ✅ Create multiple different arenas
- ✅ Test spawn points before saving
- ✅ Set icons for better overview
- ✅ Regularly update/revise arenas

### Kit Management
- ✅ Cover different play styles (Sword, Ranged, Tank, Speed)
- ✅ Balance kits (not overpowered)
- ✅ Set icons for player overview
- ✅ Regularly check and update kits

### Performance
- ✅ Arena worlds are automatically deleted after duels
- ✅ No storage problems from old worlds
- ✅ Stats are optimized (YAML)

---

## Commands Reference

| Command | Description | Permission |
|---|---|---|
| `/duel arena create <name>` | Create new arena | `epicduels.admin` |
| `/duel arena setspawn1` | Set spawn 1 | `epicduels.admin` |
| `/duel arena setspawn2` | Set spawn 2 | `epicduels.admin` |
| `/duel arena save` | Save arena | `epicduels.admin` |
| `/duel arena delete <name>` | Delete arena | `epicduels.admin` |
| `/duel arena list` | List all arenas | `epicduels.admin` |
| `/duel arena tp <name>` | Teleport to arena | `epicduels.admin` |
| `/duel arena seticon <name>` | Set arena icon | `epicduels.admin` |
| `/duel kit create <name>` | Create kit | `epicduels.admin` |
| `/duel kit delete <name>` | Delete kit | `epicduels.admin` |
| `/duel kit edit <name>` | Edit kit | `epicduels.admin` |
| `/duel kit preview <name>` | Preview kit | `epicduels.admin` |
| `/duel kit list` | List all kits | `epicduels.admin` |
| `/duel kit seticon <name>` | Set kit icon | `epicduels.admin` |
| `/duel setlobby` | Set lobby spawn | `epicduels.admin` |

---

## Next Steps

- 📖 **Permissions:** [Permissions Guide](./06-Permissions.md)
- ⚙️ **Configuration:** [Configuration Guide](./07-Configuration.md)
- 🎮 **Players:** [Player Guide](./03-Player-Guide.md)
