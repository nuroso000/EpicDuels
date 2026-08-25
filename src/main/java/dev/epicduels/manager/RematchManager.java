package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelInstance;
import dev.epicduels.model.Kit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offers both players a rematch after a 1v1 duel ends. Each player gets a
 * clickable [REMATCH] message (valid for 30 seconds); once both click (or run
 * /duel rematch), a new duel with the same kit and arena starts.
 */
public class RematchManager {

    private static final long OFFER_VALID_MILLIS = 30_000;

    private static class Offer {
        final UUID player1;
        final UUID player2;
        final String arenaName;
        final String kitName;
        final int bestOf;
        final long createdAt = System.currentTimeMillis();
        final Set<UUID> accepted = Collections.newSetFromMap(new ConcurrentHashMap<>());

        Offer(UUID player1, UUID player2, String arenaName, String kitName, int bestOf) {
            this.player1 = player1;
            this.player2 = player2;
            this.arenaName = arenaName;
            this.kitName = kitName;
            this.bestOf = bestOf;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > OFFER_VALID_MILLIS;
        }

        UUID getOpponent(UUID playerId) {
            return player1.equals(playerId) ? player2 : player1;
        }
    }

    private final EpicDuels plugin;
    // Both participants map to the same shared offer
    private final Map<UUID, Offer> offersByPlayer = new ConcurrentHashMap<>();

    public RematchManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    /**
     * Called by the DuelManager when a duel ends (win or draw). Sends both
     * players a clickable rematch offer once they are back in the lobby.
     */
    public void offerRematch(DuelInstance duel) {
        if (!plugin.isFeatureEnabled("rematch")) return;
        purgeExpired();

        Offer offer = new Offer(duel.getPlayer1(), duel.getPlayer2(), duel.getArenaName(), duel.getKitName(), duel.getBestOf());
        offersByPlayer.put(duel.getPlayer1(), offer);
        offersByPlayer.put(duel.getPlayer2(), offer);

        // Send the message after the 3s post-duel lobby teleport
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (offersByPlayer.get(duel.getPlayer1()) != offer) return;
            Player p1 = Bukkit.getPlayer(duel.getPlayer1());
            Player p2 = Bukkit.getPlayer(duel.getPlayer2());
            if (p1 != null && p2 != null) {
                sendOfferMessage(p1, p2.getName());
                sendOfferMessage(p2, p1.getName());
            }
        }, 70L);
    }

    private void sendOfferMessage(Player player, String opponentName) {
        Messages.send(player, "rematch.offer", Messages.unparsed("player", opponentName));
    }

    /**
     * Player clicked [REMATCH] or ran /duel rematch. Starts the duel once both
     * players have accepted.
     */
    public void accept(Player player) {
        Offer offer = offersByPlayer.get(player.getUniqueId());
        if (offer == null || offer.isExpired()) {
            removeOffer(offer);
            Messages.send(player, "rematch.no-offer");
            return;
        }

        Player opponent = Bukkit.getPlayer(offer.getOpponent(player.getUniqueId()));
        if (opponent == null || !opponent.isOnline()) {
            removeOffer(offer);
            Messages.send(player, "rematch.opponent-offline");
            return;
        }

        if (!offer.accepted.add(player.getUniqueId())) {
            Messages.send(player, "rematch.already-accepted", Messages.unparsed("player", opponent.getName()));
            return;
        }

        if (offer.accepted.size() < 2) {
            Messages.send(player, "rematch.accepted-waiting", Messages.unparsed("player", opponent.getName()));
            Messages.send(opponent, "rematch.opponent-wants", Messages.unparsed("player", player.getName()));
            opponent.playSound(opponent.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        // Both accepted — start the new duel
        removeOffer(offer);

        if (plugin.getDuelManager().isBusy(player.getUniqueId())
                || plugin.getDuelManager().isBusy(opponent.getUniqueId())) {
            Component busy = Messages.get("rematch.busy");
            player.sendMessage(busy);
            opponent.sendMessage(busy);
            return;
        }

        boolean ownInventory = Kit.OWN_INVENTORY.equals(offer.kitName);
        Arena arena = plugin.getArenaManager().getArena(offer.arenaName);
        Kit kit = ownInventory ? null : plugin.getKitManager().getKit(offer.kitName);
        if (arena == null || (!ownInventory && kit == null)) {
            Component gone = Messages.get("rematch.gone");
            player.sendMessage(gone);
            opponent.sendMessage(gone);
            return;
        }

        plugin.getDuelManager().startDirectDuel(player, opponent, arena, kit, offer.bestOf);
    }

    public void handleDisconnect(UUID playerId) {
        Offer offer = offersByPlayer.remove(playerId);
        if (offer == null) return;
        offersByPlayer.remove(offer.getOpponent(playerId), offer);

        Player opponent = Bukkit.getPlayer(offer.getOpponent(playerId));
        if (opponent != null && offer.accepted.contains(opponent.getUniqueId()) && !offer.isExpired()) {
            Messages.send(opponent, "rematch.cancelled-left");
        }
    }

    private void removeOffer(Offer offer) {
        if (offer == null) return;
        offersByPlayer.remove(offer.player1, offer);
        offersByPlayer.remove(offer.player2, offer);
    }

    private void purgeExpired() {
        Iterator<Map.Entry<UUID, Offer>> it = offersByPlayer.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) it.remove();
        }
    }

    public void cleanup() {
        offersByPlayer.clear();
    }
}
