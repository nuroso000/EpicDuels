# <h1 align="center">EpicDuels</h1>
<p align="center">
  <img src="https://i.ibb.co/VYNPYwxK/Neues-Projekt.png" width="100" alt="Logo">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1--26.3-green">
  <img src="https://img.shields.io/badge/Java-21--bytecode-orange">
  <a href="https://modrinth.com/plugin/epicduels">
    <img src="https://img.shields.io/badge/Modrinth-EpicDuels-00AF5C?style=flat&logo=modrinth&logoColor=white" alt="Modrinth">
  </a>
  <a href="https://discordapp.com/users/1245992673677017119">
    <img src="https://img.shields.io/badge/Discord-Profile-5865f2?style=flat&logo=discord&logoColor=white" alt="Discord Profile">
  </a>
</p>

**The complete duels experience for Paper 1.21.1–26.3 in a single, dependency-free jar.**

Version 3.1.1 extends the single-jar compatibility range through Paper 26.3.
Version 3.1.0 added configurable menus and context sidebars for the lobby,
duels, spectators, and matchmaking. See [CONFIGURATION.md](CONFIGURATION.md)
for examples, placeholders, layout constraints, and reload behavior.

1v1 challenges with Best-of-1/3/5, kit matchmaking, 2v2–4v4 team duels, party tournaments, one-click rematches, per-player kit layouts, a live spectate browser, leaderboard holograms and optional cross-server stats — every match in its own automatically managed void-world arena.

Why server owners pick it:

- **Five-minute setup, GUI-first UX.** One lobby point, one arena, one kit — after that players do everything through `/duel`. No player ever needs a command list.
- **Adapts to your server concept.** Every feature has a `features.*` toggle, duel results can be broadcast or kept private, and even the join-time lobby handling can be turned off — so EpicDuels works as a dedicated duels server *or* as one mode among many.
- **Engineered against exploits.** Isolated per-duel world copies, block tracking with per-round resets, rearrange-only kit personalization, crash-safe inventory recovery for every duel mode, and GUI identification via `InventoryHolder`/PDC instead of fragile title matching.
- **Localized.** All ~290 player-facing messages live in MiniMessage language files (English and German included, `language:` config key) — edit any wording or add your own language, reloadable at runtime.
- **Integrates, but never requires.** PlaceholderAPI placeholders, Supabase/Firebase stats sync, built-in holograms — all optional, none needed.

---

## Feature overview

| Area | What you get |
|---|---|
| **1v1 duels** | GUI challenge flow (player → kit → rounds → map), clickable accept/deny, match clock with draws, sudden death for tournament matches, forfeit with confirmation, rematch offers |
| **Matchmaking** | Per-kit queues, instant pairing, random arena, live queue time on the action bar |
| **Kit Editor** | Every player rearranges any kit's hotbar/armor/offhand for themselves (`/duel kits`) — validated as a pure reorder, applied in all modes |
| **Parties** | 2–8 players, invites, team duels (2v2/3v3/4v4, shuffled teams, no friendly fire) and single-elimination tournaments with auto-spectating eliminated players |
| **Spectating** | `/duel spectate` opens a browser of all live duels (players, kit, map, duration); one click to watch, automatic return to lobby |
| **Stats** | Wins/losses/win rate + score (`wins² / (wins+losses)`), chat top-10, in-world ArmorStand holograms, optional Supabase/Firebase sync |
| **Arenas** | Void-world builder with creative mode, template + per-duel instance copies, original-block protection, custom icons |
| **Lobby** | Adventure mode, 15 toggleable protection rules, per-admin bypass, optional PvP block (projectile-proof) |

## Installation

1. Drop `EpicDuels-3.x.jar` into `plugins/` and restart. Use Java 21 for
   Paper 1.21.x or Java 25 for Paper 26.1+.
2. `/duel setlobby` — set the lobby spawn.
3. `/duel arena create map1` → build → `/duel arena setspawn1` / `setspawn2` → `/duel arena save`.
4. Equip gear → `/duel kit create pvp`.
5. Players take it from here with `/duel`.

Optional void lobby world (in `bukkit.yml`):

```yaml
worlds:
  world:
    generator: EpicDuels
```

## Commands at a glance

