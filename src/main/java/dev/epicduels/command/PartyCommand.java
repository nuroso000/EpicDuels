package dev.epicduels.command;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    private final EpicDuels plugin;

    public PartyCommand(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("epicduels.party")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> plugin.getPartyManager().createParty(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny", "decline" -> handleDeny(player, args);
            case "leave" -> plugin.getPartyManager().leaveParty(player);
            case "disband" -> plugin.getPartyManager().disbandParty(player);
            case "list", "info" -> handleList(player);
            case "start" -> handleStart(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /party invite <player>", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return;
        }
        // Auto-create party if owner has none
        if (plugin.getPartyManager().getPartyOf(player.getUniqueId()) == null) {
            plugin.getPartyManager().createParty(player);
        }
        plugin.getPartyManager().invitePlayer(player, target);
    }

    private void handleAccept(Player player, String[] args) {
        UUID from = null;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            from = sender.getUniqueId();
        }
        plugin.getPartyManager().acceptInvite(player, from);
    }

    private void handleDeny(Player player, String[] args) {
        UUID from = null;
        if (args.length >= 2) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender == null) {
                player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            from = sender.getUniqueId();
        }
        plugin.getPartyManager().denyInvite(player, from);
    }

    private void handleList(Player player) {
        Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Party (" + party.size() + "/" + Party.MAX_SIZE + ") ===",
                NamedTextColor.GOLD, TextDecoration.BOLD));
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name == null) name = id.toString().substring(0, 8);
            NamedTextColor color = party.isOwner(id) ? NamedTextColor.GOLD : NamedTextColor.GRAY;
            String tag = party.isOwner(id) ? " [Owner]" : "";
            player.sendMessage(Component.text(" - " + name + tag, color));
        }
        player.sendMessage(Component.empty());
    }

    private void handleStart(Player player) {
        Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return;
        }
        if (!party.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the party owner can start.", NamedTextColor.RED));
            return;
        }
        if (party.size() < Party.MIN_SIZE) {
            player.sendMessage(Component.text("Need at least " + Party.MIN_SIZE + " players.", NamedTextColor.RED));
            return;
        }
        for (UUID id : party.getMembers()) {
            if (plugin.getDuelManager().isInDuel(id)
                    || plugin.getTeamDuelManager().isInTeamDuel(id)
                    || plugin.getTournamentManager().isInTournament(id)) {
                player.sendMessage(Component.text("A party member is already in a duel/tournament.",
                        NamedTextColor.RED));
                return;
            }
        }
        plugin.getGUIManager().openPartyModeMenu(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Party Help ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/party create", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a party", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party invite <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Invite a player", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party accept [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Accept invite", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party deny [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Deny invite", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party leave", NamedTextColor.YELLOW)
                .append(Component.text(" - Leave the party", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party disband", NamedTextColor.YELLOW)
                .append(Component.text(" - Disband (owner only)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party list", NamedTextColor.YELLOW)
                .append(Component.text(" - Show members", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/party start", NamedTextColor.YELLOW)
                .append(Component.text(" - Open mode select (owner)", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }
}
