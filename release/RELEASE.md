# EpicDuels — Release Notes

---

## v2.0.0 — Party System, Lobby Hardening & Stability

**Date:** May 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### Major Feature — Party System

EpicDuels now supports **group play** through a full party system. Create a
party with `/party create`, invite friends with `/party invite <player>`, and
launch a game with `/party start`. The party owner selects a kit once at the
start; the arena is picked randomly per match. Supports 2–8 players.

#### Mode 1 — Team Duel (2v2 / 3v3 / 4v4)

- Teams are shuffled randomly at match start.
- All teammates spawn at the same arena spawn point with small circular offsets
  so nobody overlaps.
- **Friendly fire is disabled** — team members cannot damage each other.
- Eliminated players automatically spectate the ongoing battle until their team
  is wiped.
- The last team with a surviving member wins.

#### Mode 2 — Single-Elimination Tournament

- All party members compete in **1v1 bracket matches** using the existing duel
  system.
- Byes are inserted automatically for party sizes that are not a power of 2.
- Eliminated players are **auto-routed to spectate** a random live tournament
  match so nobody is left waiting in the lobby. When their spectated match ends
  they are re-routed to another live match if one is still running.
- The final winner is announced **only to party members** — no server-wide
  broadcast.

#### Party Commands

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/party create` | `/p create` | Create a party | `epicduels.party` |
| `/party invite <player>` | `/p invite <player>` | Invite a player | `epicduels.party` |
| `/party accept [player]` | `/p accept` | Accept an invite | `epicduels.party` |
| `/party deny [player]` | `/p deny` | Deny an invite | `epicduels.party` |
| `/party leave` | `/p leave` | Leave your party | `epicduels.party` |
| `/party disband` | `/p disband` | Disband (owner only) | `epicduels.party` |
| `/party list` | `/p list` | Show members | `epicduels.party` |
| `/party start` | `/p start` | Open mode-select GUI (owner only) | `epicduels.party` |

#### New Permission

| Permission | Description | Default |
|---|---|---|
| `epicduels.party` | Use all party commands | Everyone |

#### New Files

| File | Purpose |
|---|---|
| `Party.java` / `PartyInvite.java` / `PartyMode.java` / `TeamSize.java` | Party model layer |
| `TeamDuelInstance.java` / `Tournament.java` / `BattleInstance.java` | Match model layer |
| `PartyManager.java` | Invite, party lifecycle, member management |
| `TeamDuelManager.java` | Team composition, arena spawn offsets, friendly fire, winner detection |
| `TournamentManager.java` | Bracket generation, match scheduling, spectator routing |
| `FriendlyFireListener.java` | Cancels damage between teammates during team duels |
| `PartyCommand.java` / `PartyTabCompleter.java` | Command handling |

---

### Major Change — Adventure Mode in Lobby

All players now join and stay in **Adventure mode** inside the lobby world. This
prevents them from breaking or placing blocks — which is especially important for
servers running external plugins like **ImageFrame** whose maps/images would
otherwise be destroyed by any player in Survival/Creative.

- Players are set to `ADVENTURE` on join and on return from a duel.
- Spectators are also returned to `ADVENTURE` when they stop spectating.
- Admins with `epicduels.admin` are unaffected — see "Per-Admin Bypass" below.

### Major Feature — Item Frame & Armor Stand Protection

A new `PlayerInteractEntityEvent` handler prevents non-admin players from
interacting with item frames and armor stands in the lobby, closing the last
bypass that Adventure mode alone did not cover (right-clicking an item frame
still works in Adventure mode without a tool).

### Major Feature — Per-Admin Lobby Bypass

Admins can now switch their own lobby protections off temporarily for building
without touching the global config or affecting other players.

| Command | Description |
|---|---|
| `/duel lobby off` | Puts *you* in Creative mode and suspends all lobby protections for your session |
| `/duel lobby on` | Restores *your* Adventure mode and re-enables all lobby protections for you |

The bypass is **per-player** — every other player keeps full protections while an
admin is in build mode.

### Bug Fixes

#### Memory Leaks

- **BukkitTask references** — `DuelManager`'s request-expiration repeating task
  and `QueueManager`'s matchmaking loop both lacked stored `BukkitTask`
  references. On plugin reload, these tasks kept running in the background,
  slowly eating RAM. Both now store their `BukkitTask` and cancel it on
  `cleanupAll()` / `cleanup()`.

- **Leftover instance worlds** — Arena instance worlds (`arena_instance_*`) left
  behind by a previous crash or hard shutdown were never cleaned up. The plugin
  now scans for and deletes them on the first server tick after startup.

- **Ghost queue entries** — Players who disconnected while in the matchmaking
  queue were previously skipped on the action-bar update but kept in the queue
  maps indefinitely. They are now removed immediately when the next tick detects
  they are offline.

#### Supabase Sync

- The plugin now runs a **connection test on startup** so misconfiguration
  (wrong URL or key) is caught immediately rather than silently failing on the
  first win.
- PostgREST returns **HTTP 204 No Content** on a successful upsert; this was
  previously treated as an error and the push was silently dropped. Now accepted.
- **HTTP 401 / 403** responses print an actionable hint: `"Check your Row-Level
  Security policy in the Supabase dashboard"`.