| Command | Purpose |
|---|---|
| `/duel` (`/d`) | Main menu — duels, spectate, stats, kit editor, matchmaking |
| `/duel challenge <player>` | Challenge someone (GUI flow) |
| `/duel queue <kit>` | Join/switch matchmaking queue (`leave` to exit) |
| `/duel kits [kit]` | Personalize kit layouts (Kit Editor) |
| `/duel spectate [player]` | Browse live duels or watch a specific player |
| `/duel forfeit` · `/duel rematch` · `/duel toggle` | Give up, rematch, block requests |
| `/duel stats [player]` · `/duel leaderboard <wins\|score>` | Stats and top-10 |
| `/party create/invite/start …` | Party system (team duels & tournaments) |
| `/duel arena …` · `/duel kit …` · `/duel setlobby` · `/duel reload` | Admin setup |

Permissions: `epicduels.duel`, `epicduels.stats`, `epicduels.party`, `epicduels.spectate` (all default true) and `epicduels.admin` (OP). The full command/permission reference lives in [release/RELEASE.md](release/RELEASE.md).

## Configuration highlights

Everything lives in a fully documented `config.yml`, reloadable via `/duel reload`:

```yaml
language: en          # en | de — all messages in lang/messages_<code>.yml (MiniMessage)

duel:
  time-limit-seconds: 300      # match clock, 0 = off
  tournament-draw: sudden-death
  countdown-seconds: 5
  broadcast-results: true      # false = results only to the fighters

result-commands:               # optional console commands after a match
  winner:
    - "ac add {player} 100"
  loser:
    - "ac take {player} 100"

features:             # switch any feature off individually
  challenges: true
  matchmaking: true
  spectating: true
  rematch: true
  kit-editor: true
  parties: true
  tournaments: true
  # ... and more

stats:
  backend: local      # local | supabase | firebase (cross-server sync)
```

Result commands run once after the complete match (not after individual
Best-of-N rounds). In the `winner` list, `{player}` is the winner; in the
`loser` list, it is the loser. Team-duel commands run once per player. Empty
lists disable the integration, draws run no commands, and `/duel reload`
applies changes. Additional placeholders are `{player_uuid}`, `{arena}`,
`{kit}`, `{score}`, `{winner_score}` and `{loser_score}`. Team matches expose
the binary score `1:0`; a tournament draw resolved by coin flip still runs no
result command. These are privileged console commands, so only trusted server
operators should be able to edit `config.yml`.

## PlaceholderAPI

With PlaceholderAPI installed (optional softdepend), EpicDuels registers:

| Placeholder | Value |
|---|---|
| `%epicduels_wins%` / `%epicduels_losses%` | Total wins / losses |
| `%epicduels_winrate%` | Win rate percentage (e.g. `62.5`) |
| `%epicduels_score%` | Leaderboard score (`wins² / (wins+losses)`) |
| `%epicduels_in_duel%` | `true` while in a duel, team duel or tournament |

## Data files

`config.yml`, `menus.yml`, `scoreboard.yml`, `arenas.yml`, `kits.yml`, `playerkits.yml` (personal layouts), `stats.yml`, `leaderboards.yml`, `toggles.yml`, `lang/` (messages) and `inventories/` (temporary inventory recovery records for all duel modes) — all created and managed automatically.

## Building from source

```bash
mvn clean package     # → target/EpicDuels.jar
# or
gradle clean build    # → build/libs/EpicDuels-3.1.1.jar
```

The jar targets Java 21 bytecode and compiles against the Paper 1.21.1 API to
retain the oldest supported server. Paper 1.21.x runs on Java 21; Paper 26.1+
requires Java 25. See [CHANGELOG.md](CHANGELOG.md) for version history and
[release/RELEASE.md](release/RELEASE.md) for detailed release notes.


<a href="https://www.star-history.com/?repos=nuroso000%2Fepicduels&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=nuroso000/epicduels&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=nuroso000/epicduels&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=nuroso000/epicduels&type=date&legend=top-left" />
 </picture>
</a>

## License & usage

Licensed under **CC BY-NC-SA 4.0**:

- **Allowed:** Use on any server, including revenue-generating ones (ranks, donations, webstores).
- **Prohibited:** Selling the plugin or modified versions, or bundling it in paid packs without permission.
- **Share-alike:** Modified redistributions must stay open source under this license.
