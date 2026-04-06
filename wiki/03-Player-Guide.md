# Player Guide

Everything players need to know about EpicDuels.

## Main Menu

```
/duel menu
```

The main menu has three sections:

### 1. Challenge
Challenge a player to a duel:
- Select an **online player**
- Select a **kit** (equipment)
- Select an **arena** (map)

The other player receives a request with Accept/Deny buttons (30-second timeout).

### 2. Stats
View your duel statistics:
- **Wins** — Duels won
- **Losses** — Duels lost
- **Total** — Total duels played
- **Win Rate** — Percentage of wins

```
/duel stats [player]  # View your or another player's stats
```

### 3. Queue (Matchmaking)
Join a matchmaking queue and be automatically matched against another player:

```
/duel queue <kit>
```

**How It Works:**
1. You join the queue for a kit
2. The action bar shows the current queue size
3. When another player queues for the same kit, you're automatically matched
4. You're teleported to a random arena
5. The duel starts after a 5-second countdown

**Leave Queue:**
```
/duel queue leave
```

---

## Duel Flow

### Phase 1: Countdown
- 5 seconds before start
- Both players are frozen at spawn
- Title displays the countdown

### Phase 2: Combat
- You can move and attack
- Your opponent can move and attack
- Block placement is possible, but:
  - ✅ You can break your own blocks
  - ❌ Original arena blocks cannot be broken
  - ❌ Opponent's blocks cannot be broken

### Phase 3: End
- **Winner:** The player who eliminates the opponent or the opponent disconnects
- 3-second victory screen
- Return to lobby

### After the Duel
- **Stats are updated** (Win/Loss)
- **World is deleted** (Cleanup)
- You can start a new duel immediately

---

## Player Commands

| Command | Description |
|---|---|
| `/duel` | Open the main menu |
| `/duel menu` | Open the main menu (alternative) |
| `/duel challenge <player>` | Challenge a player |
| `/duel accept [player]` | Accept a duel request |
| `/duel deny [player]` | Deny a duel request |
| `/duel cancel` | Cancel your current request |
| `/duel stats [player]` | View statistics |
| `/duel queue <kit>` | Join a matchmaking queue |
| `/duel queue leave` | Leave the queue |

**Shorthand:** All `/duel` commands also work with `/d`

```bash
/d              # Shorthand for /duel
/d challenge NotchMC    # Shorthand for /duel challenge NotchMC
/d queue pvp    # Shorthand for /duel queue pvp
```

---

## Strategy & Tips

### Kit Selection
- 🗡️ **Sword Kits** — Melee, good in direct combat
- 🏹 **Ranged Kits** — Ranged combat, positioning matters
- 🛡️ **Tank Kits** — Heavy armor, longer survival
- ⚡ **Speed Kits** — Speed boost, mobility focused

### Arena Dynamics
- Use the **heights and depths** of the arena for tactics
- Think about your **positioning**
- Watch out for **fall damage**

### Queue Tips
- The **queue is the same for everyone** — Arena doesn't matter
- **Action bar** shows current waiting players
- Queue is **anonymous** — You don't know who you're playing

---

## Permissions

These permissions control what you can do:

| Permission | Access |
|---|---|
| `epicduels.duel` | Challenges, queue, accept/deny |
| `epicduels.stats` | View stats |

Default: All players have these permissions.

---

## Frequently Asked Questions

**Q: Can multiple players duel at once?**
A: No, EpicDuels is 1v1 only. Each duel runs in a separate world copy.

**Q: Can I build during a duel?**
A: Yes! You can place blocks, but:
   - You can only break blocks you placed yourself
   - Original arena blocks cannot be broken

**Q: What happens if I disconnect?**
A: The other player wins automatically. Your stats are updated.

**Q: How many times can I duel?**
A: Unlimited! There's no limit on number or time.

**Q: Are my stats deleted?**
A: No, stats are permanent and stored locally.

👉 **More Questions:** [FAQ](./09-FAQ.md)

---

## Next Steps

- 🏗️ **Arena Details:** Ask your admin which arenas are available
- 📊 **Track Progress:** Use `/duel stats` to see your progress
- 🎮 **Start Dueling:** Launch your first duel with `/duel` or `/d queue <kit>`
