# EpicDuels

**A full-featured Duels plugin for Paper 1.21.1 — no external dependencies required.**

Challenge players 1v1, group up with friends for team duels or tournaments, track stats across multiple servers, and keep your lobby perfectly pristine — all out of the box.

---

## Features

### Party System (New in v2.0)

Create a party with `/party create`, invite friends, and launch a game with `/party start`. The party owner selects a kit once; arenas are chosen randomly per match.

**Team Duel (2v2 / 3v3 / 4v4)**
- Teams are split randomly at match start
- Teammates spawn together with small offsets — nobody overlaps
- Friendly fire is disabled automatically
- Eliminated players spectate the battle until their whole team falls
- Last team standing wins

**Single-Elimination Tournament**
- Full 1v1 bracket with automatic byes for non-power-of-2 counts
- Eliminated players are auto-routed to spectate a live match — they are never left waiting in the lobby
- When a spectated match ends, eliminated players are re-routed to the next live match
- The final winner is announced **inside the party only** — no server-wide spam

### 1v1 Duels

- **Challenge anyone** — Pick a player, kit, and map through the GUI. Opponent gets a clickable Accept / Deny message with a 30-second timer.
- **Queue / Matchmaking** — Join a kit-based queue from the menu or via `/duel queue <kit>`. Two players queuing for the same kit are instantly matched on a random arena. Live queue time on the action bar.
- **Spectate** — `/duel spectate <player>` puts you in Spectator mode inside the duel. Auto-returned to lobby when the match ends.
- **Isolated arenas** — Every duel runs in its own void world copy, deleted immediately after the match.
- **Full lifecycle** — Async world copy → 5-second countdown (Title API, player freeze) → fight → instant winner detection → 3-second victory screen → cleanup.

### Kits & Arenas

- **Kit editor** — Save current inventory as a kit (`/duel kit create <name>`). Edit via chest GUI. Full armor, offhand, and all 36 slots.
- **Arena builder** — `/duel arena create <name>` puts you in a void world in Creative. Build, set two spawns, then `/duel arena save`.
- **Rename, icons, list** — Both types support rename, custom icons (hold item in hand), and paginated lists.
- **Admin kit preview** — `/duel kit give <name>` copies any kit into your inventory for testing.
- **Block protection** — Player-placed blocks can be broken during a duel; original map blocks are protected. Lobby locked for non-admins.

### Leaderboards & Stats

- **Per-player stats** — Wins, losses, win rate, and a score (`wins² / (wins + losses)`) that rewards consistency.
- **In-chat leaderboards** — `/duel leaderboard wins` and `/duel leaderboard score` print the top 10 in chat.
- **In-world holograms** — Place a persistent floating leaderboard with `/duel leaderboard sethologram <wins|score>`. No hologram plugin needed. ArmorStands update in-place (no lag spikes). Refresh every 60 seconds.

### Multi-Server Stats

Sync win/loss data across servers — all async, zero tick impact:

| Backend | Setup |
|---|---|
| **Local** *(default)* | `stats.yml` only — no configuration needed |
| **Supabase** | PostgREST upsert to PostgreSQL |
| **Firebase** | Realtime Database REST API |

Local `stats.yml` always serves as a fallback cache.

### Lobby Protections

All lobby players are in **Adventure mode** — they cannot break or place blocks, and cannot interact with item frames or armor stands. Fifteen additional rules, each togglable in `config.yml`:

- Block break / place / interact
- Fall damage / void damage
- Item pickup / drop / inventory movement
- Hunger / fire spread / leaf decay / block burning
- Natural mob spawning / weather changes / death messages / PvP

**Per-admin bypass** — `/duel lobby off` switches *that admin* to Creative and suspends all protections for their session only. Every other player keeps full protection. `/duel lobby on` restores Adventure mode for them.

---

## Requirements

- **Paper 1.21.1+**
- **Java 21**

Does not work on Spigot or Vanilla.

---

## Installation

1. Drop `EpicDuels-2.0.0.jar` into your `plugins/` folder.
2. Restart the server.
3. Set lobby spawn: stand where you want it and run `/duel setlobby`.
4. Create an arena: `/duel arena create myarena` → build → `/duel arena setspawn1` → `/duel arena setspawn2` → `/duel arena save`.
5. Create a kit: equip gear → `/duel kit create pvp`.
6. Open the menu with `/duel` and start dueling.

**Optional — true void lobby world** (add to `bukkit.yml`):
```yaml
worlds:
  world:
    generator: EpicDuels
```

---

## Commands

### Player Commands

