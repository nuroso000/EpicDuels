# EpicDuels

A full-featured 1v1 Duels plugin for Paper 1.21.1 servers.

## Features

- **Arena System** — Create void-world arenas with a simple setup wizard. Each duel runs in its own isolated world copy that gets deleted after the match.
- **Kit System** — Save, edit, and preview kits with full armor, offhand, and inventory support. Set custom display icons per kit.
- **Duel Challenges** — Challenge players through an interactive GUI (pick player, kit, map) or via commands. Requests expire after 30 seconds with clickable Accept/Deny buttons.
- **Queue / Matchmaking** — Join a kit-based queue from the main menu or via command. When two players queue for the same kit, they are automatically matched and teleported to a random arena. Action bar shows real-time queue status.
- **Random Map Animation** — Selecting "Random Map" triggers a slot-machine-style animation cycling through arena icons before landing on the chosen map.
- **Duel Lifecycle** — Async world copy, 5-second countdown with Title API, freeze during countdown, automatic winner detection on death or disconnect, 3-second victory screen, full cleanup.
- **Stats Tracking** — Per-player wins, losses, total games, and win rate stored in `stats.yml`. Stats are shown directly in the main menu GUI.
- **GUI Menus** — Redesigned main menu with three sections (Challenge, Stats, Queue), plus player selector, kit selector, map selector, kit editor, kit preview, and list views.
- **Block Protection** — Lobby is protected for non-admins. Arena templates allow admin building. Instance worlds allow players to place blocks during duels; original map blocks cannot be broken, but player-placed blocks can.
- **Custom Icons** — Admins can set display icons for arenas and kits using items held in hand.
- **Void World Generator** — Custom `ChunkGenerator` for empty worlds with plains biome.

## Requirements

- Paper 1.21.1+
- Java 21

## Installation

1. Download `EpicDuels-0.2.1.jar` from the `release/` folder or build from source
2. Place it in your server's `plugins/` folder
3. Restart the server
4. (Optional) Add to `bukkit.yml` for true void lobby world:
   ```yaml
   worlds:
     world:
       generator: EpicDuels
   ```

## Commands

### Player Commands

| Command | Description | Permission |
|---|---|---|
| `/duel` or `/duel menu` | Open main GUI menu | — |
| `/duel challenge <player>` | Challenge a player (opens GUI flow) | `epicduels.duel` |
| `/duel accept [player]` | Accept a duel request | `epicduels.duel` |
| `/duel deny [player]` | Deny a duel request | `epicduels.duel` |
| `/duel cancel` | Cancel your outgoing request | `epicduels.duel` |
| `/duel stats [player]` | View duel stats | `epicduels.stats` |
| `/duel queue <kit>` | Join matchmaking queue for a kit | `epicduels.duel` |
| `/duel queue leave` | Leave the matchmaking queue | `epicduels.duel` |

### Admin Commands

| Command | Description | Permission |
|---|---|---|
| `/duel arena create <name>` | Create a new arena (void world) | `epicduels.admin` |
| `/duel arena setspawn1` | Set spawn point 1 (stand in arena world) | `epicduels.admin` |
| `/duel arena setspawn2` | Set spawn point 2 (stand in arena world) | `epicduels.admin` |
| `/duel arena save` | Save arena and mark as ready | `epicduels.admin` |
| `/duel arena delete <name>` | Delete an arena | `epicduels.admin` |
| `/duel arena list` | List all arenas with status | `epicduels.admin` |
| `/duel arena tp <name>` | Teleport to arena template world | `epicduels.admin` |
| `/duel arena seticon <name>` | Set arena icon (hold item in hand) | `epicduels.admin` |
| `/duel kit create <name>` | Save current inventory as a kit | `epicduels.admin` |
| `/duel kit delete <name>` | Delete a kit | `epicduels.admin` |
| `/duel kit list` | List all kits | `epicduels.admin` |
| `/duel kit edit <name>` | Edit kit in chest GUI | `epicduels.admin` |
| `/duel kit preview <name>` | Preview kit contents (read-only) | `epicduels.admin` |
| `/duel kit seticon <name>` | Set kit icon (hold item in hand) | `epicduels.admin` |
| `/duel setlobby` | Set lobby spawn point | `epicduels.admin` |

**Alias:** `/d` works as shorthand for `/duel`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `epicduels.admin` | All admin commands (arena, kit, setlobby, seticon) | OP |
| `epicduels.duel` | Challenge players, accept/deny duels, join queue | Everyone |
| `epicduels.stats` | View stats | Everyone |

## Quick Start Guide

1. **Set up lobby:** Stand where you want the lobby spawn and run `/duel setlobby`
2. **Create an arena:** `/duel arena create myarena` — you'll be teleported into a void world in creative mode
3. **Build your arena** and set spawn points: `/duel arena setspawn1` and `/duel arena setspawn2`
4. **Save the arena:** `/duel arena save` — teleports you back to lobby
5. **Create a kit:** Equip the gear you want, then `/duel kit create pvp`
6. **Set icons (optional):** Hold an item and run `/duel arena seticon myarena` or `/duel kit seticon pvp`
7. **Duel!** Open the menu with `/duel` — challenge a player, check your stats, or join the matchmaking queue

## Data Files

| File | Contents |
|---|---|
| `config.yml` | Lobby spawn location |
| `arenas.yml` | Arena definitions, spawn points, and icons |
| `kits.yml` | Kit inventories (Base64 encoded) and icons |
| `stats.yml` | Player win/loss records |

## Building from Source

```bash
# Gradle (recommended)
gradle clean build

# Maven
mvn clean package
```

Output JAR: `build/libs/EpicDuels-0.2.1.jar` (Gradle) or `target/EpicDuels.jar` (Maven)

## ⚖️ License & Usage

This project is licensed under **CC BY-NC-SA 4.0**.

### Commercial Use Clarification
To avoid confusion within the Minecraft community:
* **Allowed:** You are permitted to use this plugin on any Minecraft server, including those that generate revenue (e.g., through ranks, donations, or webstores).
* **Prohibited:** You are NOT allowed to sell the plugin itself, sell modified versions of the plugin, or include it in paid software bundles/modpacks without explicit permission.
* **Open Source:** If you modify the code and redistribute it, you must keep the source code open and use this same license.
