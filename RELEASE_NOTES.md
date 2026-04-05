# Release Notes — EpicDuels v1.0.0

**Release Date:** April 2026
**Minecraft:** Paper 1.21.1 | **Java:** 21

---

## What's New

This is the initial release of EpicDuels — a complete 1v1 duels plugin built from scratch.

### Arena System
- Create unlimited arenas as isolated void worlds
- Full setup wizard: create, build, set spawns, save
- Each duel runs in a fresh world copy — no cross-match interference
- Automatic cleanup: instance worlds are unloaded and deleted after every duel
- Admin teleport to edit arenas at any time

### Kit System
- Save your current inventory (armor, offhand, hotbar, full inventory) as a named kit
- Edit kits through an interactive chest GUI
- Preview kits in a read-only view
- Stored as Base64-serialized ItemStacks in `kits.yml`

### Duel Challenge Flow
- Challenge via command (`/duel challenge <player>`) or through the GUI menu
- Step-by-step selection: pick player → pick arena → pick kit
- Clickable `[ACCEPT]` / `[DENY]` buttons in chat
- 30-second request expiration with automatic notification
- Cancel outgoing requests with `/duel cancel`

### Duel Gameplay
- Async world copy for minimal server impact
- 5-second countdown with Title API (5, 4, 3, 2, 1, FIGHT!)
- Players frozen during countdown (no movement, no damage)
- Sound effects throughout (noteblock pings, dragon growl on fight start)
- Winner declared on death or disconnect
- Victory/Defeat title screens with 3-second display
- Automatic inventory clear, health restore, and lobby teleport

### Stats
- Per-player wins, losses, total games, and win rate
- Formatted chat display with a styled stats box
- View your own or another player's stats
- Persistent storage in `stats.yml`

### GUI Menus
- **Main Menu:** Challenge Player, My Stats, Kits, Arenas
- **Player Select:** Player heads of all online players
- **Arena Select:** All ready arenas with status indicators
- **Kit Select:** All available kits
- **Kit Editor:** Full chest GUI to rearrange kit items
- **Kit Preview:** Read-only view of kit contents
- Colored glass pane borders on all menus
- Click sounds and anti-item-theft protection

### World & Lobby
- Custom `VoidWorldGenerator` with empty chunks and plains biome
- Lobby world configured on first start (mob spawning off, daylight cycle off, peaceful)
- Players teleported to lobby on join
- Block break/place protection in lobby (admins exempt) and during duels
- Arena template worlds always allow building for admins

### Permissions
- `epicduels.admin` — Full admin access (default: OP)
- `epicduels.duel` — Challenge and accept duels (default: everyone)
- `epicduels.stats` — View statistics (default: everyone)

---

## Technical Details

- **API:** Paper 1.21.1 with Adventure API (Component, Title, ClickEvent)
- **Architecture:** 5 manager classes (Arena, Kit, Duel, Stats, GUI), clean separation of concerns
- **Storage:** YAML files with automatic save on changes
- **World Management:** Async file copy, per-duel instance worlds, full cleanup lifecycle
- **Thread Safety:** ConcurrentHashMap for active duels and frozen players

## Known Limitations

- Stats are only tracked for online players (no offline lookup by name)
- No ELO/ranking system (planned for v2)
- No spectator mode (planned for v2)
- No multi-round or best-of-3 duels (planned for v2)
- Arena worlds must be built manually in-game (no schematic import)

## Planned for v2.0

- ELO rating and leaderboard
- Spectator mode
- Queue system with automatic matchmaking
- Best-of-3 and tournament modes
- Arena schematic import/export
- Per-arena kit restrictions
- MySQL/SQLite database support
