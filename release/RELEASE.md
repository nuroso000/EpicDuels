# EpicDuels — Release Notes

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
