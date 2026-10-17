# Presentation configuration (3.2.0)

`menus.yml` and `scoreboard.yml` are created on first startup. `/duel reload`
reloads both, updates online sidebars immediately, and uses new menu settings on
next open. Existing open menus retain their original action IDs, including after
reload. Malformed YAML retains the last loaded configuration; initial malformed
files fall back to original menus and no sidebar. Warnings identify the file and
setting. Missing menu properties preserve the built-in English content and feature
checks from `config.yml`. These files do not replace `lang/messages_*.yml`.

## Menus

All menu IDs and default sizes appear in `menus.yml`. Titles support MiniMessage;
`<kit>` is available for edit, customize, and preview titles. A 27-slot menu may
be expanded to 36, 45, or 54 slots. Pagination, preview, and kit editor menus
remain 54 slots. Existing logical content positions remain fixed.

Each button is identified by `slot-N`, where N is its **original** zero-based slot,
not its configured destination. This ID is stored in PDC and resolved with the
InventoryHolder. Titles, names, and materials never identify actions.

```yaml
menus:
  main:
    title: '<gold><bold>My Duels</bold></gold>'
    size: 36
    buttons:
      slot-10:
        slot: 28
        material: IRON_SWORD
        name: '<aqua>Challenge a player</aqua>'
        lore: ['<gray>Choose your opponent.</gray>', '', '<yellow>Click to open']
        visible: true
  kit-list:
    buttons:
      slot-10:
        name: '<gold><kit></gold>'
        lore: ['<gray>Preview <kit>.</gray>']
```

Supported button properties: `slot`, `material`, `name`, `lore`, `visible`.
Text placeholders: `<name>` (original plain display name), `<kit>` (kit PDC or
editor context), `<arena>` (arena PDC when present). Runtime values are unparsed
MiniMessage placeholders. Legacy ampersand colors are supported for templates
without MiniMessage tags. Omit name/lore to keep dynamic built-in details.

Content buttons in paginated 54-slot menus (slots 9..44) stay fixed to preserve
pagination capacity and the random-map animation. Navigation may move only
outside that region. Editor controls may move only within 41..53; actual kit items
0..40 cannot be styled, hidden, or moved by presentation configuration. Preview
items retain authentic equipment metadata; only its title is configurable.
Duplicate or invalid destinations revert the whole menu to original positions,
while valid text and material changes remain applied. Swaps between occupied
button positions are allowed. Invalid materials retain the original item.
Hidden buttons do not bypass server feature toggles or permissions. Settings for
empty slots do not create new actions. List entries are addressed by their visible
original position on each page; there is no per-kit/per-player layout registry.

## Scoreboard

```yaml
enabled: true
update-interval-ticks: 20
views:
  duel:
    enabled: true
    title: '<gold>Live Duel</gold>'
    lines:
      - '<white><player> <gray>vs <white><opponent>'
      - ''
      - '<gray>Score: <green><score> <gray>: <red><opponent_score>'
      - '<gray>Time: <white><time>'
```

Views: `lobby`, `duel`, `spectator`, `queue`. Priority: participant, spectator,
queue, lobby. Interval accepts 10..1200 ticks; invalid intervals use 20. At most
15 lines are displayed in configured order. Empty and duplicate lines are valid.
Each view has its own `enabled`, `title`, and `lines`. Missing lines disable that
view; missing title uses EpicDuels. Invalid text clears that player's sidebar.

Placeholders: `<player>`, `<opponent>`, `<kit>`, `<arena>`, `<round>`, `<best_of>`,
`<score>`, `<opponent_score>`, `<time>`. Time is remaining deadline time in m:ss;
`--:--` indicates countdown/no deadline. Outside a match, unavailable values are
`-`. Spectators see scores from player one's perspective. Team duels expose kit,
arena, time, and “Opposing team”; round and individual scores are unavailable (`-`).
Queue views expose player and kit; no invented estimated wait time is provided.
No PlaceholderAPI expansion is performed in these templates.

One main-thread scheduled task reads current manager state at the configured
interval, so match end, leave, and spectator changes are reflected within that
interval. Reload is immediate. Unchanged components are not rewritten. Disconnect
and disable release owned boards and restore the prior board when still owned.
EpicDuels owns the player's scoreboard while enabled: other sidebar plugins may
conflict; disable EpicDuels scoreboards when another plugin should own them.

## Design decisions and verification

A separate menu builder system was considered, but would duplicate challenge,
party, and kit flow logic. Central decoration preserves the existing holders,
domain PDC IDs, permissions, and data validation. The new button ID separates
logical actions from physical positions. Kit contents and pagination retain their
physical contracts. A dedicated sidebar manager uses real match deadlines rather
than recalculating duration from config and updates changed components only.
No new visual layout or website design was introduced.

