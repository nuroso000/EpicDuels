# <h1 align="center">EpicDuels</h1>
<p align="center">
  <img src="https://i.ibb.co/VYNPYwxK/Neues-Projekt.png" width="100" alt="Logo">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1+-green">
  <img src="https://img.shields.io/badge/Java-21-orange">
  <a href="https://discordapp.com/users/1245992673677017119">
    <img src="https://img.shields.io/badge/Discord-Profile-5865f2?style=flat&logo=discord&logoColor=white" alt="Discord Profile">
  </a>
</p>

A full-featured 1v1 Duels plugin for Paper 1.21.1 servers — with multi-server stats via Supabase or Firebase.

---

## Features

- **Arena System** — Create void-world arenas with a simple setup wizard. Each duel runs in its own isolated world copy that gets deleted after the match.
- **Kit System** — Save, edit, and preview kits with full armor, offhand, and inventory support. Set custom display icons per kit.
- **Duel Challenges** — Challenge players through an interactive GUI (pick player → kit → map) or via commands. Requests expire after 30 seconds with clickable Accept/Deny buttons.
- **Queue / Matchmaking** — Join a kit-based queue from the menu or via command. When two players queue for the same kit they are instantly matched and teleported to a random arena. Action bar shows live queue time.
- **Spectate** — Watch any active duel in Spectator mode with `/duel spectate <player>`. Automatically returned to lobby when the duel ends.
- **Multi-Server Stats** — Sync win/loss stats across multiple servers using **Supabase** (PostgreSQL) or **Firebase** Realtime Database. Local `stats.yml` always acts as a fallback cache.
- **GUI Menus** — Clean 3-icon main menu (Duels / Stats / Matchmaking) with separate sub-menus. All lists support pagination (28 items per page) for many kits and arenas.
- **Random Map Animation** — Selecting "Random Map" triggers a slot-machine-style animation cycling through arena icons before landing on the chosen map.
- **Duel Lifecycle** — Async world copy, 5-second countdown with Title API, freeze during countdown, automatic winner detection on death or disconnect, 3-second victory screen, full cleanup.
- **Block Protection** — Lobby locked for non-admins. Template worlds open for admins. Instance worlds allow player-placed blocks to be broken but protect original map blocks.
- **Custom Icons** — Set display icons for arenas and kits using items held in hand.
- **Void World Generator** — Custom `ChunkGenerator` for empty worlds with plains biome.

## Requirements

- Paper 1.21.1+
- Java 21

## Installation

1. Download `EpicDuels-0.3.0.jar` from the `release/` folder or build from source
2. Place it in your server's `plugins/` folder
3. Restart the server
4. (Optional) Add to `bukkit.yml` for true void lobby world:
   ```yaml
   worlds:
     world:
       generator: EpicDuels
   ```

## Commands

### Menu Navigation

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/duel` | `/d` | Open main menu (Duels / Stats / Matchmaking) | — |
| `/duel duels` | `/d duels` | Open Duels sub-menu (player selection) | `epicduels.duel` |
| `/duel stats` | `/d stats` | Open Stats sub-menu (your profile) | `epicduels.stats` |
| `/duel matchmaking` | `/d mm` | Open Matchmaking sub-menu (queue) | `epicduels.duel` |

### Player Commands

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/duel challenge <player>` | `/d c <player>` | Challenge a player (opens GUI flow) | `epicduels.duel` |
| `/duel accept [player]` | | Accept a duel request | `epicduels.duel` |
| `/duel deny [player]` | | Deny a duel request | `epicduels.duel` |
| `/duel cancel` | | Cancel your outgoing request | `epicduels.duel` |
| `/duel stats <player>` | | View another player's stats in chat | `epicduels.stats` |
| `/duel queue <kit>` | `/d q <kit>` | Join matchmaking queue for a kit | `epicduels.duel` |
| `/duel queue leave` | `/d q leave` | Leave the matchmaking queue | `epicduels.duel` |
| `/duel spectate <player>` | `/d spec <player>` | Spectate an active duel | `epicduels.duel` |

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

>[!TIP]
>`/d` works as shorthand for `/duel`

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

## Multi-Server Stats

Configure a remote stats backend in `config.yml`:

```yaml
stats:
  backend: "local"   # local | supabase | firebase
```

### Supabase
1. Run in SQL editor:
   ```sql
   CREATE TABLE IF NOT EXISTS player_stats (
     uuid   TEXT PRIMARY KEY,
     wins   INTEGER NOT NULL DEFAULT 0,
     losses INTEGER NOT NULL DEFAULT 0
   );
   ```
2. Set `backend: "supabase"` and fill in `url` + `api-key` (from Settings → API).

### Firebase
1. Enable Realtime Database in your Firebase project.
2. Set `backend: "firebase"` and fill in `database-url`. Optionally set `auth-token` (database secret) for authenticated writes.

Stats are pushed async on every win/loss and at shutdown. `stats.yml` stays as a local fallback cache.

## Data Files

| File | Contents |
|---|---|
| `config.yml` | Lobby spawn, PvP toggle, remote stats backend |
| `arenas.yml` | Arena definitions, spawn points, and icons |
| `kits.yml` | Kit inventories (Base64 encoded) and icons |
| `stats.yml` | Player win/loss records (local cache) |

## Building from Source

```bash
# Gradle (recommended)
gradle clean build

# Maven
mvn clean package
```

Output JAR: `build/libs/EpicDuels-0.3.0.jar` (Gradle) or `target/EpicDuels.jar` (Maven)

## License & Usage

This project is licensed under **CC BY-NC-SA 4.0**.

### Commercial Use Clarification
To avoid confusion within the Minecraft community:
* **Allowed:** You are permitted to use this plugin on any Minecraft server, including those that generate revenue (e.g., through ranks, donations, or webstores).
* **Prohibited:** You are NOT allowed to sell the plugin itself, sell modified versions of the plugin, or include it in paid software bundles/modpacks without explicit permission.
* **Open Source:** If you modify the code and redistribute it, you must keep the source code open and use this same license.
