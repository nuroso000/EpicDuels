# Permissions Guide

The EpicDuels permission system.

## Permission List

EpicDuels uses the following permissions:

| Permission | Description | Default | Command |
|---|---|---|---|
| `epicduels.admin` | All admin commands | OP | Arena & Kit Management |
| `epicduels.duel` | Challenges, queue, accept/deny | All | `/duel` commands |
| `epicduels.stats` | View stats | All | `/duel stats` |

---

## Permission Details

### epicduels.admin
**Group:** Admin/Moderator
**Default:** OP (Operator)
**Access to:**
- ✅ All arena commands (`/duel arena ...`)
- ✅ All kit commands (`/duel kit ...`)
- ✅ `/duel setlobby`

```yml
epicduels:
  admin: true
```

### epicduels.duel
**Group:** Players
**Default:** All
**Access to:**
- ✅ `/duel` (Menu)
- ✅ `/duel challenge <player>`
- ✅ `/duel accept [player]`
- ✅ `/duel deny [player]`
- ✅ `/duel cancel`
- ✅ `/duel queue <kit>`
- ✅ `/duel queue leave`

```yml
epicduels:
  duel: true
```

### epicduels.stats
**Group:** Players
**Default:** All
**Access to:**
- ✅ `/duel stats [player]`

```yml
epicduels:
  stats: true
```

---

## Permission Management

### With LuckPerms (Recommended)

**Add user to group:**
```bash
/lp user <player> parent add epicduels
```

**Give admin permissions:**
```bash
/lp user <player> permission set epicduels.admin true
```

**Give player permissions:**
```bash
/lp user <player> permission set epicduels.duel true
/lp user <player> permission set epicduels.stats true
```

**Create group with permissions:**
```bash
/lp creategroup duelers
/lp group duelers permission set epicduels.duel true
/lp group duelers permission set epicduels.stats true
```

### With bukkit.yml

Edit `bukkit.yml`:

```yaml
permissions:
  epicduels.*:
    description: All EpicDuels Permissions
    children:
      epicduels.admin: true
      epicduels.duel: true
      epicduels.stats: true
  
  epicduels.admin:
    description: Admin commands
    default: op
  
  epicduels.duel:
    description: Duel commands
    default: true
  
  epicduels.stats:
    description: View stats
    default: true
```

### With PEX (PermissionsEx)

```yaml
groups:
  default:
    permissions:
      - epicduels.duel
      - epicduels.stats
  
  admin:
    permissions:
      - epicduels.admin
      - epicduels.duel
      - epicduels.stats
```

---

## Scenario Examples

### Scenario 1: Standard Server

**Goal:** Everyone can play, only admins build arenas

```bash
# Default group gets these permissions:
epicduels.duel: true
epicduels.stats: true

# Admin group gets these:
epicduels.admin: true
epicduels.duel: true
epicduels.stats: true
```

### Scenario 2: VIP System

**Goal:** VIPs get access to specific kits

**Note:** EpicDuels doesn't have built-in kit permissions. You'd need to implement this yourself or use a different approach.

### Scenario 3: Restricted Server

**Goal:** Only specific players can duel

```bash
# Default players WITHOUT epicduels.duel
# Only whitelisted players GET epicduels.duel
/lp user Steve permission set epicduels.duel true
```

---

## Troubleshooting

### Player Can't Use `/duel`

**Check:**
```bash
# Check on server
/lp user <player> permission check epicduels.duel
```

If `false`:
```bash
/lp user <player> permission set epicduels.duel true
```

### Player Can't View Stats

Check `epicduels.stats` permission:
```bash
/lp user <player> permission set epicduels.stats true
```

### Admin Commands Don't Work

Make sure the admin has `epicduels.admin`:
```bash
/lp user <admin> permission set epicduels.admin true
```

### Wildcard Permissions

If using a permission system with wildcards:

```bash
# All EpicDuels Permissions
epicduels.*

# All admin permissions
epicduels.admin
```

---

## Best Practices

✅ **Recommended:**
- 🔐 Use a permission manager like LuckPerms
- 👥 Create groups (Players, VIP, Admin)
- 📋 Document your permissions
- ✓ Test permissions after changes

❌ **Not Recommended:**
- Making everyone OP
- Hardcoding permissions in bukkit.yml
- No backup of permission config

---

## Next Steps

- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
- ⚙️ **Configuration:** [Configuration Guide](./07-Configuration.md)
- 🎮 **Playing:** [Player Guide](./03-Player-Guide.md)
