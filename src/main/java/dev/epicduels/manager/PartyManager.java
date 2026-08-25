package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
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
                                Messages.send(receiver, "party.invite-expired-receiver");
                            }
                            if (sender != null) {
                                Messages.send(sender, "party.invite-expired-sender");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Party createParty(Player owner) {
        if (playerToParty.containsKey(owner.getUniqueId())) {
            Messages.send(owner, "party.already-in-party-create");
            return null;
        }
        Party party = new Party(owner.getUniqueId());
        parties.put(party.getId(), party);
        playerToParty.put(owner.getUniqueId(), party.getId());
        Messages.send(owner, "party.created");
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
            Messages.send(owner, "party.not-in-party-create-first");
            return false;
        }
        if (!party.isOwner(owner.getUniqueId())) {
            Messages.send(owner, "party.owner-only-invite");
            return false;
        }
        if (target.equals(owner)) {
            Messages.send(owner, "party.invite-self");
            return false;
        }
        if (party.contains(target.getUniqueId())) {
            Messages.send(owner, "party.already-in-your-party", Messages.unparsed("player", target.getName()));
            return false;
        }
        if (party.isFull()) {
            Messages.send(owner, "party.party-full-own", Messages.unparsed("max", Party.MAX_SIZE));
            return false;
        }
        if (isInParty(target.getUniqueId())) {
            Messages.send(owner, "party.target-in-other-party", Messages.unparsed("player", target.getName()));
            return false;
        }

        PartyInvite invite = new PartyInvite(party.getId(), owner.getUniqueId(), target.getUniqueId());
        incomingInvites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(owner.getUniqueId(), invite);

        Messages.send(owner, "party.invited", Messages.unparsed("player", target.getName()));

        target.sendMessage(Component.empty());
        Messages.send(target, "general.separator");
        Messages.send(target, "party.invite-received", Messages.unparsed("player", owner.getName()));
        target.sendMessage(Messages.format("party.invite-accept-deny", Map.of("player", owner.getName())));
        Messages.send(target, "party.invite-expires");
        Messages.send(target, "general.separator");
        target.sendMessage(Component.empty());
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    public boolean acceptInvite(Player player, UUID fromSenderOrNull) {
        Map<UUID, PartyInvite> map = incomingInvites.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            Messages.send(player, "party.no-invites");
            return false;
        }
        PartyInvite invite;
        if (fromSenderOrNull != null) {
            invite = map.remove(fromSenderOrNull);
        } else if (map.size() == 1) {
            invite = map.values().iterator().next();
            map.remove(invite.getSender());
        } else {
            Messages.send(player, "party.multiple-invites-accept");
            return false;
        }
        if (invite == null || invite.isExpired()) {
            Messages.send(player, "party.invite-gone");
            return false;
        }
        if (isInParty(player.getUniqueId())) {
            Messages.send(player, "party.already-in-party");
            return false;
        }
        Party party = parties.get(invite.getPartyId());
        if (party == null) {
            Messages.send(player, "party.party-gone");
            return false;
        }
        if (party.isFull()) {
            Messages.send(player, "party.party-full");
            return false;
        }
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party.getId());
        // Drop other pending invites for this player (joined a party)
        map.clear();

        party.messageAll(Messages.get("party.joined", Messages.unparsed("player", player.getName())));
        return true;
    }

    public boolean denyInvite(Player player, UUID fromSenderOrNull) {
        Map<UUID, PartyInvite> map = incomingInvites.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            Messages.send(player, "party.no-invites");
            return false;
        }
        PartyInvite invite;
        if (fromSenderOrNull != null) {
            invite = map.remove(fromSenderOrNull);
        } else if (map.size() == 1) {
            invite = map.values().iterator().next();
            map.remove(invite.getSender());
        } else {
            Messages.send(player, "party.multiple-invites-deny");
            return false;
        }
        if (invite == null) {
            Messages.send(player, "party.invite-not-exist");
            return false;
        }
        if (invite.isExpired()) {
            Messages.send(player, "party.invite-already-expired");
            return false;
        }
        Player sender = Bukkit.getPlayer(invite.getSender());
        if (sender != null) {
            Messages.send(sender, "party.invite-denied-sender", Messages.unparsed("player", player.getName()));
        }
        Messages.send(player, "party.invite-denied");
        return true;
    }

    public boolean leaveParty(Player player) {
        Party party = getPartyOf(player.getUniqueId());
        if (party == null) {
            Messages.send(player, "party.not-in-party");
            return false;
        }
        return removeFromParty(party, player.getUniqueId(), false);
    }

    public boolean disbandParty(Player owner) {
        Party party = getPartyOf(owner.getUniqueId());
        if (party == null) {
            Messages.send(owner, "party.not-in-party");
            return false;
        }
        if (!party.isOwner(owner.getUniqueId())) {
            Messages.send(owner, "party.owner-only-disband");
            return false;
        }
        disband(party, "party.disbanded-owner");
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
            disband(party, "party.disbanded-members");
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
                Messages.send(p, "party.new-owner");
            }
        }

        if (!silent) {
            Player leaver = Bukkit.getPlayer(playerId);
            String name = leaver != null ? leaver.getName() : playerId.toString().substring(0, 8);
            party.messageAll(Messages.get("party.left", Messages.unparsed("player", name)));
        }
        return true;
    }

    private void disband(Party party, String reasonKey) {
        party.messageAll(Messages.get(reasonKey));
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
