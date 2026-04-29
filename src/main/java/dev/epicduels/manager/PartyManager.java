package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Party;
import dev.epicduels.model.PartyInvite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final EpicDuels plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToParty = new ConcurrentHashMap<>();
    // receiverId -> list of pending invites (one per sender)
    private final Map<UUID, Map<UUID, PartyInvite>> incomingInvites = new ConcurrentHashMap<>();
    private BukkitTask expirationTask;

    public PartyManager(EpicDuels plugin) {
        this.plugin = plugin;
        startExpirationTask();
    }

    private void startExpirationTask() {
        expirationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Map<UUID, PartyInvite>> entry : incomingInvites.entrySet()) {
                    Map<UUID, PartyInvite> map = entry.getValue();
                    Iterator<Map.Entry<UUID, PartyInvite>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        PartyInvite inv = it.next().getValue();
                        if (inv.isExpired()) {
                            it.remove();
                            Player receiver = Bukkit.getPlayer(inv.getReceiver());
                            Player sender = Bukkit.getPlayer(inv.getSender());
                            if (receiver != null) {
                                receiver.sendMessage(Component.text("A party invite expired.", NamedTextColor.GRAY));
                            }
                            if (sender != null) {
                                sender.sendMessage(Component.text("Your party invite expired.", NamedTextColor.GRAY));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Party createParty(Player owner) {
        if (playerToParty.containsKey(owner.getUniqueId())) {
            owner.sendMessage(Component.text("You are already in a party!", NamedTextColor.RED));
            return null;
        }
        Party party = new Party(owner.getUniqueId());
        parties.put(party.getId(), party);
        playerToParty.put(owner.getUniqueId(), party.getId());
        owner.sendMessage(Component.text("Party created! Invite players with /party invite <player>.", NamedTextColor.GREEN));
        owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        return party;
    }

    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    public Party getPartyOf(UUID playerId) {
        UUID pid = playerToParty.get(playerId);
        return pid == null ? null : parties.get(pid);
    }

    public boolean isInParty(UUID playerId) {
        return playerToParty.containsKey(playerId);
    }

    public boolean invitePlayer(Player owner, Player target) {
        Party party = getPartyOf(owner.getUniqueId());
        if (party == null) {
            owner.sendMessage(Component.text("You are not in a party. Use /party create first.", NamedTextColor.RED));
            return false;
        }
        if (!party.isOwner(owner.getUniqueId())) {
            owner.sendMessage(Component.text("Only the party owner can invite players.", NamedTextColor.RED));
            return false;
        }
        if (target.equals(owner)) {
            owner.sendMessage(Component.text("You cannot invite yourself.", NamedTextColor.RED));
            return false;
        }
        if (party.contains(target.getUniqueId())) {
            owner.sendMessage(Component.text(target.getName() + " is already in your party.", NamedTextColor.RED));
            return false;
        }
        if (party.isFull()) {
            owner.sendMessage(Component.text("Your party is full (max " + Party.MAX_SIZE + ").", NamedTextColor.RED));
            return false;
        }
        if (isInParty(target.getUniqueId())) {
            owner.sendMessage(Component.text(target.getName() + " is already in another party.", NamedTextColor.RED));
            return false;
        }

        PartyInvite invite = new PartyInvite(party.getId(), owner.getUniqueId(), target.getUniqueId());
        incomingInvites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(owner.getUniqueId(), invite);

        owner.sendMessage(Component.text("Invited " + target.getName() + " to your party.", NamedTextColor.GREEN));

        target.sendMessage(Component.empty());
        target.sendMessage(Component.text("=========================", NamedTextColor.GOLD));
        target.sendMessage(Component.text(owner.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" invited you to their party!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("[ACCEPT]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/party accept " + owner.getName()))
                .append(Component.text("  "))
                .append(Component.text("[DENY]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/party deny " + owner.getName()))));
        target.sendMessage(Component.text("Expires in 30 seconds.", NamedTextColor.GRAY));
        target.sendMessage(Component.text("=========================", NamedTextColor.GOLD));
        target.sendMessage(Component.empty());
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    public boolean acceptInvite(Player player, UUID fromSenderOrNull) {
        Map<UUID, PartyInvite> map = incomingInvites.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            player.sendMessage(Component.text("You have no pending party invites.", NamedTextColor.RED));
            return false;
        }
        PartyInvite invite;
        if (fromSenderOrNull != null) {
            invite = map.remove(fromSenderOrNull);
        } else if (map.size() == 1) {
            invite = map.values().iterator().next();
            map.remove(invite.getSender());
        } else {
            player.sendMessage(Component.text("You have multiple invites. Use /party accept <player>.", NamedTextColor.YELLOW));
            return false;
        }
        if (invite == null || invite.isExpired()) {
            player.sendMessage(Component.text("That invite has expired or does not exist.", NamedTextColor.RED));
            return false;
        }
        if (isInParty(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a party.", NamedTextColor.RED));
            return false;
        }
        Party party = parties.get(invite.getPartyId());
        if (party == null) {
            player.sendMessage(Component.text("That party no longer exists.", NamedTextColor.RED));
            return false;
        }
        if (party.isFull()) {
            player.sendMessage(Component.text("That party is full.", NamedTextColor.RED));
            return false;
        }
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party.getId());
        // Drop other pending invites for this player (joined a party)
        map.clear();

        party.messageAll(Component.text(player.getName() + " joined the party!", NamedTextColor.GREEN));
        return true;
    }

    public boolean denyInvite(Player player, UUID fromSenderOrNull) {
        Map<UUID, PartyInvite> map = incomingInvites.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            player.sendMessage(Component.text("You have no pending party invites.", NamedTextColor.RED));
            return false;
        }
        PartyInvite invite;
        if (fromSenderOrNull != null) {
            invite = map.remove(fromSenderOrNull);
        } else if (map.size() == 1) {
            invite = map.values().iterator().next();
            map.remove(invite.getSender());
        } else {
            player.sendMessage(Component.text("You have multiple invites. Use /party deny <player>.", NamedTextColor.YELLOW));
            return false;
        }
        if (invite == null) {
            player.sendMessage(Component.text("That invite does not exist.", NamedTextColor.RED));
            return false;
        }
        Player sender = Bukkit.getPlayer(invite.getSender());
        if (sender != null) {
            sender.sendMessage(Component.text(player.getName() + " denied your party invite.", NamedTextColor.RED));
        }
        player.sendMessage(Component.text("Invite denied.", NamedTextColor.YELLOW));
        return true;
    }

    public boolean leaveParty(Player player) {
        Party party = getPartyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return false;
        }
        return removeFromParty(party, player.getUniqueId(), false);
    }

    public boolean disbandParty(Player owner) {
        Party party = getPartyOf(owner.getUniqueId());
        if (party == null) {
            owner.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
            return false;
        }
        if (!party.isOwner(owner.getUniqueId())) {
            owner.sendMessage(Component.text("Only the party owner can disband.", NamedTextColor.RED));
            return false;
        }
        disband(party, "Party disbanded by owner.");
        return true;
    }

    public void handleDisconnect(UUID playerId) {
        Party party = getPartyOf(playerId);
        if (party == null) return;
        removeFromParty(party, playerId, true);
    }

    private boolean removeFromParty(Party party, UUID playerId, boolean silent) {
        boolean wasOwner = party.isOwner(playerId);
        party.removeMember(playerId);
        playerToParty.remove(playerId);

        if (party.size() < Party.MIN_SIZE && party.size() > 0) {
            // Only one (or zero) member left -> disband
            disband(party, "Party disbanded (not enough members).");
            return true;
        }
        if (party.size() == 0) {
            parties.remove(party.getId());
            return true;
        }

        if (wasOwner) {
            // Promote next member
            UUID newOwner = party.getMembers().get(0);
            party.setOwner(newOwner);
            Player p = Bukkit.getPlayer(newOwner);
            if (p != null) {
                p.sendMessage(Component.text("You are now the party owner.", NamedTextColor.GOLD));
            }
        }

        if (!silent) {
            Player leaver = Bukkit.getPlayer(playerId);
            String name = leaver != null ? leaver.getName() : playerId.toString().substring(0, 8);
            party.messageAll(Component.text(name + " left the party.", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void disband(Party party, String reason) {
        party.messageAll(Component.text(reason, NamedTextColor.GRAY));
        for (UUID member : party.getMembers()) {
            playerToParty.remove(member);
        }
        parties.remove(party.getId());
    }

    public void cleanup() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        parties.clear();
        playerToParty.clear();
        incomingInvites.clear();
    }
}
