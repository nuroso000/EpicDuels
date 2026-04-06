# Commands Reference

Complete list of all EpicDuels commands.

## 📋 Overview

- **[Player Commands](#player-commands)** — For regular players
- **[Admin Commands](#admin-commands)** — For server administrators
- **[Shorthand](#shorthand)** — Abbreviations

---

## Player Commands

All players can use these commands (if they have permission).

### Menu

```bash
/duel
/duel menu
```
Opens the main menu with three options:
- Challenge
- Stats
- Queue

**Permission:** `epicduels.duel`

---

### Challenges

#### Start a Challenge
```bash
/duel challenge <player>
```

Opens a GUI to challenge a player:
1. Player is already selected
2. Select a kit
3. Select a map

The opponent receives an Accept/Deny request.

**Permission:** `epicduels.duel`

**Example:**
```bash
/duel challenge NotchMC
```

#### Accept Challenge
```bash
/duel accept [player]
```

Accept an incoming challenge request.

**Permission:** `epicduels.duel`

**Example:**
```bash
/duel accept
/duel accept Steve
```

#### Deny Challenge
```bash
/duel deny [player]
```

Deny an incoming challenge request.

**Permission:** `epicduels.duel`

**Example:**
```bash
/duel deny
/duel deny Steve
```

#### Cancel Challenge
```bash
/duel cancel
```

Cancel your current outgoing challenge request.

**Permission:** `epicduels.duel`

---

### Statistics

```bash
/duel stats [player]
```

View duel statistics:
- **Wins** — Duels won
- **Losses** — Duels lost
- **Total** — Total duels played
- **Win Rate** — Win percentage

**Permission:** `epicduels.stats`

**Example:**
```bash
/duel stats              # Your own stats
/duel stats NotchMC      # NotchMC's stats
```

---

### Matchmaking Queue

#### Join Queue
```bash
/duel queue <kit>
```

Join the matchmaking queue for a kit.

The plugin waits for another player:
- Both players must select the same kit
- When 2 players queue → automatic match
- Both get teleported to a random arena
- Duel starts after 5-second countdown

**Action bar** shows current queue size.

**Permission:** `epicduels.duel`

**Example:**
```bash
/duel queue pvp_sword
/duel queue speed_pvp
```

#### Leave Queue
```bash
/duel queue leave
```

Leave the current matchmaking queue.

**Permission:** `epicduels.duel`

---

## Admin Commands

Only administrators with `epicduels.admin` permission can use these commands.

### Arena Management

#### Create Arena
```bash
/duel arena create <name>
```

Create a new arena.

**Permission:** `epicduels.admin`

**Example:**
```bash
/duel arena create pvp_1
/duel arena create sky_arena
```

#### Set Spawn 1
```bash
/duel arena setspawn1
```

Set your current location as spawn point 1 (must be in arena world).

**Permission:** `epicduels.admin`

#### Set Spawn 2
```bash
/duel arena setspawn2
```

Set your current location as spawn point 2 (must be in arena world).

**Permission:** `epicduels.admin`

#### Save Arena
```bash
/duel arena save
```

Save the current arena and mark it as "ready".

**Permission:** `epicduels.admin`

#### Delete Arena
```bash
/duel arena delete <name>
```

Permanently delete an arena.

**Permission:** `epicduels.admin`

**⚠️ Warning:** This cannot be undone!

**Example:**
```bash
/duel arena delete pvp_1
```

#### Arena List
```bash
/duel arena list
```

Show all arenas with their status (ready/building).

**Permission:** `epicduels.admin`

#### Teleport to Arena
```bash
/duel arena tp <name>
```

Teleport to the arena for editing.

**Permission:** `epicduels.admin`

**Example:**
```bash
/duel arena tp pvp_1
```

#### Set Arena Icon
```bash
/duel arena seticon <name>
```

Set the arena's icon to the item in your hand.

**Permission:** `epicduels.admin`

**Example:**
```bash
# Hold a Grass Block in your hand
/duel arena seticon pvp_1
```

---

### Kit Management

#### Create Kit
```bash
/duel kit create <name>
```

Save your current inventory as a kit.

**Permission:** `epicduels.admin`

**Example:**
```bash
/duel kit create survival_pvp
/duel kit create ranged_kit
```

#### Delete Kit
```bash
/duel kit delete <name>
```

Permanently delete a kit.

**Permission:** `epicduels.admin`

**⚠️ Warning:** Players can no longer use this kit!

**Example:**
```bash
/duel kit delete old_kit
```

#### Edit Kit
```bash
/duel kit edit <name>
```

Open a chest GUI to edit the kit.

**Permission:** `epicduels.admin`

**Example:**
```bash
/duel kit edit survival_pvp
```

#### Preview Kit
```bash
/duel kit preview <name>
```

Show kit items in a read-only chest GUI.

**Permission:** `epicduels.admin`

**Example:**
```bash
/duel kit preview survival_pvp
```

#### Kit List
```bash
/duel kit list
```

Show all available kits.

**Permission:** `epicduels.admin`

#### Set Kit Icon
```bash
/duel kit seticon <name>
```

Set the kit's icon to the item in your hand.

**Permission:** `epicduels.admin`

**Example:**
```bash
# Hold a Diamond Sword in your hand
/duel kit seticon survival_pvp
```

---

### Lobby Management

#### Set Lobby Spawn
```bash
/duel setlobby
```

Set your current location as the lobby spawn.

**Permission:** `epicduels.admin`

---

## Shorthand

All `/duel` commands also work with the shorthand `/d`:

```bash
/d                       # = /duel
/d menu                  # = /duel menu
/d challenge Steve       # = /duel challenge Steve
/d accept                # = /duel accept
/d deny                  # = /duel deny
/d cancel                # = /duel cancel
/d stats                 # = /duel stats
/d queue pvp             # = /duel queue pvp
/d queue leave           # = /duel queue leave

# Admin commands
/d arena create pvp_1    # = /duel arena create pvp_1
/d arena list            # = /duel arena list
/d kit list              # = /duel kit list
/d kit create my_kit     # = /duel kit create my_kit
```

---

## Permission Overview

| Command | Permission | Default |
|---|---|---|
| `/duel` (Menu) | — | All |
| `/duel challenge` | `epicduels.duel` | All |
| `/duel accept/deny/cancel` | `epicduels.duel` | All |
| `/duel stats` | `epicduels.stats` | All |
| `/duel queue` | `epicduels.duel` | All |
| All arena commands | `epicduels.admin` | OP |
| All kit commands | `epicduels.admin` | OP |
| `/duel setlobby` | `epicduels.admin` | OP |

---

## Invalid Commands

If you enter a command incorrectly:

```bash
/duel invalid_command
# → Error: Unknown command
```

Check:
1. Spelling
2. Required parameters
3. Permissions
4. Valid kit/arena names

---

## Next Steps

- 🔐 **Permissions:** [Permissions Guide](./06-Permissions.md)
- 🎮 **Playing:** [Player Guide](./03-Player-Guide.md)
- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
