# Changelog

All notable changes to **EpicDuels** are documented in this file.
Detailed release notes with setup instructions live in [release/RELEASE.md](release/RELEASE.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [3.0.0] — 2026-07 · Gameplay Update

### Added
- **Kit Editor** — every player can personalize the item layout of any kit for themselves: `/duel kits [kit]` or the new "Kit Editor" entry in the main menu opens a customize GUI (inventory, armor and offhand slots) where the kit's items can be rearranged, saved and reset. Layouts are pure rearrangements (nothing can be added, removed or modified), are stored per player in `playerkits.yml`, and are applied automatically in 1v1 duels, matchmaking, team duels and tournaments. If an admin edits the base kit, outdated personal layouts fall back to the default. New feature toggle `features.kit-editor`; new manager class `PlayerKitManager`.
- **Spectate GUI** — `/duel spectate` without arguments (and a new Ender-Eye entry in the main menu) opens a paginated browser of all running 1v1 duels showing both players, kit, map, round and duration. One click starts spectating; the duel is re-validated on click in case it ended while browsing. Team duels are not listed yet (no outside-spectator mechanism there).
- **Localization** — every player-facing message (~290 keys: chat, action bars, titles, clickable buttons) now lives in `lang/messages_en.yml` / `lang/messages_de.yml` (MiniMessage format), selected via the new `language` config key and reloadable with `/duel reload`. Missing keys fall back to the built-in English texts; player names are inserted tag-escaped (no MiniMessage injection). New class `dev.epicduels.i18n.Messages`.
- **PlaceholderAPI support** (optional softdepend) — `%epicduels_wins%`, `%epicduels_losses%`, `%epicduels_winrate%`, `%epicduels_score%` and `%epicduels_in_duel%` via a new expansion that only registers when PlaceholderAPI is installed.
- **Match time limit & draws** — configurable clock (`duel.time-limit-seconds`, default 300 s, 0 = off) with action-bar countdown. Time-up ends 1v1 and team duels in a draw (no win/loss recorded). Tournament matches resolve via a one-time **sudden-death extension** and/or **coin flip** (`duel.tournament-draw`, `duel.sudden-death-seconds`).
- **Best of 1 / 3 / 5** — new rounds-selection step in the challenge flow (player → kit → rounds → map). Rounds reuse the same arena instance: player-placed blocks are reset, players healed, re-kitted and counted down again. Score shown as titles and "Round N — X:Y" messages. The match clock pauses between rounds; a timeout with a round lead ends the match in the leader's favor.
- **Rematch** — clickable `[REMATCH]` message for both players after every regular duel (30 s validity). Both accept (click or `/duel rematch`) → new duel with the same kit, arena and round mode.
- **`/duel forfeit`** (aliases `ff`, `leave`) — give up the current 1v1 or team duel with a clickable 10-second confirmation. Counts as a loss.
- **Own Inventory (no-kit) duels** — "Own Inventory" option in the kit selection (`duel.allow-own-inventory`). Inventories are saved to `plugins/EpicDuels/inventories/` at duel start and restored afterwards — crash- and disconnect-safe (leftover backups restore on the next join).
- **`/duel toggle`** — disable/enable incoming duel requests, persisted in `toggles.yml`. Blocked players are hidden from the challenge menu.
- **`/duel reload`** — reload `config.yml` at runtime (`epicduels.admin`).
- **`lobby.handle-join`** — config toggle for the join-time inventory wipe + lobby teleport (disable when EpicDuels runs alongside other gameplay).
- **Feature toggles** — new `features:` config section: every player-facing feature (challenges, matchmaking, spectating, rematch, forfeit, best-of-N, own-inventory duels, parties, team duels, tournaments, leaderboards) can be disabled individually. Disabled features vanish from the GUIs (barrier icons) and their commands answer with a clear message. Missing entries default to enabled.
- **`duel.countdown-seconds`** — configurable pre-fight countdown length (0–60 s, 0 = instant start).
- **`duel.broadcast-results`** — choose between server-wide duel result announcements and participants-only messages (for survival/hub servers).
- New manager classes: `RematchManager`, `InventoryBackupManager`, `PlayerKitManager`.
- New data files: `toggles.yml`, `inventories/`, `playerkits.yml`, `lang/`.

### Changed
- **GUI internals refactored** — plugin menus are now identified by a custom `InventoryHolder` (`MenuHolder` with menu type, kit context and page) and clicks are resolved through `PersistentDataContainer` keys on the button items instead of window-title and display-name parsing. Fixes potential collisions with other plugins' GUIs (or kits named like buttons), stops clicks in the player's own inventory from triggering menu actions, gives the arena list working pagination, and blocks drags onto the admin kit editor's control row.
- `build.gradle` now mirrors the Maven build (PlaceholderAPI repo/dependency, jar version synced to the project version).

### Fixed
- **Duel-state guards** — players already in a duel, team duel or tournament can no longer be pulled into a second match via accept, queue or challenge (central `isBusy()` check).
- **Concurrent duel requests** — a second incoming challenge no longer silently overwrites the first; requests are tracked per sender and `/duel accept|deny <player>` disambiguate.
- **Stats thread safety** — the remote stats fetch callback (Supabase/Firebase) now hops back to the main thread before mutating the stats map.
- **Projectile PvP bypass** — lobby PvP protection now also blocks arrows, snowballs and other projectiles.
- **Arena-exit exploit** — teleporting out of the arena world (e.g. another plugin's `/spawn`) now counts as a forfeit instead of leaving the duel running forever.
- **Stale template copies** — the template world is saved before being copied into a duel instance.
- **Respawn flow** — dead team-duel players respawn inside the arena as spectators; admins in template worlds keep their default respawn.
- GUI state (challenge/party flow, random-map animation) is cleaned up when a plugin menu is closed.
- Kit edit GUI: unused slots 41–52 are blocked so items placed there are no longer lost on save.
- `/duel queue <kit>` switches queues instead of only leaving.
- Kit serialization switched to Paper's version-upgrade-safe `ItemStack.serializeItemsAsBytes` (legacy `kits.yml` still loads).
- `plugin.yml`: proper description/usage metadata; new `epicduels.spectate` permission.

---

## [2.0.0] — 2026-05 · Party System, Lobby Hardening & Stability

### Added
- **Party system** (2–8 players): `/party create|invite|accept|deny|leave|disband|list|start` with mode-select GUI.
- **Team Duels (2v2/3v3/4v4)** — shuffled teams, circular spawn offsets, friendly fire disabled, dead players spectate, last team standing wins.
- **Single-elimination tournaments** — 1v1 bracket with automatic byes; eliminated players auto-spectate live matches; winner announced party-only.
- **Adventure mode lobby** — players join and stay in Adventure mode; item frame & armor stand interaction protection.
- **Per-admin lobby bypass** — `/duel lobby off|on` toggles Creative + protection bypass for that admin only.
- New permission `epicduels.party`.

### Fixed
- Memory leaks: unreferenced repeating tasks (request expiration, matchmaking loop), leftover `arena_instance_*` worlds from crashes, ghost queue entries of disconnected players.
- Supabase sync: startup connection test, HTTP 204 accepted as success, actionable 401/403 hints, `pushAllToRemote()` awaits async futures on shutdown.

### Changed
- Hologram engine rewritten: in-place ArmorStand text updates instead of delete/respawn, 60 s refresh, cached name lookups — no more entity churn lag spikes.

---

## [1.0.0] — 2026-04 · Leaderboards, Holograms & Polish

### Added
- **In-chat leaderboards** — `/duel leaderboard wins|score` (top 10), score formula `wins² / (wins + losses)`.
- **In-world holograms** — `/duel leaderboard sethologram|removehologram <wins|score>`, ArmorStand-based, no external plugin, persisted in `leaderboards.yml`.

### Fixed
- Accepting a duel while queued no longer leaves the player in matchmaking (clean-slate duel start).
- Instant respawn for duel deaths — no more players stuck on the death screen.

---

## [0.3.0] — 2026-04 · Multi-Server Stats

### Added
- **Remote stats backends** — Supabase (PostgREST) and Firebase (Realtime Database REST) via Java 21 `HttpClient`; async push on every change and on shutdown; merge-on-fetch; `stats.yml` stays as local fallback cache.

---

## [0.2.3] — 2026-04 · GUI Redesign, Spectator Mode & Lobby Safety

### Added
- Redesigned 27-slot main menu (Duels / Stats / Matchmaking) with dedicated sub-menus.
- Pagination for all list GUIs (28 items per page).
- `/duel spectate <player>` with automatic lobby return.
- Configurable lobby PvP protection (`lobby.disable-pvp`).

### Fixed
- Duel items no longer persist after rejoin (full reset on join).

---

## [0.2.2] — 2026-04 · Hotfix: Arena Spawns & Shutdown Stability

### Fixed
- NPE on arena spawn locations after restart (lazy world resolution).
- Arena serialization crash with null world references.
- "Plugin attempted to register task while disabled" during shutdown world cleanup.
- Duels abort cleanly when arena spawns are missing.

---

## [0.2.1] — 2026-04 · Patch Release

### Fixed
- World generator startup error — `load: STARTUP` in `plugin.yml` so the void generator registers before world creation.

---

## [0.2.0] — 2026-04 · Major Feature Update

### Added
- Three-section main menu GUI (challenge / stats / queue).
- Queue/matchmaking system with action-bar status.
- Random-map slot-machine animation.
- Custom arena & kit icons (`seticon`).

### Fixed
- Block protection in arena instances (original blocks protected, player blocks breakable).
- Instance world cleanup (unload + recursive delete).
- Disconnect mid-duel ends the duel properly.

### Removed
- `/duel setlobbyspawn1` / `setlobbyspawn2` (replaced by `/duel setlobby`).

---

## Initial Release — early 2026

- Arena system with void template worlds and per-duel instance copies.
- Kit system with inventory/armor/offhand support and in-GUI editor.
- GUI challenge flow with 30-second expiry and clickable Accept/Deny.
- 5-second countdown, player freeze, auto-win on death or disconnect.
- Win/loss stats in `stats.yml`.
- World protection (lobby locked, templates admin-only, instances duel-only).
- Custom void world generator.