- `StatsManager.pushAllToRemote()` during `onDisable` now properly awaits all
  async futures (up to 15 s) so no stats are lost on shutdown.

### Performance Improvements — Hologram Efficiency Rewrite

The `HologramManager` was re-written to eliminate the biggest source of entity
churn on leaderboard servers:

| Metric | Before | After |
|---|---|---|
| Update strategy | Delete all ArmorStands, re-spawn | Update text in-place |
| Refresh interval | 200 ticks (10 s) | 1 200 ticks (60 s) |
| Orphan scan scope | All loaded worlds | Hologram worlds only |
| Player name lookups | `Bukkit.getOfflinePlayer()` per line every tick | Cached per refresh cycle |
| Line spacing | 0.28 blocks (causes overlap at small fonts) | 0.30 blocks |

The old approach spawned and killed 20 ArmorStand entities every 10 seconds per
hologram. On a server with two holograms this meant 240 entity-create/destroy
operations per minute — causing noticeable lag spikes. The new approach touches
only existing entities' metadata.

### Upgrade Notes

- Replace `EpicDuels-1.0.0.jar` with `EpicDuels-2.0.0.jar`. All existing
  config, arenas, kits, stats, and `.yml` files remain fully compatible.
- No config changes are required — new features work out of the box.
- The new `epicduels.party` permission defaults to `true` (all players). If you
  want to restrict party creation, add it to your permissions plugin.
- Admins who had custom lobby protections toggled off in `config.yml` should
  review whether the per-player bypass (`/duel lobby off`) is a better fit.

---

## v1.0.0 — Full Release: Leaderboards, Holograms & Polish

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

This is the first **stable release** of EpicDuels. It brings a full leaderboard
system with in-world holograms, two important stability fixes, and a long list
of polished features carried over from the 0.2.x / 0.3.x series.

### Major Feature — Leaderboards

EpicDuels now ships with a complete leaderboard system:

- **`/duel leaderboard wins`** — shows the top 10 players by total wins in chat
- **`/duel leaderboard score`** — shows the top 10 players by **score** in chat
- Both read from the same `StatsManager` that powers the Stats GUI, so local +
  Supabase/Firebase-synced data is handled transparently.
- Aliases: `/duel lb`, `/duel top`

#### Score Formula

The score rewards both activity *and* consistency:

```
score = wins × winrate = wins² / (wins + losses)
```

Example: a player with **21 wins / 9 losses** → winrate 70%, score ≈ 14.7 ≈ 15.
Pure grinders with a low winrate score lower than players with fewer but more
decisive wins.

### Major Feature — Leaderboard Holograms

Admins can place persistent in-world leaderboard holograms without installing
any external plugin:

- `/duel leaderboard sethologram wins` — places the Wins hologram at your head
- `/duel leaderboard sethologram score` — places the Score hologram at your head
- `/duel leaderboard removehologram <wins|score>` — deletes a hologram

Under the hood:
- Each hologram is a stack of invisible, marker, small ArmorStands tagged via
  the Persistent Data Container so the plugin can clean them up even after a
  crash or reload.
- Positions are persisted in `leaderboards.yml`.
- Holograms automatically refresh every **10 seconds** from the StatsManager,
  so data stays fresh whether you're running local or remote stats.
- Top 3 ranks are gold/gray/red, lines 4–10 are white. Each line shows
  `#rank name — value`.

### Bug Fixes

