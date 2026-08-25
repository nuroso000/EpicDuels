package dev.epicduels.command;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelRequest;
import dev.epicduels.model.Kit;
import dev.epicduels.model.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelCommand implements CommandExecutor {

    private static final long FORFEIT_CONFIRM_MILLIS = 10_000;

    private final EpicDuels plugin;
    // Pending /duel forfeit confirmations: player -> time the prompt was shown
    private final Map<UUID, Long> forfeitConfirmations = new HashMap<>();

    public DuelCommand(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "general.players-only");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            plugin.getGUIManager().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "arena" -> handleArena(player, args);
            case "setlobby" -> handleSetLobby(player);
            case "kit" -> handleKit(player, args);
            case "challenge", "c" -> {
                if (requireFeature(player, "challenges")) handleChallenge(player, args);
            }
            case "accept" -> {
                if (requireFeature(player, "challenges")) handleAccept(player, args);
            }
            case "deny" -> handleDeny(player, args);
            case "cancel" -> handleCancel(player);
            case "stats" -> handleStats(player, args);
            case "queue", "q" -> {
                if (requireFeature(player, "matchmaking")) handleQueue(player, args);
            }
            case "spectate", "spec" -> {
                if (requireFeature(player, "spectating")) handleSpectate(player, args);
            }
            case "forfeit", "ff", "leave" -> {
                if (requireFeature(player, "forfeit")) handleForfeit(player, args);
            }
            case "rematch" -> {
                if (requireFeature(player, "rematch")) handleRematch(player);
            }
            case "kits", "editkit" -> {
                if (requireFeature(player, "kit-editor")) handleKitEditor(player, args);
            }
            case "toggle" -> handleToggle(player);
            case "reload" -> handleReload(player);
            case "duels" -> {
                if (!player.hasPermission("epicduels.duel")) {
                    Messages.send(player, "general.no-permission");
                    return true;
                }
                if (requireFeature(player, "challenges")) plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            case "matchmaking", "mm" -> {
                if (!player.hasPermission("epicduels.duel")) {
                    Messages.send(player, "general.no-permission");
                    return true;
                }
                if (requireFeature(player, "matchmaking")) plugin.getGUIManager().openMatchmakingMenu(player, 0);
            }
            case "leaderboard", "lb", "top" -> {
                if (requireFeature(player, "leaderboards")) handleLeaderboard(player, args);
            }
            case "lobby" -> handleLobby(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    /**
     * True if the feature is enabled; otherwise tells the player it is
     * disabled and returns false.
     */
    private boolean requireFeature(Player player, String feature) {
        if (plugin.isFeatureEnabled(feature)) return true;
        Messages.send(player, "general.feature-disabled");
        return false;
    }

    private void handleArena(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            Messages.send(player, "admin.arena-usage");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "rename" -> {
                if (args.length < 4) {
                    Messages.send(player, "admin.arena-rename-usage");
                    return;
                }
                if (isArenaInUse(args[2])) {
                    Messages.send(player, "admin.arena-in-use", Messages.unparsed("arena", args[2]));
                    return;
                }
                String error = plugin.getArenaManager().renameArena(args[2], args[3]);
                if (error != null) {
                    player.sendMessage(Component.text(error, NamedTextColor.RED));
                } else {
                    Messages.send(player, "admin.arena-renamed",
                            Messages.unparsed("old", args[2]), Messages.unparsed("new", args[3]));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.arena-create-usage");
                    return;
                }
                String name = args[2];
                // Same rule as rename — the name becomes part of a world
                // folder path, so anything else is a path-traversal risk
                if (!name.matches("[A-Za-z0-9_-]+")) {
                    Messages.send(player, "admin.invalid-name");
                    return;
                }
                Arena arena = plugin.getArenaManager().createArena(name);
                if (arena == null) {
                    Messages.send(player, "admin.arena-exists", Messages.unparsed("arena", name));
                    return;
                }
                org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    player.teleport(new Location(world, 0.5, 65, 0.5));
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    Messages.send(player, "admin.arena-created", Messages.unparsed("arena", name));
                    Messages.send(player, "admin.arena-created-hint");
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.arena-delete-usage");
                    return;
                }
                if (isArenaInUse(args[2])) {
                    Messages.send(player, "admin.arena-in-use", Messages.unparsed("arena", args[2]));
                    return;
                }
                if (plugin.getArenaManager().deleteArena(args[2])) {
                    Messages.send(player, "admin.arena-deleted", Messages.unparsed("arena", args[2]));
                } else {
                    Messages.send(player, "admin.arena-not-found");
                }
            }
            case "setspawn1" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    Messages.send(player, "admin.not-in-arena-world");
                    return;
                }
                arena.setSpawn1(player.getLocation().clone());
                plugin.getArenaManager().saveArenas();
                Messages.send(player, "admin.spawn1-set", Messages.unparsed("arena", arena.getName()));
            }
            case "setspawn2" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    Messages.send(player, "admin.not-in-arena-world");
                    return;
                }
                arena.setSpawn2(player.getLocation().clone());
                plugin.getArenaManager().saveArenas();
                Messages.send(player, "admin.spawn2-set", Messages.unparsed("arena", arena.getName()));
            }
            case "save" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    Messages.send(player, "admin.not-in-arena-world");
                    return;
                }
                if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
                    Messages.send(player, "admin.set-spawns-first");
                    return;
                }
                arena.setReady(true);
                plugin.getArenaManager().saveArenas();
                player.teleport(plugin.getLobbyLocation());
                Messages.send(player, "admin.arena-saved", Messages.unparsed("arena", arena.getName()));
            }
            case "list" -> {
                Messages.send(player, "admin.arena-list-header");
                for (Arena arena : plugin.getArenaManager().getAllArenas()) {
                    Messages.send(player, arena.isReady() ? "admin.arena-list-ready" : "admin.arena-list-incomplete",
                            Messages.unparsed("arena", arena.getName()));
                }
                if (plugin.getArenaManager().getAllArenas().isEmpty()) {
                    Messages.send(player, "admin.arena-list-empty");
                }
            }
            case "tp" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.arena-tp-usage");
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) {
                    Messages.send(player, "admin.arena-not-found");
                    return;
                }
                plugin.getArenaManager().ensureArenaWorldLoaded(arena);
                org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    player.teleport(new Location(world, 0.5, 65, 0.5));
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    Messages.send(player, "admin.arena-tp", Messages.unparsed("arena", arena.getName()));
                }
            }
            case "seticon" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.arena-seticon-usage");
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) {
                    Messages.send(player, "admin.arena-not-found");
                    return;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    Messages.send(player, "admin.hold-item");
                    return;
                }
                arena.setIcon(hand.getType());
                plugin.getArenaManager().saveArenas();
                Messages.send(player, "admin.arena-icon-set",
                        Messages.unparsed("arena", arena.getName()), Messages.unparsed("icon", hand.getType().name()));
            }
            default -> Messages.send(player, "admin.arena-unknown-action");
        }
    }

    private boolean isArenaInUse(String arenaName) {
        return plugin.getDuelManager().isArenaInUse(arenaName)
                || plugin.getTeamDuelManager().isArenaInUse(arenaName);
    }

    private Arena getArenaFromWorld(Player player) {
        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("arena_template_")) return null;
        String arenaName = worldName.substring("arena_template_".length());
        return plugin.getArenaManager().getArena(arenaName);
    }

    private void handleSetLobby(Player player) {
        if (!player.hasPermission("epicduels.admin")) {
            Messages.send(player, "general.no-permission");
            return;
        }
        plugin.setLobbyLocation(player.getLocation());
        Messages.send(player, "admin.lobby-set");
    }

    private void handleKit(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            Messages.send(player, "admin.kit-usage");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "give", "copy", "load" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-give-usage");
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    Messages.send(player, "general.kit-not-found");
                    return;
                }
                // Clear current inventory first
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);

                // Deep-copy so edits to the player's inventory don't mutate the kit
                ItemStack[] src = kit.getContents();
                ItemStack[] copy = new ItemStack[src.length];
                for (int i = 0; i < src.length; i++) {
                    copy[i] = src[i] != null ? src[i].clone() : null;
                }
                player.getInventory().setContents(copy);

                if (kit.getArmorContents() != null) {
                    ItemStack[] armorSrc = kit.getArmorContents();
                    ItemStack[] armorCopy = new ItemStack[armorSrc.length];
                    for (int i = 0; i < armorSrc.length; i++) {
                        armorCopy[i] = armorSrc[i] != null ? armorSrc[i].clone() : null;
                    }
                    player.getInventory().setArmorContents(armorCopy);
                }
                if (kit.getOffHand() != null) {
                    player.getInventory().setItemInOffHand(kit.getOffHand().clone());
                }
                Messages.send(player, "admin.kit-copied", Messages.unparsed("kit", kit.getName()));
            }
            case "rename" -> {
                if (args.length < 4) {
                    Messages.send(player, "admin.kit-rename-usage");
                    return;
                }
                String error = plugin.getKitManager().renameKit(args[2], args[3]);
                if (error != null) {
                    player.sendMessage(Component.text(error, NamedTextColor.RED));
                } else {
                    Messages.send(player, "admin.kit-renamed",
                            Messages.unparsed("old", args[2]), Messages.unparsed("new", args[3]));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-create-usage");
                    return;
                }
                String name = args[2];
                // Same rule as rename — the name is used as a YAML path in
                // kits.yml, so dots or other special characters corrupt it
                if (!name.matches("[A-Za-z0-9_-]+")) {
                    Messages.send(player, "admin.invalid-name");
                    return;
                }
                ItemStack[] contents = player.getInventory().getStorageContents();
                ItemStack[] armor = player.getInventory().getArmorContents();
                ItemStack offHand = player.getInventory().getItemInOffHand();

                ItemStack[] clonedContents = new ItemStack[contents.length];
                for (int i = 0; i < contents.length; i++) {
                    clonedContents[i] = contents[i] != null ? contents[i].clone() : null;
                }
                ItemStack[] clonedArmor = new ItemStack[armor.length];
                for (int i = 0; i < armor.length; i++) {
                    clonedArmor[i] = armor[i] != null ? armor[i].clone() : null;
                }
                ItemStack clonedOffHand = offHand.getType() != Material.AIR ? offHand.clone() : null;

                Kit kit = plugin.getKitManager().createKit(name, clonedContents, clonedArmor, clonedOffHand);
                if (kit == null) {
                    Messages.send(player, "admin.kit-exists", Messages.unparsed("kit", name));
                } else {
                    Messages.send(player, "admin.kit-created", Messages.unparsed("kit", name));
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-delete-usage");
                    return;
                }
                if (plugin.getKitManager().deleteKit(args[2])) {
                    Messages.send(player, "admin.kit-deleted", Messages.unparsed("kit", args[2]));
                } else {
                    Messages.send(player, "general.kit-not-found");
                }
            }
            case "list" -> {
                Messages.send(player, "admin.kit-list-header");
                for (Kit kit : plugin.getKitManager().getAllKits()) {
                    Messages.send(player, "admin.kit-list-entry", Messages.unparsed("kit", kit.getName()));
                }
                if (plugin.getKitManager().getAllKits().isEmpty()) {
                    Messages.send(player, "admin.kit-list-empty");
                }
            }
            case "edit" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-edit-usage");
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    Messages.send(player, "general.kit-not-found");
                    return;
                }
                plugin.getGUIManager().openKitEdit(player, kit);
            }
            case "preview" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-preview-usage");
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    Messages.send(player, "general.kit-not-found");
                    return;
                }
                plugin.getGUIManager().openKitPreview(player, kit);
            }
            case "seticon" -> {
                if (args.length < 3) {
                    Messages.send(player, "admin.kit-seticon-usage");
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    Messages.send(player, "general.kit-not-found");
                    return;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    Messages.send(player, "admin.hold-item");
                    return;
                }
                kit.setIcon(hand.getType());
                plugin.getKitManager().updateKit(kit);
                Messages.send(player, "admin.kit-icon-set",
                        Messages.unparsed("kit", kit.getName()), Messages.unparsed("icon", hand.getType().name()));
            }
            default -> Messages.send(player, "admin.kit-unknown-action");
        }
    }

    private void handleKitEditor(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        // /duel kits <kit> jumps straight into the customize GUI
        if (args.length >= 2) {
            Kit kit = plugin.getKitManager().getKit(args[1]);
            if (kit == null) {
                Messages.send(player, "general.kit-not-found");
                return;
            }
            plugin.getGUIManager().openKitCustomize(player, kit);
            return;
        }

        plugin.getGUIManager().openKitEditorList(player, 0);
    }

    private void handleChallenge(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "general.already-in-match");
            return;
        }

        if (args.length < 2) {
            plugin.getGUIManager().openDuelsMenu(player, 0);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Messages.send(player, "general.player-not-found");
            return;
        }

        if (target.equals(player)) {
            Messages.send(player, "challenge.self");
            return;
        }

        if (plugin.getDuelManager().isBusy(target.getUniqueId())) {
            Messages.send(player, "general.target-in-match");
            return;
        }

        if (plugin.getDuelManager().hasRequestsDisabled(target.getUniqueId())) {
            Messages.send(player, "challenge.requests-disabled");
            return;
        }

        // Open kit selection first (new flow: player -> kit -> map)
        plugin.getGUIManager().openKitSelect(player, target.getUniqueId());
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        DuelRequest request;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                Messages.send(player, "general.player-not-found-simple");
                return;
            }
            request = plugin.getDuelManager().getIncomingRequestFrom(player.getUniqueId(), sender.getUniqueId());
            if (request == null) {
                Messages.send(player, "challenge.no-request-from");
                return;
            }
        } else {
            var requests = plugin.getDuelManager().getIncomingRequests(player.getUniqueId());
            if (requests.isEmpty()) {
                Messages.send(player, "challenge.no-requests");
                return;
            }
            if (requests.size() > 1) {
                Messages.send(player, "challenge.multiple-accept");
                return;
            }
            request = requests.get(0);
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null) {
            Messages.send(player, "challenge.challenger-offline");
            plugin.getDuelManager().denyRequest(player.getUniqueId(), request.getSender());
            return;
        }

        Messages.send(player, "challenge.accepted");
        Messages.send(sender, "challenge.accepted-sender", Messages.unparsed("player", player.getName()));
        plugin.getDuelManager().acceptRequest(player.getUniqueId(), request.getSender());
    }

    private void handleDeny(Player player, String[] args) {
        DuelRequest request;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                Messages.send(player, "general.player-not-found-simple");
                return;
            }
            request = plugin.getDuelManager().getIncomingRequestFrom(player.getUniqueId(), sender.getUniqueId());
            if (request == null) {
                Messages.send(player, "challenge.no-request-from");
                return;
            }
        } else {
            var requests = plugin.getDuelManager().getIncomingRequests(player.getUniqueId());
            if (requests.isEmpty()) {
                Messages.send(player, "challenge.no-requests");
                return;
            }
            if (requests.size() > 1) {
                Messages.send(player, "challenge.multiple-deny");
                return;
            }
            request = requests.get(0);
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        plugin.getDuelManager().denyRequest(player.getUniqueId(), request.getSender());
        Messages.send(player, "challenge.denied");
        if (sender != null) {
            Messages.send(sender, "challenge.denied-sender", Messages.unparsed("player", player.getName()));
        }
    }

    private void handleCancel(Player player) {
        DuelRequest request = plugin.getDuelManager().getOutgoingRequest(player.getUniqueId());
        if (request == null) {
            Messages.send(player, "challenge.no-outgoing");
            return;
        }

        plugin.getDuelManager().cancelRequest(player.getUniqueId());
        Messages.send(player, "challenge.cancelled");

        Player receiver = Bukkit.getPlayer(request.getReceiver());
        if (receiver != null) {
            Messages.send(receiver, "challenge.cancelled-receiver", Messages.unparsed("player", player.getName()));
        }
    }

    private void handleQueue(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        boolean wasInQueue = plugin.getQueueManager().isInQueue(player.getUniqueId());

        // No kit argument, or explicit "leave" -> just leave the current queue.
        if (args.length < 2 || args[1].equalsIgnoreCase("leave")) {
            if (wasInQueue) {
                plugin.getQueueManager().leaveQueue(player.getUniqueId());
                Messages.send(player, "queue.left");
                player.sendActionBar(Component.empty());
            } else if (args.length < 2) {
                Messages.send(player, "queue.usage");
            } else {
                Messages.send(player, "queue.not-in-queue");
            }
            return;
        }

        // Kit argument given -> join that queue (switching from any current queue).
        String kitName = args[1];
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            Messages.send(player, "general.kit-not-found");
            return;
        }

        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "general.already-in-match");
            return;
        }

        if (wasInQueue) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
        }

        boolean joined = plugin.getQueueManager().joinQueue(player.getUniqueId(), kit.getName());
        if (joined) {
            Messages.send(player, wasInQueue ? "queue.switched" : "queue.joined",
                    Messages.unparsed("kit", kit.getName()));
        } else {
            Messages.send(player, "queue.join-failed");
        }
    }

    private void handleSpectate(Player player, String[] args) {
        if (!player.hasPermission("epicduels.spectate")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "spectate.while-in-match");
            return;
        }

        // If already spectating, leave
        if (plugin.getDuelManager().isSpectating(player.getUniqueId())) {
            plugin.getDuelManager().removeSpectator(player.getUniqueId());
            return;
        }

        // No target given — open the live-duels browser GUI
        if (args.length < 2) {
            plugin.getGUIManager().openSpectateMenu(player, 0);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            Messages.send(player, "general.player-not-found");
            return;
        }

        var duel = plugin.getDuelManager().getDuel(target.getUniqueId());
        if (duel == null || !duel.isActive()) {
            Messages.send(player, "spectate.not-in-duel", Messages.unparsed("player", target.getName()));
            return;
        }

        if (duel.getInstanceWorld() == null) {
            Messages.send(player, "spectate.arena-not-ready");
            return;
        }

        plugin.getDuelManager().addSpectator(player, duel);
    }

    private void handleForfeit(Player player, String[] args) {
        UUID id = player.getUniqueId();
        boolean inDuel = plugin.getDuelManager().isInDuel(id);
        boolean inTeamDuel = !inDuel
                && plugin.getTeamDuelManager().isInTeamDuel(id)
                && plugin.getTeamDuelManager().getTeamDuelOf(id).isAlive(id);

        if (!inDuel && !inTeamDuel) {
            forfeitConfirmations.remove(id);
            Messages.send(player, "forfeit.not-in-duel");
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            Long promptedAt = forfeitConfirmations.remove(id);
            if (promptedAt == null || System.currentTimeMillis() - promptedAt > FORFEIT_CONFIRM_MILLIS) {
                Messages.send(player, "forfeit.confirm-expired");
                return;
            }
            if (inDuel) {
                plugin.getDuelManager().forfeitDuel(id);
            } else {
                plugin.getTeamDuelManager().forfeit(player);
            }
            return;
        }

        forfeitConfirmations.put(id, System.currentTimeMillis());
        Messages.send(player, "forfeit.warning");
        Messages.send(player, "forfeit.confirm-hint");
    }

    private void handleRematch(Player player) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }
        plugin.getRematchManager().accept(player);
    }

    private void handleToggle(Player player) {
        if (!player.hasPermission("epicduels.duel")) {
            Messages.send(player, "general.no-permission");
            return;
        }
        boolean disabled = plugin.getDuelManager().toggleRequests(player.getUniqueId());
        Messages.send(player, disabled ? "toggle.disabled" : "toggle.enabled");
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("epicduels.admin")) {
            Messages.send(player, "general.no-permission");
            return;
        }
        plugin.reloadConfig();
        Messages.reload();
        Messages.send(player, "general.config-reloaded");
        Messages.send(player, "general.config-reloaded-note");
    }

    private void handleLeaderboard(Player player, String[] args) {
        if (!player.hasPermission("epicduels.stats")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        // Default: show wins leaderboard
        String sub = args.length >= 2 ? args[1].toLowerCase() : "wins";

        if (sub.equals("wins") || sub.equals("score")) {
            printLeaderboard(player, sub);
            return;
        }

        // Admin: /duel leaderboard sethologram <wins|score>
        if (sub.equals("sethologram") || sub.equals("sethologramm")) {
            if (!player.hasPermission("epicduels.admin")) {
                Messages.send(player, "general.no-permission");
                return;
            }
            if (args.length < 3) {
                Messages.send(player, "leaderboard.sethologram-usage");
                return;
            }
            dev.epicduels.manager.HologramManager.Type type = parseType(args[2]);
            if (type == null) {
                Messages.send(player, "leaderboard.type-invalid");
                return;
            }
            plugin.getHologramManager().setHologram(type, player.getLocation().clone().add(0, 2, 0));
            Messages.send(player, "leaderboard.hologram-placed", Messages.unparsed("type", type.name()));
            return;
        }

        if (sub.equals("removehologram") || sub.equals("delhologram")) {
            if (!player.hasPermission("epicduels.admin")) {
                Messages.send(player, "general.no-permission");
                return;
            }
            if (args.length < 3) {
                Messages.send(player, "leaderboard.removehologram-usage");
                return;
            }
            dev.epicduels.manager.HologramManager.Type type = parseType(args[2]);
            if (type == null) {
                Messages.send(player, "leaderboard.type-invalid");
                return;
            }
            if (plugin.getHologramManager().removeHologram(type)) {
                Messages.send(player, "leaderboard.hologram-removed", Messages.unparsed("type", type.name()));
            } else {
                Messages.send(player, "leaderboard.hologram-missing", Messages.unparsed("type", type.name()));
            }
            return;
        }

        Messages.send(player, "leaderboard.usage");
    }

    private dev.epicduels.manager.HologramManager.Type parseType(String s) {
        return switch (s.toLowerCase()) {
            case "wins", "win" -> dev.epicduels.manager.HologramManager.Type.WINS;
            case "score" -> dev.epicduels.manager.HologramManager.Type.SCORE;
            default -> null;
        };
    }

    private void printLeaderboard(Player player, String type) {
        boolean wins = type.equals("wins");
        var entries = wins
                ? plugin.getStatsManager().getTopByWins(10)
                : plugin.getStatsManager().getTopByScore(10);

        player.sendMessage(Component.empty());
        Messages.send(player, wins ? "leaderboard.header-wins" : "leaderboard.header-score");
        if (entries.isEmpty()) {
            Messages.send(player, "leaderboard.empty");
            player.sendMessage(Component.empty());
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(entry.uuid);
            String name = op.getName() != null ? op.getName() : entry.uuid.toString().substring(0, 8);
            int value = wins ? entry.wins : entry.score;
            NamedTextColor rankColor = switch (i + 1) {
                case 1 -> NamedTextColor.GOLD;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.RED;
                default -> NamedTextColor.WHITE;
            };
            player.sendMessage(Messages.get("leaderboard.entry",
                    Messages.unparsed("rank", i + 1),
                    Messages.unparsed("name", name),
                    Messages.unparsed("value", value)).color(rankColor));
        }
        player.sendMessage(Component.empty());
    }

    private void handleLobby(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        var listener = plugin.getLobbyProtectionListener();

        if (args.length < 2) {
            boolean bypassing = listener.isBypassing(player.getUniqueId());
            Messages.send(player, bypassing ? "lobby.status-off" : "lobby.status-on");
            Messages.send(player, "lobby.usage");
            return;
        }

        String toggle = args[1].toLowerCase();
        if (toggle.equals("on")) {
            listener.setBypass(player.getUniqueId(), false);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            Messages.send(player, "lobby.enabled");
        } else if (toggle.equals("off")) {
            listener.setBypass(player.getUniqueId(), true);
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
            Messages.send(player, "lobby.disabled");
            Messages.send(player, "lobby.disabled-hint");
        } else {
            Messages.send(player, "lobby.usage");
        }
    }

    private void handleStats(Player player, String[] args) {
        if (!player.hasPermission("epicduels.stats")) {
            Messages.send(player, "general.no-permission");
            return;
        }

        // No argument → open GUI stats menu for own profile
        if (args.length < 2) {
            plugin.getGUIManager().openStatsMenu(player);
            return;
        }

        // With a player argument → show chat stats for that player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Messages.send(player, "general.player-not-found");
            return;
        }

        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());

        player.sendMessage(Component.empty());
        Messages.send(player, "stats.header", Messages.unparsed("player", target.getName()));
        Messages.send(player, "stats.wins", Messages.unparsed("value", stats.getWins()));
        Messages.send(player, "stats.losses", Messages.unparsed("value", stats.getLosses()));
        Messages.send(player, "stats.total", Messages.unparsed("value", stats.getTotalGames()));
        Messages.send(player, "stats.winrate",
                Messages.unparsed("value", String.format("%.1f", stats.getWinRate())));
        player.sendMessage(Component.empty());
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        Messages.send(player, "help.header");
        Messages.send(player, "help.menu");
        Messages.send(player, "help.duels");
        Messages.send(player, "help.matchmaking");
        Messages.send(player, "help.stats");
        Messages.send(player, "help.stats-player");
        Messages.send(player, "help.challenge");
        Messages.send(player, "help.accept");
        Messages.send(player, "help.deny");
        Messages.send(player, "help.cancel");
        Messages.send(player, "help.queue");
        Messages.send(player, "help.queue-leave");
        Messages.send(player, "help.spectate");
        Messages.send(player, "help.forfeit");
        Messages.send(player, "help.rematch");
        Messages.send(player, "help.kits");
        Messages.send(player, "help.toggle");
        Messages.send(player, "help.leaderboard");
        if (player.hasPermission("epicduels.admin")) {
            Messages.send(player, "help.admin-arena");
            Messages.send(player, "help.admin-kit");
            Messages.send(player, "help.admin-setlobby");
            Messages.send(player, "help.admin-lobby");
            Messages.send(player, "help.admin-reload");
        }
        player.sendMessage(Component.empty());
    }
}
