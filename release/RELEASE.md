# EpicDuels v0.2.0

**Release Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

---

## Changelog

### New Features

- **Redesigned Main Menu GUI** — The `/duel` menu now has three visual sections:
  - **Left:** Challenge a Player — opens player selection, then kit selection, then map selection
  - **Middle:** Stats — displays your wins, losses, win rate, and total duels directly in the GUI with your player head
  - **Right:** Queue / Matchmaking — join a kit-based queue to auto-match against opponents

- **Queue / Matchmaking System** — New `QueueManager` enables automatic matchmaking:
  - Join a queue for any kit from the main menu or via `/duel queue <kit>`
  - Action bar shows real-time queue status with elapsed time
  - When two players queue for the same kit, they are automatically matched and teleported to a random arena
  - Players are removed from queue on disconnect

- **Random Map Animation** — When selecting "Random Map" in the map selection GUI:
  - A slot rapidly cycles through all arena icons, gradually slowing down
  - Ends with the chosen arena and a level-up sound effect
  - Proceeds to send the challenge after the animation

- **Arena & Kit Icons** — Admins can now set custom display icons:
  - `/duel arena seticon <name>` — hold any item, it becomes the arena's icon in GUIs
  - `/duel kit seticon <name>` — hold any item, it becomes the kit's icon in GUIs
  - Icons are persisted in `arenas.yml` and `kits.yml`
  - Default icons: arenas use GRASS_BLOCK, kits use CHEST

- **Architecture Documentation** — Added a comprehensive comment block in the main plugin class explaining how all managers interact

### Bug Fixes

- **Block Placement & Breaking in Arena Instances** — Players can now place AND break blocks during active duels inside instance worlds. However, original arena template blocks (the map itself) cannot be broken. When an instance world is created, all existing block positions are recorded; only player-placed blocks can be broken.

- **Arena Instance Deletion** — Instance worlds are now properly unloaded with `Bukkit.unloadWorld(world, false)` and recursively deleted from disk using `Files.walkFileTree()`. Added error handling and warning logs if deletion fails.

- **Player Disconnect Mid-Duel** — When a player disconnects during a duel, the duel ends immediately, the remaining player is declared winner and teleported to lobby, stats are updated, and the instance world is cleaned up.

### Changes

- **Removed Lobby Spawn Commands** — Removed `/duel setlobbyspawn1` and `/duel setlobbyspawn2`. Only `/duel setlobby` remains. All players teleport to the single lobby spawn on join and after duels.

- **Challenge Flow Reordered** — The challenge flow is now: Select Player -> Select Kit -> Select Map (previously it was Player -> Map -> Kit). This feels more natural.

- **Improved GUIs** — All GUIs now use the correct display icon for arenas and kits instead of hardcoded materials.

### Commands Added

| Command | Description | Permission |
|---|---|---|
| `/duel queue <kit>` | Join matchmaking queue for a kit | `epicduels.duel` |
| `/duel queue leave` | Leave the matchmaking queue | `epicduels.duel` |
| `/duel arena seticon <name>` | Set arena display icon (hold item) | `epicduels.admin` |
| `/duel kit seticon <name>` | Set kit display icon (hold item) | `epicduels.admin` |

### Commands Removed

| Command | Reason |
|---|---|
| `/duel setlobbyspawn1` | Replaced by single `/duel setlobby` |
| `/duel setlobbyspawn2` | Replaced by single `/duel setlobby` |

---

## Installation

1. Drop `EpicDuels-0.2.0.jar` into your server's `plugins/` folder
2. Restart the server
3. Set lobby: `/duel setlobby`
4. Create arenas and kits, then duel!
