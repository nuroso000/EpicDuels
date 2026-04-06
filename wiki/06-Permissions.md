# Permissions Guide

Das Permission-System von EpicDuels.

## Permission-Liste

EpicDuels nutzt folgende Permissions:

| Permission | Beschreibung | Default | Befehl |
|---|---|---|---|
| `epicduels.admin` | Alle Admin-Befehle | OP | Arena & Kit Management |
| `epicduels.duel` | Challenges, Queue, Accept/Deny | Alle | `/duel` Befehle |
| `epicduels.stats` | Stats anschauen | Alle | `/duel stats` |

---

## Permission-Details

### epicduels.admin
**Gruppe:** Admin/Moderator
**Standard:** OP (Operator)
**Zugriff auf:**
- ✅ Alle Arena-Befehle (`/duel arena ...`)
- ✅ Alle Kit-Befehle (`/duel kit ...`)
- ✅ `/duel setlobby`

```yml
epicduels:
  admin: true
```

### epicduels.duel
**Gruppe:** Spieler
**Standard:** Alle
**Zugriff auf:**
- ✅ `/duel` (Menü)
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
**Gruppe:** Spieler
**Standard:** Alle
**Zugriff auf:**
- ✅ `/duel stats [player]`

```yml
epicduels:
  stats: true
```

---

## Permission-Management

### Mit LuckPerms (empfohlen)

**Spieler zur Gruppe hinzufügen:**
```bash
/lp user <player> parent add epicduels
```

**Admin-Permissions geben:**
```bash
/lp user <player> permission set epicduels.admin true
```

**Spieler-Permissions geben:**
```bash
/lp user <player> permission set epicduels.duel true
/lp user <player> permission set epicduels.stats true
```

**Gruppe mit Permission erstellen:**
```bash
/lp creategroup duelers
/lp group duelers permission set epicduels.duel true
/lp group duelers permission set epicduels.stats true
```

### Mit bukkit.yml

Bearbeite `bukkit.yml`:

```yaml
permissions:
  epicduels.*:
    description: Alle EpicDuels Permissions
    children:
      epicduels.admin: true
      epicduels.duel: true
      epicduels.stats: true
  
  epicduels.admin:
    description: Admin-Befehle
    default: op
  
  epicduels.duel:
    description: Duel-Befehle
    default: true
  
  epicduels.stats:
    description: Stats ansehen
    default: true
```

### Mit PEX (PermissionsEx)

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

## Scenario-Beispiele

### Szenario 1: Standard-Server

**Wunsch:** Alle können spielen, nur Admins können Arenen bauen

```bash
# Standard-Gruppe bekommen diese Permissions:
epicduels.duel: true
epicduels.stats: true

# Admin-Gruppe bekommen diese:
epicduels.admin: true
epicduels.duel: true
epicduels.stats: true
```

### Szenario 2: VIP-System

**Wunsch:** VIPs bekommen Zugang zu bestimmten Kits

**Hinweis:** EpicDuels hat kein eingebautes Kit-Permission-System. Du müsstest diese Features selbst implementieren oder einen anderen Ansatz nutzen.

### Szenario 3: Restricted Server

**Wunsch:** Nur bestimmte Spieler dürfen duellieren

```bash
# Standard-Spieler OHNE epicduels.duel
# Nur Whitelisted-Spieler BEKOMMEN epicduels.duel
/lp user Steve permission set epicduels.duel true
```

---

## Troubleshooting

### Spieler können `/duel` nicht benutzen

**Überprüfe:**
```bash
# Check im Server
/lp user <player> permission check epicduels.duel
```

Falls `false`:
```bash
/lp user <player> permission set epicduels.duel true
```

### Spieler können Stats nicht sehen

Überprüfe `epicduels.stats` Permission:
```bash
/lp user <player> permission set epicduels.stats true
```

### Admin-Befehle funktionieren nicht

Stelle sicher, dass der Admin `epicduels.admin` hat:
```bash
/lp user <admin> permission set epicduels.admin true
```

### Wildcard Permission

Falls du ein Permission-System mit Wildcards nutzt:

```bash
# Alle EpicDuels Permissions
epicduels.*

# Alle Admin-Permissions
epicduels.admin
```

---

## Best Practices

✅ **Empfohlen:**
- 🔐 Nutze ein Permission-Manager wie LuckPerms
- 👥 Erstelle Gruppen (Spieler, VIP, Admin)
- 📋 Dokumentiere deine Permissions
- ✓ Teste Permissions nach Änderungen

❌ **Nicht empfohlen:**
- Alle als OP setzen
- Permissions in bukkit.yml hardcoden
- Keine Backup der Permission-Konfiguration

---

## Nächste Schritte

- 🛠️ **Admin Setup:** [Admin Guide](./04-Admin-Guide.md)
- ⚙️ **Konfiguration:** [Configuration Guide](./07-Configuration.md)
- 🎮 **Spielen:** [Player Guide](./03-Player-Guide.md)