| Command | Alias | Description |
|---|---|---|
| `/duel` | `/d` | Open main menu |
| `/duel challenge <player>` | `/d c <player>` | Challenge a player via GUI |
| `/duel accept [player]` | | Accept a duel request |
| `/duel deny [player]` | | Deny a duel request |
| `/duel cancel` | | Cancel your outgoing request |
| `/duel queue <kit>` | `/d q <kit>` | Join matchmaking queue |
| `/duel queue leave` | `/d q leave` | Leave the queue |
| `/duel spectate <player>` | `/d spec <player>` | Spectate an active duel |
| `/duel stats [player]` | | View stats in chat |
| `/duel leaderboard wins` | `/d lb wins` | Top 10 by wins |
| `/duel leaderboard score` | `/d lb score` | Top 10 by score |

### Party Commands

| Command | Alias | Description |
|---|---|---|
| `/party create` | `/p create` | Create a party |
| `/party invite <player>` | `/p invite <player>` | Invite a player |
| `/party accept [player]` | `/p accept` | Accept an invite |
| `/party deny [player]` | `/p deny` | Deny an invite |
| `/party leave` | `/p leave` | Leave your party |
| `/party disband` | `/p disband` | Disband the party (owner only) |
| `/party list` | `/p list` | Show members and online status |
| `/party start` | `/p start` | Open mode-select GUI (owner only) |

### Admin Commands

| Command | Description |
|---|---|
| `/duel arena create/delete/rename/list/tp/save/setspawn1/setspawn2/seticon` | Arena management |
| `/duel kit create/delete/rename/list/edit/preview/give/seticon` | Kit management |
| `/duel setlobby` | Set lobby spawn |
| `/duel lobby off` | Creative + bypass all lobby protections (you only) |
| `/duel lobby on` | Restore Adventure + re-enable protections (you only) |
| `/duel leaderboard sethologram <wins\|score>` | Place leaderboard hologram |
| `/duel leaderboard removehologram <wins\|score>` | Remove leaderboard hologram |

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `epicduels.admin` | All admin commands | OP |
| `epicduels.duel` | Challenge, accept, queue, spectate | Everyone |
| `epicduels.stats` | View stats and leaderboards | Everyone |
| `epicduels.party` | All party commands | Everyone |

---

## Multi-Server Stats Setup

### Supabase

1. Run in SQL Editor:
   ```sql
   CREATE TABLE IF NOT EXISTS player_stats (
     uuid   TEXT PRIMARY KEY,
     wins   INTEGER NOT NULL DEFAULT 0,
     losses INTEGER NOT NULL DEFAULT 0
   );
   ```
2. In `config.yml`:
   ```yaml
   stats:
     backend: "supabase"
     supabase:
       url: "https://your-project.supabase.co"
       api-key: "your-anon-key"
   ```

### Firebase

1. Enable Realtime Database in your Firebase project.
2. In `config.yml`:
   ```yaml
   stats:
     backend: "firebase"
     firebase:
       database-url: "https://your-project-default-rtdb.firebaseio.com"
       auth-token: ""   # optional database secret
   ```

---

## Data Files

| File | Contents |
|---|---|
| `config.yml` | Lobby spawn, protections, PvP toggle, stats backend |
| `arenas.yml` | Arena definitions, spawn points, icons |
| `kits.yml` | Kit inventories (Base64) and icons |
| `stats.yml` | Player win/loss records (local cache) |
| `leaderboards.yml` | Hologram positions |

---

## Changelog

### v2.0.0 — Party System, Lobby Hardening & Stability
- **Party System** — Team Duel (2v2/3v3/4v4 with friendly fire off) and Single-Elimination Tournament with auto-spectate routing for eliminated players
- **Adventure mode** in lobby — players cannot break blocks or interact with item frames/armor stands
- **Per-admin lobby bypass** — `/duel lobby off/on` suspends protections for that admin only
- **Memory leaks fixed** — stored BukkitTask refs, startup leftover-world cleanup, ghost queue entries removed
- **Hologram rewrite** — in-place ArmorStand updates, 60s refresh, better line spacing, player name cache
- **Supabase** — accept HTTP 204, startup connection test, RLS hint on 401/403, await shutdown flush

### v1.0.0 — Leaderboards & Holograms
- Top 10 leaderboards in chat + persistent in-world holograms (no external plugin)
- Score formula: `wins² / (wins + losses)`
- Instant respawn fix, duel/queue double-start fix

### v0.3.0 — Multi-Server Stats
- Supabase (PostgreSQL) and Firebase Realtime Database backends
- Async sync with local YAML fallback

### v0.2.x — Queue, GUI, Spectate & Polish
- Kit-based matchmaking queue with action bar timer
- 3-icon main menu with sub-menus and pagination
- Spectate, configurable lobby PvP, random map slot-machine animation
- Async world copy, per-duel block protection

---

*Licensed under CC BY-NC-SA 4.0 — free to use on any server, including revenue-generating ones. Not for resale.*
