package dev.epicduels.command;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
import dev.epicduels.model.Party;
import net.kyori.adventure.text.Component;
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
            Messages.send(sender, "general.players-only");
            return true;
        }
        if (!player.hasPermission("epicduels.party")) {
            Messages.send(player, "general.no-permission");
            return true;
        }
        if (!plugin.isFeatureEnabled("parties")) {
            Messages.send(player, "general.feature-disabled");
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
            Messages.send(player, "party.usage-invite");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Messages.send(player, "general.player-not-found");
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
                Messages.send(player, "general.player-not-found-simple");
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
                Messages.send(player, "general.player-not-found-simple");
                return;
            }
            from = sender.getUniqueId();
        }
        plugin.getPartyManager().denyInvite(player, from);
    }

    private void handleList(Player player) {
        Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
        if (party == null) {
            Messages.send(player, "party.not-in-party");
            return;
        }
        player.sendMessage(Component.empty());
        Messages.send(player, "party.list-header",
                Messages.unparsed("count", party.size()), Messages.unparsed("max", Party.MAX_SIZE));
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name == null) name = id.toString().substring(0, 8);
            Messages.send(player, party.isOwner(id) ? "party.list-owner" : "party.list-member",
                    Messages.unparsed("name", name));
        }
        player.sendMessage(Component.empty());
    }

    private void handleStart(Player player) {
        Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
        if (party == null) {
            Messages.send(player, "party.not-in-party");
            return;
        }
        if (!party.isOwner(player.getUniqueId())) {
            Messages.send(player, "party.owner-only-start");
            return;
        }
        if (party.size() < Party.MIN_SIZE) {
            Messages.send(player, "party.need-min", Messages.unparsed("count", Party.MIN_SIZE));
            return;
        }
        for (UUID id : party.getMembers()) {
            if (plugin.getDuelManager().isInDuel(id)
                    || plugin.getTeamDuelManager().isInTeamDuel(id)
                    || plugin.getTournamentManager().isInTournament(id)) {
                Messages.send(player, "party.member-busy");
                return;
            }
        }
        plugin.getGUIManager().openPartyModeMenu(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        Messages.send(player, "party.help-header");
        Messages.send(player, "party.help-create");
        Messages.send(player, "party.help-invite");
        Messages.send(player, "party.help-accept");
        Messages.send(player, "party.help-deny");
        Messages.send(player, "party.help-leave");
        Messages.send(player, "party.help-disband");
        Messages.send(player, "party.help-list");
        Messages.send(player, "party.help-start");
        player.sendMessage(Component.empty());
    }
}
