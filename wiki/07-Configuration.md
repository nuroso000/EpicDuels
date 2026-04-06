# Configuration Guide

Configuring EpicDuels.

## File Structure

EpicDuels automatically creates these files in the `plugins/EpicDuels/` directory:

```
plugins/EpicDuels/
├── config.yml      # Lobby configuration
├── arenas.yml      # Arena data
├── kits.yml        # Kit data
└── stats.yml       # Player statistics (auto-created)
```

---

## config.yml

The main configuration file.

### Structure

```yaml
lobbySpawn:
  worldName: "world"
  x: 0.0
  y: 100.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0
```

### Setting It

**Automatically:** Use `/duel setlobby` - writes the current position automatically.

**Manually:** Edit the file and restart the server.

### Example

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

Stores all arena data.

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

### Example

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

### Fields

| Field | Description | Example |
|---|---|---|
| `spawn1` | Spawn 1 coordinates | `{x: 0, y: 100, z: 0}` |
| `spawn2` | Spawn 2 coordinates | `{x: 0, y: 100, z: 20}` |
| `icon` | Display icon | `GRASS_BLOCK` |
| `worldName` | World directory | `epicduels_pvp_arena` |
| `ready` | Is playable yes/no | `true` or `false` |

### Manual Editing

⚠️ **Caution:** Errors in this file can cause problems. Use commands instead!

```bash
/duel arena create myarena
/duel arena setspawn1
/duel arena setspawn2
/duel arena save
```

---

## kits.yml

Stores all kit data.

### Format

```yaml
kits:
  <kit-name>:
    inventory: <Base64-Encoded-Inventory>
    armor: <Base64-Encoded-Armor>
    icon: <ItemStack>
```

### Example

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

### Manual Editing

⚠️ **Not Recommended:** Use commands instead:

```bash
/duel kit create my_kit
/duel kit edit my_kit
/duel kit preview my_kit
/duel kit seticon my_kit
```

---

## stats.yml

Stores player statistics (auto-generated).

### Format

```yaml
stats:
  <player-uuid>:
    wins: <int>
    losses: <int>
    totalGames: <int>
```

### Example

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

### Automatic Updates

Stats automatically update when:
- ✅ A duel ends
- ✅ A player wins or loses
- ✅ A player wins a queue duel

### Win Rate Calculation

```
Win Rate = (Wins / Total Games) * 100
```

**Example:**
```
15 Wins / 23 Total = 0.652 * 100 = 65.2%
```

---

## Bukkit.yml Integration

### Void World Generator

To have a void lobby with the custom generator:

**bukkit.yml:**
```yaml
worlds:
  world:
    generator: EpicDuels
```

This makes the lobby a void world.

### After Editing

```bash
# Restart the server
/stop
# Start the server again
```

---

## File Permissions

```bash
# Linux/Mac: Set correct permissions
chmod 644 plugins/EpicDuels/*.yml

# Ensure server can read/write files
chmod 755 plugins/EpicDuels/
```

---

## Backup & Recovery

### Create Backup

```bash
# Backup all data
cp -r plugins/EpicDuels/ plugins/EpicDuels_backup/
```

### Restore

```bash
# Restore from backup
rm -r plugins/EpicDuels/
cp -r plugins/EpicDuels_backup/ plugins/EpicDuels/
```

---

## Common Issues

### Stats Not Being Saved

**Solution:**
- Check that `stats.yml` is writable
- Use `/duel stats` to check if values update
- Restart the server

### Arenas Don't Work After Editing

**Solution:**
- Make sure both spawn points are set
- Check that `ready: true` is set for the arena
- Use `/duel arena tp <name>` to verify the arena

### Config Errors After Manual Edit

**Solution:**
- Check YAML syntax (spaces, not tabs)
- Use a YAML validator: [yamllint.com](https://www.yamllint.com)
- Restart the server
- Check server logs

---

## Advanced Configuration

### Logs

Enable debug logs for troubleshooting (if available):

**bukkit.yml:**
```yaml
plugins:
  EpicDuels:
    debug: true
```

### Performance Tips

- ✅ Limit arena count (e.g., 10)
- ✅ Keep kit inventories simple
- ✅ Regularly clean up stats file
- ✅ Run server with sufficient RAM

---

## Next Steps

- 🎮 **Playing:** [Player Guide](./03-Player-Guide.md)
- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
- 🔐 **Permissions:** [Permissions Guide](./06-Permissions.md)