- **Duel accept no longer keeps you in the queue** — When a player accepted a
  duel request while sitting in matchmaking, they stayed queued and could get
  matched into a second duel. `startDuel()` now proactively removes both
  players from the queue, cancels any pending outgoing/incoming challenge
  requests, and starts from a clean slate. Works for both challenge-based and
  queue-based duels.

- **Instant respawn in duels** — The death screen's "Respawn" button is
  sometimes unresponsive on modded clients or when the server is briefly busy,
  which left players stuck in the void. `PlayerDeathEvent` for duel
  participants now schedules `player.spigot().respawn()` on the next tick, so
  the losing player is immediately teleported back to the lobby.

### Carried Forward from 0.3.x / 0.2.x

All of the features from the 0.2.x and 0.3.x line are part of v1.0.0:

- **Multi-server stats** — Supabase / Firebase REST-based sync, local YAML
  cache as fallback (0.3.0).
- **Direct menu commands** — `/duel duels`, `/duel matchmaking`, `/duel stats`
  each open their sub-menu directly.
- **Kit & Arena rename** — `/duel kit rename <old> <new>` and
  `/duel arena rename <old> <new>` (also renames the template world on disk).
- **/duel kit give** — admin command to copy a saved kit into the admin's
  inventory for testing.
- **Lobby-only protections** — 15 toggleable rules under `lobby.protections.*`
  (block break/place/interact, fall damage, void death, weather, item
  pickup/drop, hunger, mob spawning, fire spread, block burning, leaf decay,
  death messages, inventory movement).
- **Spectator mode** — `/duel spectate <player>`.
- **Pagination** — 28 items per page across all list GUIs.
- **GUI redesign** — 27-slot main menu with Duels / Stats / Matchmaking
  sub-menus.
- **Queue / Matchmaking** — Auto-match on same kit with action-bar time.
- **Random map animation** — Slot-machine animation on random arena pick.
- **Async world copy** — Every duel runs in its own disposable world.
- **Block protection** — Per-duel original-block tracking.

### Commands Added in v1.0.0

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/duel leaderboard wins` | `/d lb wins` | Top 10 by wins | `epicduels.stats` |
| `/duel leaderboard score` | `/d lb score` | Top 10 by score | `epicduels.stats` |
| `/duel leaderboard sethologram <wins\|score>` | | Create/move hologram at your location | `epicduels.admin` |
| `/duel leaderboard removehologram <wins\|score>` | | Remove a leaderboard hologram | `epicduels.admin` |

### Data Files Added

| File | Contents |
|---|---|
| `leaderboards.yml` | Persistent hologram positions (per type) |

### Upgrade Notes

- Simply replace `EpicDuels-0.3.0.jar` with `EpicDuels-1.0.0.jar`. All existing
  config, arenas, kits, stats and `.yml` files remain compatible.
- Holograms are not created automatically — an admin must run
  `/duel leaderboard sethologram wins` and `/duel leaderboard sethologram score`
  once.

---

## v0.3.0 — Multi-Server Stats (Supabase & Firebase)

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### Major Feature: Remote Stats Backends

EpicDuels can now sync player statistics across multiple servers using **Supabase** (PostgreSQL) or **Firebase** Realtime Database. Configure the backend in `config.yml`:

```yaml
stats:
  backend: "supabase"   # or "firebase" or "local" (default)
