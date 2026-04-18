package dev.epicduels.command;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelRequest;
import dev.epicduels.model.Kit;
import dev.epicduels.model.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DuelCommand implements CommandExecutor {

    private final EpicDuels plugin;

    public DuelCommand(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
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
            case "challenge", "c" -> handleChallenge(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "cancel" -> handleCancel(player);
            case "stats" -> handleStats(player, args);
            case "queue", "q" -> handleQueue(player, args);
            case "spectate", "spec" -> handleSpectate(player, args);
            case "duels" -> {
                if (!player.hasPermission("epicduels.duel")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            case "matchmaking", "mm" -> {
                if (!player.hasPermission("epicduels.duel")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGUIManager().openMatchmakingMenu(player, 0);
            }
            case "leaderboard", "lb", "top" -> handleLeaderboard(player, args);
            case "lobby" -> handleLobby(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleArena(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /duel arena <create|delete|rename|setspawn1|setspawn2|save|list|tp|seticon> [name]", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "rename" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /duel arena rename <oldname> <newname>", NamedTextColor.YELLOW));
                    return;
                }
                String error = plugin.getArenaManager().renameArena(args[2], args[3]);
                if (error != null) {
                    player.sendMessage(Component.text(error, NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Arena '" + args[2] + "' renamed to '" + args[3] + "'.", NamedTextColor.GREEN));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel arena create <name>", NamedTextColor.YELLOW));
                    return;
                }
                String name = args[2];
                Arena arena = plugin.getArenaManager().createArena(name);
                if (arena == null) {
                    player.sendMessage(Component.text("Arena '" + name + "' already exists!", NamedTextColor.RED));
                    return;
                }
                org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    player.teleport(new Location(world, 0.5, 65, 0.5));
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    player.sendMessage(Component.text("Arena '" + name + "' created! You are now in build mode.", NamedTextColor.GREEN));
                    player.sendMessage(Component.text("Use /duel arena setspawn1, setspawn2, then save when done.", NamedTextColor.GRAY));
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel arena delete <name>", NamedTextColor.YELLOW));
                    return;
                }
                if (plugin.getArenaManager().deleteArena(args[2])) {
                    player.sendMessage(Component.text("Arena '" + args[2] + "' deleted.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
                }
            }
            case "setspawn1" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    player.sendMessage(Component.text("You must be in an arena world!", NamedTextColor.RED));
                    return;
                }
                arena.setSpawn1(player.getLocation().clone());
                plugin.getArenaManager().saveArenas();
                player.sendMessage(Component.text("Spawn 1 set for arena '" + arena.getName() + "'.", NamedTextColor.GREEN));
            }
            case "setspawn2" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    player.sendMessage(Component.text("You must be in an arena world!", NamedTextColor.RED));
                    return;
                }
                arena.setSpawn2(player.getLocation().clone());
                plugin.getArenaManager().saveArenas();
                player.sendMessage(Component.text("Spawn 2 set for arena '" + arena.getName() + "'.", NamedTextColor.GREEN));
            }
            case "save" -> {
                Arena arena = getArenaFromWorld(player);
                if (arena == null) {
                    player.sendMessage(Component.text("You must be in an arena world!", NamedTextColor.RED));
                    return;
                }
                if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
                    player.sendMessage(Component.text("You must set both spawn points first!", NamedTextColor.RED));
                    return;
                }
                arena.setReady(true);
                plugin.getArenaManager().saveArenas();
                player.teleport(plugin.getLobbyLocation());
                player.sendMessage(Component.text("Arena '" + arena.getName() + "' saved and marked as ready!", NamedTextColor.GREEN));
            }
            case "list" -> {
                player.sendMessage(Component.text("=== Arenas ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (Arena arena : plugin.getArenaManager().getAllArenas()) {
                    NamedTextColor statusColor = arena.isReady() ? NamedTextColor.GREEN : NamedTextColor.RED;
                    String status = arena.isReady() ? "Ready" : "Incomplete";
                    player.sendMessage(Component.text(" - " + arena.getName() + " ", NamedTextColor.GRAY)
                            .append(Component.text("[" + status + "]", statusColor)));
                }
                if (plugin.getArenaManager().getAllArenas().isEmpty()) {
                    player.sendMessage(Component.text("  No arenas created yet.", NamedTextColor.GRAY));
                }
            }
            case "tp" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel arena tp <name>", NamedTextColor.YELLOW));
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) {
                    player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
                    return;
                }
                plugin.getArenaManager().ensureArenaWorldLoaded(arena);
                org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    player.teleport(new Location(world, 0.5, 65, 0.5));
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    player.sendMessage(Component.text("Teleported to arena '" + arena.getName() + "'.", NamedTextColor.GREEN));
                }
            }
            case "seticon" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel arena seticon <name> (hold item in hand)", NamedTextColor.YELLOW));
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(args[2]);
                if (arena == null) {
                    player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
                    return;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage(Component.text("Hold an item in your hand to use as the icon!", NamedTextColor.RED));
                    return;
                }
                arena.setIcon(hand.getType());
                plugin.getArenaManager().saveArenas();
                player.sendMessage(Component.text("Arena '" + arena.getName() + "' icon set to " + hand.getType().name() + "!", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Unknown arena action.", NamedTextColor.RED));
        }
    }

    private Arena getArenaFromWorld(Player player) {
        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("arena_template_")) return null;
        String arenaName = worldName.substring("arena_template_".length());
        return plugin.getArenaManager().getArena(arenaName);
    }

    private void handleSetLobby(Player player) {
        if (!player.hasPermission("epicduels.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.setLobbyLocation(player.getLocation());
        player.sendMessage(Component.text("Lobby spawn set!", NamedTextColor.GREEN));
    }

    private void handleKit(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /duel kit <create|delete|rename|give|list|edit|preview|seticon> [name]", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "give", "copy", "load" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit give <name>", NamedTextColor.YELLOW));
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Kit '" + kit.getName() + "' copied into your inventory.", NamedTextColor.GREEN));
            }
            case "rename" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /duel kit rename <oldname> <newname>", NamedTextColor.YELLOW));
                    return;
                }
                String error = plugin.getKitManager().renameKit(args[2], args[3]);
                if (error != null) {
                    player.sendMessage(Component.text(error, NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Kit '" + args[2] + "' renamed to '" + args[3] + "'.", NamedTextColor.GREEN));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit create <name>", NamedTextColor.YELLOW));
                    return;
                }
                String name = args[2];
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
                    player.sendMessage(Component.text("Kit '" + name + "' already exists!", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Kit '" + name + "' created from your current inventory!", NamedTextColor.GREEN));
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit delete <name>", NamedTextColor.YELLOW));
                    return;
                }
                if (plugin.getKitManager().deleteKit(args[2])) {
                    player.sendMessage(Component.text("Kit '" + args[2] + "' deleted.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                }
            }
            case "list" -> {
                player.sendMessage(Component.text("=== Kits ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (Kit kit : plugin.getKitManager().getAllKits()) {
                    player.sendMessage(Component.text(" - " + kit.getName(), NamedTextColor.GRAY));
                }
                if (plugin.getKitManager().getAllKits().isEmpty()) {
                    player.sendMessage(Component.text("  No kits created yet.", NamedTextColor.GRAY));
                }
            }
            case "edit" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit edit <name>", NamedTextColor.YELLOW));
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                    return;
                }
                plugin.getGUIManager().openKitEdit(player, kit);
            }
            case "preview" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit preview <name>", NamedTextColor.YELLOW));
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                    return;
                }
                plugin.getGUIManager().openKitPreview(player, kit);
            }
            case "seticon" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /duel kit seticon <name> (hold item in hand)", NamedTextColor.YELLOW));
                    return;
                }
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit == null) {
                    player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                    return;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage(Component.text("Hold an item in your hand to use as the icon!", NamedTextColor.RED));
                    return;
                }
                kit.setIcon(hand.getType());
                plugin.getKitManager().updateKit(kit);
                player.sendMessage(Component.text("Kit '" + kit.getName() + "' icon set to " + hand.getType().name() + "!", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Unknown kit action.", NamedTextColor.RED));
        }
    }

    private void handleChallenge(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            plugin.getGUIManager().openDuelsMenu(player, 0);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot duel yourself!", NamedTextColor.RED));
            return;
        }

        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            player.sendMessage(Component.text("That player is already in a duel!", NamedTextColor.RED));
            return;
        }

        // Open kit selection first (new flow: player -> kit -> map)
        plugin.getGUIManager().openKitSelect(player, target.getUniqueId());
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        DuelRequest request;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            request = plugin.getDuelManager().getIncomingRequestFrom(player.getUniqueId(), sender.getUniqueId());
        } else {
            request = plugin.getDuelManager().getIncomingRequest(player.getUniqueId());
        }

        if (request == null) {
            player.sendMessage(Component.text("You have no pending duel requests.", NamedTextColor.RED));
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null) {
            player.sendMessage(Component.text("The challenger is no longer online.", NamedTextColor.RED));
            plugin.getDuelManager().denyRequest(player.getUniqueId());
            return;
        }

        player.sendMessage(Component.text("Duel accepted!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(player.getName() + " accepted your duel!", NamedTextColor.GREEN));
        plugin.getDuelManager().acceptRequest(player.getUniqueId());
    }

    private void handleDeny(Player player, String[] args) {
        DuelRequest request;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            request = plugin.getDuelManager().getIncomingRequestFrom(player.getUniqueId(), sender.getUniqueId());
        } else {
            request = plugin.getDuelManager().getIncomingRequest(player.getUniqueId());
        }

        if (request == null) {
            player.sendMessage(Component.text("You have no pending duel requests.", NamedTextColor.RED));
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        plugin.getDuelManager().denyRequest(player.getUniqueId());
        player.sendMessage(Component.text("Duel request denied.", NamedTextColor.YELLOW));
        if (sender != null) {
            sender.sendMessage(Component.text(player.getName() + " denied your duel request.", NamedTextColor.RED));
        }
    }

    private void handleCancel(Player player) {
        DuelRequest request = plugin.getDuelManager().getOutgoingRequest(player.getUniqueId());
        if (request == null) {
            player.sendMessage(Component.text("You have no outgoing duel request.", NamedTextColor.RED));
            return;
        }

        plugin.getDuelManager().cancelRequest(player.getUniqueId());
        player.sendMessage(Component.text("Duel request cancelled.", NamedTextColor.YELLOW));

        Player receiver = Bukkit.getPlayer(request.getReceiver());
        if (receiver != null) {
            receiver.sendMessage(Component.text(player.getName() + " cancelled the duel request.", NamedTextColor.GRAY));
        }
    }

    private void handleQueue(Player player, String[] args) {
        if (!player.hasPermission("epicduels.duel")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
            player.sendMessage(Component.text("You left the queue.", NamedTextColor.YELLOW));
            player.sendActionBar(Component.empty());
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /duel queue <kit> or /duel queue leave", NamedTextColor.YELLOW));
            return;
        }

        if (args[1].equalsIgnoreCase("leave")) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
            player.sendMessage(Component.text("You left the queue.", NamedTextColor.YELLOW));
            player.sendActionBar(Component.empty());
            return;
        }

        String kitName = args[1];
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
            return;
        }

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }

        boolean joined = plugin.getQueueManager().joinQueue(player.getUniqueId(), kit.getName());
        if (joined) {
            player.sendMessage(Component.text("You joined the queue for: " + kit.getName(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Could not join queue.", NamedTextColor.RED));
        }
    }

    private void handleSpectate(Player player, String[] args) {
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(Component.text("You can't spectate while in a duel!", NamedTextColor.RED));
            return;
        }

        // If already spectating, leave
        if (plugin.getDuelManager().isSpectating(player.getUniqueId())) {
            plugin.getDuelManager().removeSpectator(player.getUniqueId());
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /duel spectate <player>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return;
        }

        var duel = plugin.getDuelManager().getDuel(target.getUniqueId());
        if (duel == null || !duel.isActive()) {
            player.sendMessage(Component.text(target.getName() + " is not in a duel.", NamedTextColor.RED));
            return;
        }

        if (duel.getInstanceWorld() == null) {
            player.sendMessage(Component.text("Duel arena is not ready yet.", NamedTextColor.RED));
            return;
        }

        plugin.getDuelManager().addSpectator(player, duel);
    }

    private void handleLeaderboard(Player player, String[] args) {
        if (!player.hasPermission("epicduels.stats")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /duel leaderboard sethologram <wins|score>", NamedTextColor.YELLOW));
                return;
            }
            dev.epicduels.manager.HologramManager.Type type = parseType(args[2]);
            if (type == null) {
                player.sendMessage(Component.text("Type must be 'wins' or 'score'.", NamedTextColor.RED));
                return;
            }
            plugin.getHologramManager().setHologram(type, player.getLocation().clone().add(0, 2, 0));
            player.sendMessage(Component.text(type.name() + " leaderboard hologram placed at your location.", NamedTextColor.GREEN));
            return;
        }

        if (sub.equals("removehologram") || sub.equals("delhologram")) {
            if (!player.hasPermission("epicduels.admin")) {
                player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /duel leaderboard removehologram <wins|score>", NamedTextColor.YELLOW));
                return;
            }
            dev.epicduels.manager.HologramManager.Type type = parseType(args[2]);
            if (type == null) {
                player.sendMessage(Component.text("Type must be 'wins' or 'score'.", NamedTextColor.RED));
                return;
            }
            if (plugin.getHologramManager().removeHologram(type)) {
                player.sendMessage(Component.text(type.name() + " leaderboard hologram removed.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("No " + type.name() + " hologram was set.", NamedTextColor.RED));
            }
            return;
        }

        player.sendMessage(Component.text("Usage: /duel leaderboard <wins|score> [sethologram|removehologram <wins|score>]", NamedTextColor.YELLOW));
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
        player.sendMessage(Component.text("=== Top " + (wins ? "Wins" : "Score") + " ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        if (entries.isEmpty()) {
            player.sendMessage(Component.text("  No data yet.", NamedTextColor.GRAY));
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
            player.sendMessage(Component.text("#" + (i + 1) + " ", rankColor, TextDecoration.BOLD)
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text(" — ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(value), NamedTextColor.YELLOW)));
        }
        player.sendMessage(Component.empty());
    }

    private void handleLobby(Player player, String[] args) {
        if (!player.hasPermission("epicduels.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        var listener = plugin.getLobbyProtectionListener();

        if (args.length < 2) {
            boolean bypassing = listener.isBypassing(player.getUniqueId());
            player.sendMessage(Component.text("Lobby protections for you: ", NamedTextColor.GRAY)
                    .append(Component.text(bypassing ? "OFF" : "ON", bypassing ? NamedTextColor.RED : NamedTextColor.GREEN, TextDecoration.BOLD)));
            player.sendMessage(Component.text("Usage: /duel lobby <on|off>", NamedTextColor.YELLOW));
            return;
        }

        String toggle = args[1].toLowerCase();
        if (toggle.equals("on")) {
            listener.setBypass(player.getUniqueId(), false);
            player.sendMessage(Component.text("Lobby protections re-enabled for you.", NamedTextColor.GREEN));
        } else if (toggle.equals("off")) {
            listener.setBypass(player.getUniqueId(), true);
            player.sendMessage(Component.text("Lobby protections disabled for you.", NamedTextColor.RED));
            player.sendMessage(Component.text("You can now interact, pick up items, move inventory, etc.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Usage: /duel lobby <on|off>", NamedTextColor.YELLOW));
        }
    }

    private void handleStats(Player player, String[] args) {
        if (!player.hasPermission("epicduels.stats")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return;
        }

        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== " + target.getName() + "'s Stats ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text(" Wins: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getWins()), NamedTextColor.GREEN)));
        player.sendMessage(Component.text(" Losses: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getLosses()), NamedTextColor.RED)));
        player.sendMessage(Component.text(" Total: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getTotalGames()), NamedTextColor.AQUA)));
        player.sendMessage(Component.text(" Win Rate: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", stats.getWinRate()), NamedTextColor.GOLD)));
        player.sendMessage(Component.empty());
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== EpicDuels Help ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/duel", NamedTextColor.YELLOW).append(Component.text(" - Open main menu", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel duels", NamedTextColor.YELLOW).append(Component.text(" - Open duels/player select menu", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel matchmaking", NamedTextColor.YELLOW).append(Component.text(" - Open matchmaking queue menu", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel stats", NamedTextColor.YELLOW).append(Component.text(" - Open stats menu", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel stats <player>", NamedTextColor.YELLOW).append(Component.text(" - View another player's stats in chat", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel challenge <player>", NamedTextColor.YELLOW).append(Component.text(" - Challenge a player", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel accept [player]", NamedTextColor.YELLOW).append(Component.text(" - Accept a duel", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel deny [player]", NamedTextColor.YELLOW).append(Component.text(" - Deny a duel", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel cancel", NamedTextColor.YELLOW).append(Component.text(" - Cancel outgoing request", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel queue <kit>", NamedTextColor.YELLOW).append(Component.text(" - Join matchmaking queue", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel queue leave", NamedTextColor.YELLOW).append(Component.text(" - Leave the queue", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel spectate <player>", NamedTextColor.YELLOW).append(Component.text(" - Spectate a duel", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/duel leaderboard <wins|score>", NamedTextColor.YELLOW).append(Component.text(" - Show the top 10", NamedTextColor.GRAY)));
        if (player.hasPermission("epicduels.admin")) {
            player.sendMessage(Component.text("/duel arena <...>", NamedTextColor.YELLOW).append(Component.text(" - Arena management", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/duel kit <...>", NamedTextColor.YELLOW).append(Component.text(" - Kit management", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/duel setlobby", NamedTextColor.YELLOW).append(Component.text(" - Set lobby spawn", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/duel lobby <on|off>", NamedTextColor.YELLOW).append(Component.text(" - Toggle lobby protections", NamedTextColor.GRAY)));
        }
        player.sendMessage(Component.empty());
    }
}
