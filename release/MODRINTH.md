# EpicDuels
<p align="center">
  <img src="https://i.ibb.co/VYNPYwxK/Neues-Projekt.png" width="100" alt="Logo">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1+-green">
  <img src="https://img.shields.io/badge/Java-21-orange">
  <a href="https://github.com/nuroso000/EpicDuels">
    <img src="https://img.shields.io/badge/GitHub-EpicDuels-181717?style=flat&logo=github&logoColor=white" alt="GitHub">
  </a>
  <a href="https://discordapp.com/users/1245992673677017119">
    <img src="https://img.shields.io/badge/Discord-Profile-5865f2?style=flat&logo=discord&logoColor=white" alt="Discord Profile">
  </a>
</p>

---

**Everything a duels server needs, in one plugin — and zero dependencies.**

Drop in one jar and your players get 1v1 challenges with Best-of-1/3/5, kit-based matchmaking, 2v2–4v4 team duels, party tournaments with a real bracket, one-click rematches, personal kit layouts, live spectating, leaderboards with floating holograms — each match in its own isolated arena world that is created and deleted automatically.

---

## Why EpicDuels?

- **Set up in five minutes.** `/duel setlobby`, build one arena, save one kit — done. Everything else is GUI-driven; your players never have to memorize commands.
- **No dependency roulette.** No hologram plugin, no world manager, no economy required. PlaceholderAPI is supported but strictly optional.
- **Made for real servers, not just duel networks.** Every single feature — matchmaking, rematch, tournaments, spectating, even the join-time lobby handling — has its own config toggle. Run it as a full duels server or as one mode on a survival hub.
- **Cheat-proof by design.** Every duel runs in a fresh copy of the arena, placed blocks reset between rounds, kits can only be *rearranged* by players (never modified), and inventory backups survive crashes and disconnects.
- **Speaks your players' language.** All messages ship in English and German and live in an editable MiniMessage file — add your own wording or a whole new language without touching code.

## Highlights

⚔️ **Duels the way players expect them** — challenge via GUI (player → kit → rounds → map), clickable Accept/Deny, countdown, victory screens, automatic draw handling with a match clock, and a `[REMATCH]` button after every fight.

🎯 **Matchmaking queues** — players queue per kit and are matched instantly, with live queue time on the action bar.

🎒 **Personal kit layouts** — every player can rearrange any kit's hotbar, armor and offhand for themselves in a drag-and-drop editor (`/duel kits`). Pure reordering, so there is nothing to exploit.

👥 **Parties, team duels & tournaments** — group up (2–8 players), fight 2v2/3v3/4v4 with auto-balanced teams, or run a single-elimination bracket where eliminated players automatically spectate the remaining matches.

👁 **Spectate anything** — `/duel spectate` opens a live browser of all running duels (players, kit, map, duration) — one click to watch.

🏆 **Stats, leaderboards & holograms** — wins, losses, win rate and a consistency-rewarding score; top-10 in chat or as floating in-world holograms (no hologram plugin needed). Optionally sync stats across servers via Supabase or Firebase.

🧩 **PlaceholderAPI support** — `%epicduels_wins%`, `%epicduels_losses%`, `%epicduels_winrate%`, `%epicduels_score%`, `%epicduels_in_duel%` for scoreboards, tab and chat plugins.

🛡 **A lobby that stays pristine** — Adventure mode plus 15 individually toggleable protection rules, with a per-admin bypass that never weakens protection for regular players.

## Getting started

1. Drop the jar into `plugins/`, restart.
2. `/duel setlobby` where players should wait.
3. `/duel arena create map1` → build → set the two spawns → `/duel arena save`.
4. Equip gear and `/duel kit create pvp`.

That's it — players open everything else through `/duel`.

## Requirements

**Paper 1.21.1+** and **Java 21**. Does not run on Spigot or Vanilla.

---

Full command reference, configuration guide and setup for multi-server stats: see the [GitHub README](https://github.com/nuroso000/EpicDuels) and the release notes.

*Licensed under CC BY-NC-SA 4.0 — free to use on any server, including revenue-generating ones. Not for resale.*