For live Paper verification: open each menu, change titles/materials, move and swap
buttons, test paging and party/challenge flows, save/reset a kit, try duplicate and
out-of-range slots, reload while menus are open, finish/forfeit a match, leave a
queue or spectator session, disconnect, and disable the plugin. Confirm sidebar
restoration, blank lines, countdown, round transitions, and team-match placeholders.
These in-game checks require a running server and are not claimed by the build.

## Built-in button IDs

| Menu | Original slots and purpose |
| --- | --- |
| main | 4 spectate, 10 challenge, 13 stats, 16 matchmaking, 22 kit editor |
| stats | 4 profile, 10 wins, 12 losses, 14 overall, 16 score, 22 back |
| rounds-select | 11 Bo1, 13 Bo3, 15 Bo5, 22 back |
| party-mode | 11 team duel, 15 tournament |
| party-team-size | 11 2v2, 13 3v3, 15 4v4, 22 back |
| party-confirm | 11 confirm, 13 summary, 15 cancel |
| kit-edit | 53 save |
| kit-customize | 50 information, 51 reset, 53 save |
| Paginated menus | 45 previous, 49 back, 53 next |

Paginated entries occupy 10..16, 19..25, 28..34, and 37..43.
Their button properties apply to whichever real entry is shown there. Previous
and next buttons exist only when that page is available. Decorative panes are
not action buttons and are not covered by button overrides.

## Automated checks

`mvn clean verify` builds the Java 21 artifact against the Paper 1.21.1 API.
The same jar supports Paper 26.1+; those servers must run on Java 25. No JUnit
framework is introduced.
The standalone `src/test/java/dev/epicduels/PresentationChecks.java` checks layout
boundaries, duplicate destinations, swaps, blank lines, legacy colors, and literal
runtime placeholders. After compilation, run it with `target/classes`,
`target/test-classes`, and the resolved Adventure dependency JARs on the Java
classpath. It exits with an assertion failure on regression. These 15 assertions
were run successfully for this change; they do not simulate a Paper server.


## Inventory recovery and remote statistics upgrade

Every duel mode now saves the personal inventory before equipment changes. Failed snapshots abort the start before either inventory changes. Invalid snapshots remain in `inventories/` for operator recovery; never delete them without inspecting the player's saved items. Successful restoration saves player data and atomically consumes the recovery file. Files ending in `.consumed` are housekeeping leftovers and are never replayed.

When `lobby.handle-join: false`, recovery restores the saved location and game mode. Otherwise it resets combat effects and returns the player to the configured lobby. Failed return teleports keep the session protected and retry once per second while the plugin remains enabled. During an unresolved recovery, storage access, item drops outside active combat, and external commands are blocked. The `/duel forfeit` and `/d forfeit` confirmation flow, including its `confirm` step and supported aliases, remains available. Plugins that directly mutate inventories or dispatch commands from the console require their own integration with this boundary.

Automatic lobby world configuration is now opt-in with `lobby.configure-void-world: true` and requires the configured lobby to use EpicDuels' void generator. Existing survival worlds are not reconfigured. Fire destruction and pistons are disabled in battle instances to preserve the original arena across rounds.

Remote statistics no longer upload absolute cached totals. Supabase requires `database/supabase-results.sql` to be installed before restarting with the remote backend. Custom table names require matching event table and RPC names. Use a private server credential and keep RLS enabled; the supplied compatibility SQL grants access only to `service_role`. Prefer a dedicated server identity with narrower permissions when your deployment supports it. Firebase requires server authentication and restrictive rules; transactions use ETags to commit a counter and its event ID together. Do not delete result event IDs while a server could still replay its outbox.

Back up existing statistics before upgrading. Treat existing remote totals as the baseline. Old local cache totals are not automatically imported, because they may already be included remotely. If local-only history needs migration, reconcile it once with the remote baseline before enabling remote recording. Unacknowledged events survive in the `_pending` section of `stats.yml`; keep that section intact. Local display refreshes on access after at most 30 seconds, once pending writes are acknowledged. HTTP failures retain results for retry. A full filesystem cannot durably record new results; it is logged and prevents sending undurable events, while match cleanup continues.


## Running regression checks

Use `python3 scripts/checks.py` (`--offline` when dependencies are already cached). This compiles tests and then executes both standalone suites with Maven's actual dependency classpath. `mvn test` by itself only compiles these main-based suites; it does not execute their assertions. The bug suite binds an HTTP server exclusively to localhost and uses API doubles rather than a live Paper server. JDK 21 or newer, Maven, Python 3 and permission to bind localhost are required.