```

#### Supabase Setup

1. Create a `player_stats` table in your Supabase SQL editor:
   ```sql
   CREATE TABLE IF NOT EXISTS player_stats (
     uuid   TEXT PRIMARY KEY,
     wins   INTEGER NOT NULL DEFAULT 0,
     losses INTEGER NOT NULL DEFAULT 0
   );
   ```
2. Fill in `config.yml`:
   ```yaml
   stats:
     backend: "supabase"
     supabase:
       url: "https://your-project.supabase.co"
       api-key: "your-anon-key"
       table: "player_stats"
   ```

#### Firebase Setup

1. Create a Firebase project and enable Realtime Database.
2. Fill in `config.yml`:
   ```yaml
   stats:
     backend: "firebase"
     firebase:
       database-url: "https://your-project-default-rtdb.firebaseio.com"
       auth-token: ""   # optional database secret
   ```

### How It Works

- **Local cache is always active** — `stats.yml` remains the primary cache. The plugin works normally even when the remote is unreachable.
- **Async sync** — All remote reads/writes happen asynchronously and never block the main server thread.
- **Merge on fetch** — When a player's stats are loaded from remote, the higher value for wins and losses is kept (local vs. remote), so no data is lost if two servers wrote different values.
- **Push on change** — Every win/loss is pushed to the remote immediately after updating the local cache.
- **Push on shutdown** — All cached stats are pushed to the remote during `onDisable` to ensure nothing is lost.

### Architecture

```
StatsProvider (interface)
├── SupabaseProvider  — PostgREST upsert via java.net.http
└── FirebaseProvider  — Realtime Database REST via java.net.http
```

No external libraries required — uses Java 21's built-in `HttpClient`.

### Other (included from v0.2.3)

- Redesigned 27-slot main menu with Duels / Stats / Matchmaking sub-menus
- Pagination for all list GUIs (28 items per page)
- Dedicated Stats menu
- `/duel spectate <player>` command
- Configurable lobby PvP protection
- Duel item cleanup on rejoin

---

## v0.2.3 — GUI Redesign, Spectator Mode & Lobby Safety

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### Major Changes

- **Redesigned Main Menu** — The `/duel` menu is now a clean 27-slot (3-row) chest with three centered icons:
  - **Slot 10 — Diamond Sword** → Opens the **Duels** sub-menu (player selection for private duels)
  - **Slot 13 — Player Head** → Opens the **Stats** sub-menu (dedicated statistics view)
  - **Slot 16 — Hopper** → Opens the **Matchmaking** sub-menu (kit-based queue selection)

- **Separate Sub-Menus** — Duels, Stats, and Matchmaking each have their own dedicated GUI instead of being crammed into one screen. This keeps things clean and scalable.

- **Pagination for all list GUIs** — Player select, kit select, arena/map select, matchmaking, kit list, and arena list all now support pagination with Previous/Next arrows and a Back button. Supports up to 28 items per page (4 rows × 7 columns).

- **Stats Menu** — Dedicated 27-slot stats view with player head, emerald (wins), redstone (losses), and book (total duels + win rate).

### New Features

- **Spectate Command** — `/duel spectate <player>` (alias: `/d spec <player>`) teleports you to a player's active duel in Spectator mode. You are automatically returned to the lobby when the duel ends or when you disconnect. Run the command again (or `/d spectate`) to stop spectating.

- **Configurable lobby PvP protection** — PvP damage is now disabled outside of active duels by default. Toggle in `config.yml`:
  ```yaml
  lobby:
    disable-pvp: true  # set to false to allow lobby PvP
  ```

### Bug Fixes

- **Fixed duel items persisting after rejoin** — Player inventory, armor, health, hunger, fire, and potion effects are fully reset on join.

### Commands Added in v0.2.3

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/duel spectate <player>` | `/d spec <player>` | Spectate an active duel | `epicduels.duel` |

---

## v0.2.2 — Hotfix: Arena Spawn & Shutdown Stability

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### Bug Fixes

- **Fixed NullPointerException on arena spawn locations** — Arena spawn points could become `null` after a server restart because `deserializeLocation` required the `world` key to exist in `arenas.yml`. If the key was missing (e.g. due to a failed prior save), the entire spawn was dropped. Now the Location is always created (with null world) and resolved lazily once the arena template world loads.

- **Fixed arena serialization crash with null world** — `serializeLocation` called `loc.getWorld().getName()` which threw a NullPointerException when the Location had a null world reference (possible between deserialization and world loading). Now uses the arena's template world name as fallback.

- **Fixed "Plugin attempted to register task while disabled"** — `deleteInstanceWorld` used `runTaskAsynchronously` during `onDisable()`, which is forbidden by Bukkit. Now detects whether the plugin is still enabled and runs folder deletion synchronously during shutdown.

- **Added null-spawn safety in duel start** — If an arena's spawn points are missing, the duel is cleanly aborted with an error message instead of crashing the server with an unhandled NPE.

### Improvements

- **Debug logging for arena loading** — On startup, the plugin now logs each arena's spawn state (`set` / `NOT SET`) and the resolved spawn coordinates after world loading, making it easier to diagnose configuration issues.

---

## v0.2.1 — Patch Release

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### Bug Fixes

- **Fixed world generator startup error** — Added `load: STARTUP` to `plugin.yml` so the plugin is loaded during the server startup phase (before worlds are created). Without this, Paper could not register the `VoidWorldGenerator` for the default lobby world and printed:
  ```
  Could not set generator for default world 'world': Plugin 'EpicDuels v0.2.0' is not enabled yet (is it load:STARTUP?)
  ```

