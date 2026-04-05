# EpicDuels

A full-featured 1v1 Duels plugin for Paper 1.21.1 servers.

## Features

- **Arena System** — Create void-world arenas with a simple setup wizard. Each duel runs in its own isolated world copy that gets deleted after the match.
- **Kit System** — Save, edit, and preview kits with full armor, offhand, and inventory support.
- **Duel Challenges** — Challenge players through an interactive GUI (pick player, arena, kit) or via commands. Requests expire after 30 seconds with clickable Accept/Deny buttons.
- **Duel Lifecycle** — Async world copy, 5-second countdown with Title API, freeze during countdown, automatic winner detection on death or disconnect, 3-second victory screen, full cleanup.
- **Stats Tracking** — Per-player wins, losses, total games, and win rate stored in `stats.yml`.
- **GUI Menus** — Main menu, player selector, arena selector, kit selector, kit editor, kit preview — all with glass pane borders and click sounds.
- **World Protection** — Lobby is protected, arena templates allow building for admins, instance worlds block building during duels.
- **Void World Generator** — Custom `ChunkGenerator` for empty worlds with plains biome.

## Requirements

- Paper 1.21.1+
- Java 21

## Installation

1. Download `EpicDuels-1.0.0.jar` from the repository
2. Place it in your server's `plugins/` folder
3. Restart the server
4. (Optional) Add to `bukkit.yml` for true void lobby world:
   ```yaml
   worlds:
     world:
       generator: EpicDuels
   ```

## Commands

| Command | Description | Permission |
|---|---|---|
| `/duel` or `/duel menu` | Open main GUI menu | — |
| `/duel challenge <player>` | Challenge a player (opens GUI) | `epicduels.duel` |
| `/duel accept [player]` | Accept a duel request | `epicduels.duel` |
| `/duel deny [player]` | Deny a duel request | `epicduels.duel` |
| `/duel cancel` | Cancel your outgoing request | `epicduels.duel` |
| `/duel stats [player]` | View duel stats | `epicduels.stats` |
| `/duel arena create <name>` | Create a new arena | `epicduels.admin` |
| `/duel arena setspawn1` | Set spawn point 1 | `epicduels.admin` |
| `/duel arena setspawn2` | Set spawn point 2 | `epicduels.admin` |
| `/duel arena save` | Save arena and mark ready | `epicduels.admin` |
| `/duel arena delete <name>` | Delete an arena | `epicduels.admin` |
| `/duel arena list` | List all arenas | `epicduels.admin` |
| `/duel arena tp <name>` | Teleport to arena template | `epicduels.admin` |
| `/duel kit create <name>` | Save current inventory as kit | `epicduels.admin` |
| `/duel kit delete <name>` | Delete a kit | `epicduels.admin` |
| `/duel kit list` | List all kits | `epicduels.admin` |
| `/duel kit edit <name>` | Edit kit in chest GUI | `epicduels.admin` |
| `/duel kit preview <name>` | Preview kit (read-only) | `epicduels.admin` |
| `/duel setlobby` | Set lobby spawn point | `epicduels.admin` |
| `/duel setlobbyspawn1` | Set lobby queue position 1 | `epicduels.admin` |
| `/duel setlobbyspawn2` | Set lobby queue position 2 | `epicduels.admin` |

**Alias:** `/d` works as shorthand for `/duel`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `epicduels.admin` | All admin commands | OP |
| `epicduels.duel` | Challenge and accept duels | Everyone |
| `epicduels.stats` | View stats | Everyone |

## Quick Start Guide

1. **Set up lobby:** Stand where you want the lobby spawn and run `/duel setlobby`
2. **Create an arena:** `/duel arena create myarena` — you'll be teleported into a void world in creative mode
3. **Build your arena** and set spawn points: `/duel arena setspawn1` and `/duel arena setspawn2`
4. **Save the arena:** `/duel arena save` — teleports you back to lobby
5. **Create a kit:** Equip the gear you want, then `/duel kit create pvp`
6. **Duel!** `/duel challenge <player>` or open the menu with `/duel`

## Data Files

| File | Contents |
|---|---|
| `config.yml` | Lobby and queue spawn locations |
| `arenas.yml` | Arena definitions and spawn points |
| `kits.yml` | Kit inventories (Base64 encoded) |
| `stats.yml` | Player win/loss records |

## Building from Source

```bash
# Maven
mvn clean package

# Gradle
gradle build
```

Output JAR: `build/libs/EpicDuels-1.0.0.jar` (Gradle) or `target/EpicDuels.jar` (Maven)
