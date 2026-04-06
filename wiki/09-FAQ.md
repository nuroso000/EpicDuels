# FAQ (Frequently Asked Questions)

Common questions and answers about EpicDuels.

---

## Installation & Setup

### Q: Which Minecraft version do I need?
**A:** EpicDuels requires **Paper 1.21.1 or higher**. Older versions are not supported.

### Q: What Minecraft version is my server running?
**A:** Your server must be **1.21.1+**. Check with:
```bash
/version
```

### Q: Do I need Java 21?
**A:** Yes! EpicDuels requires **Java 21 or higher**. Check your Java version:
```bash
java -version
```

### Q: Where do I download EpicDuels?
**A:** Here:
- 📥 [GitHub Releases](https://github.com/nuroso000/epicduels/releases)
- 🎮 [Modrinth](https://modrinth.com/plugin/epicduels)

### Q: Where do I place the JAR file?
**A:** In your server's `plugins/` folder:
```
server/
└── plugins/
    └── EpicDuels-0.2.1.jar
```

### Q: The server won't start after installation
**A:** Check:
1. ✅ Java version is 21+
2. ✅ JAR file is in `plugins/`
3. ✅ JAR file isn't corrupted
4. ✅ Check `logs/latest.log` for errors

---

## Gameplay & Features

### Q: What is a "Duel"?
**A:** A 1v1 fight between two players. One wins, one loses. Stats are updated.

### Q: Can I duel with more players?
**A:** No, EpicDuels is 1v1 only. Multiplayer duels are not supported.

### Q: What is the "Queue"?
**A:** A waiting list for automatic matchmaking. You wait until another player selects the same kit, then you're automatically matched.

### Q: How long does a duel take?
**A:** As long as it takes until one player dies or disconnects. Usually 2-5 minutes.

### Q: Can I build during a duel?
**A:** Yes! You can:
- ✅ Place blocks
- ✅ Break your own blocks
- ❌ Cannot break original arena blocks
- ❌ Cannot break opponent's blocks

### Q: What happens if both players die?
**A:** The player who died first loses. The other wins.

### Q: Can I bring items with me?
**A:** No! You get the items from the kit. Your items aren't used.

### Q: Are my stats deleted?
**A:** No, stats are permanent in `stats.yml`. They're never auto-deleted.

---

## Kits & Arenas

### Q: How do I create a kit?
**A:** 
1. Equip yourself with the desired gear
2. `/duel kit create my_kit`
3. Done!

### Q: Can I change a kit after creation?
**A:** Yes! Use `/duel kit edit my_kit` to modify items.

### Q: How many kits can I create?
**A:** Unlimited!

### Q: How do I create an arena?
**A:**
1. `/duel arena create my_arena` — Teleports to template
2. Build your arena
3. Set spawns: `/duel arena setspawn1` and `/duel arena setspawn2`
4. `/duel arena save` — Saves the arena

### Q: Can I edit an arena after creation?
**A:** Yes! Use `/duel arena tp my_arena` to go back to the template and edit.

### Q: How many arenas can I create?
**A:** Unlimited, but keep in mind:
- Each arena takes storage space
- Each duel creates a world copy
- Too many arenas = performance issues

### Q: Are arena worlds deleted?
**A:** Yes! After each duel, the world copy is automatically deleted. Clean system, no storage problems.

### Q: Players can't see an arena
**A:** Check:
1. Arena has both spawns set
2. Arena has status `ready: true`
3. Players have permission for the kit

### Q: Can I change the arena icon?
**A:** Yes! Hold an item and use `/duel arena seticon my_arena`.

---

## Statistics

### Q: Where are statistics stored?
**A:** In `plugins/EpicDuels/stats.yml`

### Q: Can I view statistics?
**A:** Yes, with `/duel stats` or `/duel stats <player>`

### Q: Can I reset statistics?
**A:** Yes, but you must manually edit `stats.yml`:
```yaml
stats:
  "player-uuid":
    wins: 0
    losses: 0
    totalGames: 0
```

### Q: How is win rate calculated?
**A:**
```
Win Rate (%) = (Wins / Total Games) * 100
```

**Example:** 15 Wins / 30 Duels = 50% Win Rate

### Q: Does each player have separate stats?
**A:** Yes, each player has their own stats.

---

## Administration

### Q: How do I give players permissions?
**A:** Use a permission manager like LuckPerms:
```bash
/lp user <player> permission set epicduels.duel true
```

### Q: What permissions exist?
**A:**
- `epicduels.admin` — Admin commands
- `epicduels.duel` — Play duels
- `epicduels.stats` — View stats

### Q: Can players create arenas themselves?
**A:** No, only admins with `epicduels.admin` can create arenas.

### Q: What is a "Void World Generator"?
**A:** A custom generator that creates empty worlds. Add to `bukkit.yml`:
```yaml
worlds:
  world:
    generator: EpicDuels
```

### Q: Do I need to restart the server after configuring?
**A:** Yes, always after changes to `bukkit.yml`. For other files, sometimes not (but recommended).

---

## Problems & Errors

### Q: Plugin won't load
**A:** Check:
1. Java 21+
2. Paper 1.21.1+
3. Server logs for error messages
4. JAR file isn't corrupted

### Q: Players can't run `/duel`
**A:** Check permissions:
```bash
/lp user <player> permission check epicduels.duel
```

### Q: Arena worlds aren't deleted
**A:** That's normal! The world is deleted after the duel. Sometimes takes a few seconds.

### Q: Stats aren't being saved
**A:** Check:
1. File permissions
2. Server logs for errors
3. `stats.yml` exists and is writable

### Q: Duel won't start
**A:** Check:
1. ✅ At least 1 arena exists
2. ✅ Arena has 2 spawns
3. ✅ Arena has status `ready`
4. ✅ At least 1 kit exists

### Q: Player won't teleport to arena
**A:** Check:
1. Arena world exists
2. Spawn coordinates are valid
3. Server logs for errors

---

## Performance & Optimization

### Q: The plugin is slow
**A:** 
1. ✅ Ensure server has enough RAM (minimum 4GB)
2. ✅ Limit arena count (e.g., 10)
3. ✅ Simplify arena complexity
4. ✅ Check other plugins

### Q: Server crashes during duels
**A:**
1. Check RAM
2. Check logs for Out of Memory
3. Reduce number of simultaneous players
4. Check other plugins for conflicts

### Q: Players lag during duels
**A:**
1. Check arena size (not too big)
2. Check particle effects
3. Check other plugins
4. Check internet connection

---

## License & Legal

### Q: Can I use EpicDuels commercially?
**A:** Yes, you can use it on a money-making server. **But:** You cannot sell the plugin itself.

### Q: Can I modify EpicDuels?
**A:** Yes! It's open source (CC BY-NC-SA 4.0).

### Q: Must I keep the code open?
**A:** Yes, if you redistribute it, the code must remain open.

### Q: Who developed EpicDuels?
**A:** [nuroso000](https://github.com/nuroso000) on GitHub.

---

## Need More Help?

### Q: My question isn't answered here
**A:** 
- 📖 Check the [Documentation](./01-Home.md)
- 🎮 [Admin Guide](./04-Admin-Guide.md)
- 📋 [Commands Reference](./05-Commands.md)
- 🔐 [Permissions Guide](./06-Permissions.md)

### Q: I want to report a bug
**A:** Open an issue on [GitHub](https://github.com/nuroso000/epicduels/issues)

### Q: I want to suggest a feature
**A:** Open an issue on [GitHub](https://github.com/nuroso000/epicduels/issues) with "Feature Request"

### Q: I want to contribute
**A:** See [Development Guide](./08-Development.md) and [CONTRIBUTING.md](../CONTRIBUTING.md)

---

## Checklists

### Installation Checklist
- [ ] Java 21+ installed
- [ ] Paper 1.21.1+ installed
- [ ] EpicDuels JAR downloaded
- [ ] JAR placed in `plugins/`
- [ ] Server started
- [ ] Plugin loaded (check logs)

### Setup Checklist
- [ ] Lobby set with `/duel setlobby`
- [ ] At least 1 arena created
- [ ] Arena has 2 spawns set
- [ ] Arena saved
- [ ] At least 1 kit created
- [ ] Permissions configured

### First Duel Checklist
- [ ] Player has `epicduels.duel` permission
- [ ] Player knows `/duel` command
- [ ] Arena and kit exist
- [ ] `/duel` opens GUI
- [ ] Player can accept challenge
- [ ] Duel starts successfully

---

**Have fun with EpicDuels! 🎮**

---

*Last updated: 2026-04-06*