### Other

- Added `CONTRIBUTING.md` — development guide, project structure, and code style conventions
- `LICENSE.txt` and `README.md` are now consistently present and up-to-date on all branches

---

## v0.2.0 — Major Feature Update

**Date:** April 2026
**Minecraft:** Paper 1.21.1+
**Java:** 21

### New Features

- **Redesigned Main Menu GUI** — The `/duel` menu is now a 54-slot chest split into three visual sections:
  - **Left (columns 1–3):** Challenge a Player — opens player selection, then kit selection, then map selection
  - **Middle (columns 4–6):** Stats — displays your wins, losses, win rate, and total duels directly in the GUI with your player head and a BOOK item showing colored lore
  - **Right (columns 7–9):** Queue / Matchmaking — per-kit queue buttons showing current queue counts; includes "Random Kit" (ENDER_PEARL) and "No Kit" (BARRIER) buttons

- **Queue / Matchmaking System (`QueueManager`)** — Kit-based auto-matchmaking:
  - Join a queue from the main menu or via `/duel queue <kit>`
  - Action bar updates every second: `Queue [KitName]: searching for opponent... (Xs)`
  - When two players queue for the same kit they are instantly matched, a random ready arena is chosen, and both are teleported in
  - Players removed from queue automatically on disconnect

- **Random Map Animation** — Clicking "Random Map" in the map selection GUI triggers a slot-machine animation:
  - Rapidly cycles through all arena icons, gradually slowing down over ~2 seconds
  - Plays `BLOCK_NOTE_BLOCK_HAT` ticks during cycling and `ENTITY_PLAYER_LEVELUP` on the final result
  - Automatically proceeds with the challenge after the animation finishes

- **Custom Arena & Kit Icons** — Admins can assign any item as the display icon:
  - `/duel arena seticon <name>` — hold item in hand
  - `/duel kit seticon <name>` — hold item in hand
  - Icons stored in `arenas.yml` and `kits.yml`; default icons: `GRASS_BLOCK` for arenas, `CHEST` for kits

- **Architecture Documentation** — Detailed comment block in `EpicDuels.java` explaining how all six managers interact

### Bug Fixes

- **Block protection in arena instances** — Players can now freely place blocks and break *player-placed* blocks during a duel. Original map blocks are protected: when an instance world is created, all non-air block positions are recorded in `DuelInstance#originalBlocks`; break attempts on those positions are cancelled.

- **Arena instance world cleanup** — Instance worlds are now properly unloaded with `Bukkit.unloadWorld(world, false)` and recursively deleted from disk via `Files.walkFileTree()`. Warnings are logged if deletion fails.

- **Disconnect mid-duel** — When a player disconnects during an active duel the duel immediately ends, the remaining player wins and is teleported to lobby, stats are updated, and the instance world is deleted.

### Breaking Changes

- **Removed `/duel setlobbyspawn1` and `/duel setlobbyspawn2`** — These commands no longer exist. Only `/duel setlobby` is supported. The `config.yml` no longer stores `lobby-spawn1` / `lobby-spawn2`.
- **Challenge flow reordered** — The GUI flow is now Player → Kit → Map (previously Player → Map → Kit).

### Commands Added in v0.2.0

| Command | Description | Permission |
|---|---|---|
| `/duel queue <kit>` | Join matchmaking queue | `epicduels.duel` |
| `/duel queue leave` | Leave the queue | `epicduels.duel` |
| `/duel arena seticon <name>` | Set arena display icon | `epicduels.admin` |
| `/duel kit seticon <name>` | Set kit display icon | `epicduels.admin` |

### Commands Removed in v0.2.0

| Command | Replacement |
|---|---|
| `/duel setlobbyspawn1` | Use `/duel setlobby` |
| `/duel setlobbyspawn2` | Use `/duel setlobby` |

---

## v1.0.0 — Initial Release

**Date:** early 2026

- Arena system with void template worlds and per-duel instance copies
- Kit system with full inventory / armor / offhand support and in-GUI editor
- Duel challenge flow via GUI (player → arena → kit) with 30-second expiry and clickable Accept/Deny
- 5-second countdown, player freeze, auto-win on death or disconnect
- Stats tracking (wins, losses, win rate) in `stats.yml`
- World protection: lobby locked for non-admins, template worlds open for admins, instance worlds locked during duels
- Void world generator with plains biome
