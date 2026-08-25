package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.gui.MenuHolder;
import dev.epicduels.gui.MenuKeys;
import dev.epicduels.gui.MenuType;
import dev.epicduels.i18n.Messages;
import dev.epicduels.manager.GUIManager;
import dev.epicduels.model.Arena;
import dev.epicduels.model.Kit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GUIListener implements Listener {

    private final EpicDuels plugin;

    public GUIListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) return;

        // Kit edit (admin) and kit customize (player) are the only non-read-only GUIs
        if (holder.getType() == MenuType.KIT_EDIT) {
            handleKitEditClick(event, player, holder);
            return;
        }
        if (holder.getType() == MenuType.KIT_CUSTOMIZE) {
            handleKitCustomizeClick(event, player, holder);
            return;
        }

        // All other plugin menus are read-only
        event.setCancelled(true);

        // Only clicks in the menu itself trigger actions — never clicks in
        // the player's own inventory below it.
        if (event.getClickedInventory() != event.getInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().endsWith("STAINED_GLASS_PANE")) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        int slot = event.getSlot();

        switch (holder.getType()) {
            case MAIN -> handleMainMenuClick(player, slot);
            case DUELS -> handleDuelsMenuClick(player, slot, clicked, holder);
            case STATS -> handleStatsMenuClick(player, slot);
            case MATCHMAKING -> handleMatchmakingClick(player, slot, clicked, holder);
            case KIT_SELECT -> handleKitSelectClick(player, slot, clicked, holder);
            case ROUNDS_SELECT -> handleRoundsSelectClick(player, slot);
            case ARENA_SELECT -> handleArenaSelectClick(player, slot, clicked, holder);
            case KIT_LIST -> handleKitListClick(player, slot, clicked, holder);
            case KIT_EDITOR_LIST -> handleKitEditorListClick(player, slot, clicked, holder);
            case ARENA_LIST -> handleArenaListClick(player, slot, holder);
            case SPECTATE -> handleSpectateClick(player, slot, clicked, holder);
            case PARTY_MODE -> handlePartyModeClick(player, slot);
            case PARTY_TEAM_SIZE -> handlePartyTeamSizeClick(player, slot);
            case PARTY_KIT -> handlePartyKitClick(player, slot, clicked, holder);
            case PARTY_CONFIRM -> handlePartyConfirmClick(player, slot);
            case KIT_PREVIEW -> { } // view-only
            default -> { }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) return;

        // The customize GUI only ever holds clones of kit items — void a
        // leftover cursor item so it is not handed to the player on close.
        if (holder.getType() == MenuType.KIT_CUSTOMIZE) {
            event.getView().setCursor(null);
        }

        UUID uuid = player.getUniqueId();

        // The challenge flow (Player -> Kit -> Map) opens a new GUI right after closing
        // the old one, which fires this close event for the GUI being replaced. Delay the
        // state cleanup by one tick and only clear it if the player doesn't have one of
        // our GUIs open anymore by then.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof MenuHolder) return;

            plugin.getGUIManager().cancelAnimation(uuid);
            plugin.getGUIManager().clearChallengeData(uuid);
            plugin.getGUIManager().clearPartyFlow(uuid);
        });
    }

    // ========== MAIN MENU ==========

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { // Diamond Sword — Duels
                if (!plugin.isFeatureEnabled("challenges")) return;
                player.closeInventory();
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            case 13 -> { // Player Head — Stats
                player.closeInventory();
                plugin.getGUIManager().openStatsMenu(player);
            }
            case 4 -> { // Ender Eye — Spectate
                if (!plugin.isFeatureEnabled("spectating")) return;
                if (!player.hasPermission("epicduels.spectate")) return;
                player.closeInventory();
                plugin.getGUIManager().openSpectateMenu(player, 0);
            }
            case 16 -> { // Hopper — Matchmaking
                if (!plugin.isFeatureEnabled("matchmaking")) return;
                player.closeInventory();
                plugin.getGUIManager().openMatchmakingMenu(player, 0);
            }
            case 22 -> { // Anvil — Kit Editor
                if (!plugin.isFeatureEnabled("kit-editor")) return;
                player.closeInventory();
                plugin.getGUIManager().openKitEditorList(player, 0);
            }
        }
    }

    // ========== DUELS — PLAYER SELECT ==========

    private void handleDuelsMenuClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        // Navigation
        if (handlePagination(player, slot, holder)) return;

        // Back button
        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        // Player head click
        String targetId = MenuKeys.tag(clicked, MenuKeys.PLAYER);
        if (targetId == null) return;

        Player target = Bukkit.getPlayer(UUID.fromString(targetId));
        if (target == null || !target.isOnline()) {
            Messages.send(player, "general.player-offline");
            player.closeInventory();
            return;
        }

        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "general.already-in-match");
            player.closeInventory();
            return;
        }

        if (plugin.getDuelManager().isBusy(target.getUniqueId())) {
            Messages.send(player, "general.target-in-match");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().openKitSelect(player, target.getUniqueId());
    }

    // ========== STATS ==========

    private void handleStatsMenuClick(Player player, int slot) {
        if (slot == 22) { // Back arrow
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
        }
    }

    // ========== MATCHMAKING ==========

    private void handleMatchmakingClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        // Kit item click — toggle queue
        String kitName = MenuKeys.tag(clicked, MenuKeys.KIT);
        if (kitName == null) return;

        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;

        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "general.already-in-match");
            return;
        }

        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
            Messages.send(player, "queue.left");
            player.sendActionBar(Component.empty());
        } else {
            boolean joined = plugin.getQueueManager().joinQueue(player.getUniqueId(), kit.getName());
            if (joined) {
                Messages.send(player, "queue.joined", Messages.unparsed("kit", kit.getName()));
            } else {
                Messages.send(player, "queue.join-failed");
            }
        }

        // Refresh the menu to show updated queue status
        player.closeInventory();
        plugin.getGUIManager().openMatchmakingMenu(player, holder.getPage());
    }

    // ========== KIT SELECT (challenge flow) ==========

    private void handleKitSelectClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openDuelsMenu(player, 0);
            return;
        }

        UUID targetUUID = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        // "Own Inventory" option — no-kit duel
        if ("own-inventory".equals(MenuKeys.tag(clicked, MenuKeys.ACTION))) {
            if (!plugin.isOwnInventoryDuelsEnabled()) return;
            player.closeInventory();
            if (plugin.isFeatureEnabled("best-of-n")) {
                plugin.getGUIManager().openRoundsSelect(player, targetUUID, Kit.OWN_INVENTORY);
            } else {
                plugin.getGUIManager().openArenaSelect(player, targetUUID, Kit.OWN_INVENTORY);
            }
            return;
        }

        String kitName = MenuKeys.tag(clicked, MenuKeys.KIT);
        if (kitName == null) return;

        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            Messages.send(player, "general.kit-not-available");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        if (plugin.isFeatureEnabled("best-of-n")) {
            plugin.getGUIManager().openRoundsSelect(player, targetUUID, kit.getName());
        } else {
            // Rounds selection disabled — every duel is a single round (Bo1)
            plugin.getGUIManager().openArenaSelect(player, targetUUID, kit.getName());
        }
    }

    // ========== ROUNDS SELECT (challenge flow) ==========

    private void handleRoundsSelectClick(Player player, int slot) {
        UUID targetUUID = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
        String kitName = plugin.getGUIManager().getChallengeKit(player.getUniqueId());

        if (slot == 22) { // Back arrow
            player.closeInventory();
            if (targetUUID != null) {
                plugin.getGUIManager().openKitSelect(player, targetUUID);
            } else {
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            return;
        }

        int bestOf = switch (slot) {
            case 11 -> 1;
            case 13 -> 3;
            case 15 -> 5;
            default -> 0;
        };
        if (bestOf == 0) return;

        if (targetUUID == null || kitName == null) {
            player.closeInventory();
            return;
        }

        plugin.getGUIManager().setChallengeRounds(player.getUniqueId(), bestOf);
        player.closeInventory();
        plugin.getGUIManager().openArenaSelect(player, targetUUID, kitName);
    }

    // ========== ARENA SELECT (challenge flow) ==========

    private void handleArenaSelectClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            UUID targetUUID = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
            String kitName = plugin.getGUIManager().getChallengeKit(player.getUniqueId());
            player.closeInventory();
            if (targetUUID != null && kitName != null) {
                plugin.getGUIManager().openRoundsSelect(player, targetUUID, kitName);
            } else if (targetUUID != null) {
                plugin.getGUIManager().openKitSelect(player, targetUUID);
            } else {
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            return;
        }

        // Random Map compass
        if ("random-map".equals(MenuKeys.tag(clicked, MenuKeys.ACTION))) {
            if (!plugin.getGUIManager().isAnimating(player.getUniqueId())) {
                plugin.getGUIManager().startRandomMapAnimation(player);
            }
            return;
        }

        if (plugin.getGUIManager().isAnimating(player.getUniqueId())) return;

        String arenaName = MenuKeys.tag(clicked, MenuKeys.ARENA);
        if (arenaName == null) return;

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || !arena.isReady()) {
            Messages.send(player, "general.arena-not-available");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().finishChallengeWithArena(player, arena.getName());
    }

    // ========== KIT LIST (admin) ==========

    private void handleKitListClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            return;
        }

        String kitName = MenuKeys.tag(clicked, MenuKeys.KIT);
        if (kitName == null) return;

        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;

        player.closeInventory();
        plugin.getGUIManager().openKitPreview(player, kit);
    }

    // ========== ARENA LIST (admin) ==========

    private void handleArenaListClick(Player player, int slot, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
        }
    }

    // ========== SPECTATE — LIVE DUELS ==========

    private void handleSpectateClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        String targetId = MenuKeys.tag(clicked, MenuKeys.PLAYER);
        if (targetId == null) return;

        if (!player.hasPermission("epicduels.spectate")) {
            Messages.send(player, "general.no-permission");
            player.closeInventory();
            return;
        }

        // Re-validate on click — the duel may have ended (or the clicker may
        // have been pulled into a match) while the menu was open.
        if (plugin.getDuelManager().isBusy(player.getUniqueId())) {
            Messages.send(player, "spectate.while-in-match");
            player.closeInventory();
            return;
        }

        var duel = plugin.getDuelManager().getDuel(UUID.fromString(targetId));
        if (duel == null || !duel.isActive() || duel.getInstanceWorld() == null) {
            Messages.send(player, "spectate.duel-ended");
            player.closeInventory();
            plugin.getGUIManager().openSpectateMenu(player, holder.getPage());
            return;
        }

        player.closeInventory();
        plugin.getDuelManager().addSpectator(player, duel);
    }

    // ========== KIT EDITOR (per-player personalization) ==========

    private void handleKitEditorListClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        String kitName = MenuKeys.tag(clicked, MenuKeys.KIT);
        if (kitName == null) return;

        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;

        player.closeInventory();
        plugin.getGUIManager().openKitCustomize(player, kit);
    }

    private void handleKitCustomizeClick(InventoryClickEvent event, Player player, MenuHolder holder) {
        int slot = event.getRawSlot();

        // The kit items in this GUI are clones — never let them reach the
        // player's own inventory (and never let own items in): block every
        // interaction with the bottom inventory and every cross-inventory move.
        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                event.setCancelled(true);
                return;
            }
            default -> { }
        }
        if (slot >= 54) {
            event.setCancelled(true);
            return;
        }

        // Control area (panes + buttons)
        if (slot >= 41 && slot <= 53) {
            event.setCancelled(true);

            String kitName = holder.getContext();
            Kit kit = kitName != null ? plugin.getKitManager().getKit(kitName) : null;
            if (kit == null) {
                Messages.send(player, "general.kit-not-found");
                player.closeInventory();
                return;
            }

            if (slot == GUIManager.CUSTOMIZE_RESET_SLOT) {
                boolean had = plugin.getPlayerKitManager().resetLayout(player.getUniqueId(), kit.getName());
                Messages.send(player, had ? "kiteditor.reset" : "kiteditor.no-layout",
                        Messages.unparsed("kit", kit.getName()));
                player.closeInventory();
                plugin.getGUIManager().openKitCustomize(player, kit);
                return;
            }

            if (slot == GUIManager.CUSTOMIZE_SAVE_SLOT) {
                Inventory inv = event.getInventory();

                ItemStack[] contents = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    ItemStack item = inv.getItem(i);
                    contents[i] = item != null ? item.clone() : null;
                }
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    ItemStack item = inv.getItem(36 + i);
                    armor[i] = item != null ? item.clone() : null;
                }
                ItemStack offHandItem = inv.getItem(40);
                ItemStack offHand = offHandItem != null ? offHandItem.clone() : null;

                String error = plugin.getPlayerKitManager().validateLayout(kit, contents, armor, offHand);
                if (error != null) {
                    Messages.send(player, error);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
                    return;
                }

                plugin.getPlayerKitManager().saveLayout(player.getUniqueId(), kit.getName(),
                        new dev.epicduels.model.KitLayout(contents, armor, offHand));
                player.closeInventory();
                Messages.send(player, "kiteditor.saved", Messages.unparsed("kit", kit.getName()));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) return;

        switch (holder.getType()) {
            case KIT_CUSTOMIZE -> {
                // Only allow drags fully inside the editable kit slots (0-40)
                for (int raw : event.getRawSlots()) {
                    if (raw > 40) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            case KIT_EDIT -> {
                // Admins may drag between their inventory and the kit slots,
                // but never onto the control row (items placed there are lost)
                for (int raw : event.getRawSlots()) {
                    if (raw >= 41 && raw <= 53) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            default ->
                // Read-only GUIs: no item placement at all
                event.setCancelled(true);
        }
    }

    // ========== KIT EDIT ==========

    private void handleKitEditClick(InventoryClickEvent event, Player player, MenuHolder holder) {
        int slot = event.getRawSlot();

        if (slot >= 41 && slot <= 52) {
            event.setCancelled(true);
            return;
        }

        if (slot == 53) {
            event.setCancelled(true);

            String kitName = holder.getContext();
            Kit kit = kitName != null ? plugin.getKitManager().getKit(kitName) : null;
            if (kit == null) {
                Messages.send(player, "general.kit-not-found");
                player.closeInventory();
                return;
            }

            Inventory inv = event.getInventory();

            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                contents[i] = item != null ? item.clone() : null;
            }

            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack item = inv.getItem(36 + i);
                armor[i] = item != null ? item.clone() : null;
            }

            ItemStack offHandItem = inv.getItem(40);
            ItemStack offHand = offHandItem != null ? offHandItem.clone() : null;

            kit.setContents(contents);
            kit.setArmorContents(armor);
            kit.setOffHand(offHand);
            plugin.getKitManager().updateKit(kit);

            player.closeInventory();
            Messages.send(player, "general.kit-saved", Messages.unparsed("kit", kit.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }

    // ========== PAGINATION ==========

    /**
     * Handles prev/next page clicks. Returns true if a nav button was clicked.
     */
    private boolean handlePagination(Player player, int slot, MenuHolder holder) {
        int currentPage = holder.getPage();

        if (slot == GUIManager.PREV_SLOT) {
            if (currentPage > 0) {
                player.closeInventory();
                openMenuAtPage(player, holder.getType(), currentPage - 1);
            }
            return true;
        }

        if (slot == GUIManager.NEXT_SLOT) {
            player.closeInventory();
            openMenuAtPage(player, holder.getType(), currentPage + 1);
            return true;
        }

        return false;
    }

    private void openMenuAtPage(Player player, MenuType type, int page) {
        switch (type) {
            case DUELS -> plugin.getGUIManager().openDuelsMenu(player, page);
            case MATCHMAKING -> plugin.getGUIManager().openMatchmakingMenu(player, page);
            case KIT_SELECT -> {
                UUID target = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
                if (target != null) {
                    plugin.getGUIManager().openKitSelect(player, target, page);
                }
            }
            case ARENA_SELECT -> {
                UUID target = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
                String kit = plugin.getGUIManager().getChallengeKit(player.getUniqueId());
                if (target != null && kit != null) {
                    plugin.getGUIManager().openArenaSelect(player, target, kit, page);
                }
            }
            case KIT_LIST -> plugin.getGUIManager().openKitList(player, page);
            case KIT_EDITOR_LIST -> plugin.getGUIManager().openKitEditorList(player, page);
            case ARENA_LIST -> plugin.getGUIManager().openArenaList(player, page);
            case SPECTATE -> plugin.getGUIManager().openSpectateMenu(player, page);
            case PARTY_KIT -> plugin.getGUIManager().openPartyKitMenu(player, page);
            default -> { }
        }
    }

    // ========== PARTY FLOW HANDLERS ==========

    private void handlePartyModeClick(Player player, int slot) {
        if (slot == 11) {
            if (!plugin.isFeatureEnabled("team-duels")) return;
            player.closeInventory();
            plugin.getGUIManager().openPartyTeamSizeMenu(player);
        } else if (slot == 15) {
            if (!plugin.isFeatureEnabled("tournaments")) return;
            player.closeInventory();
            plugin.getGUIManager().setPartyFlowMode(player.getUniqueId(),
                    dev.epicduels.model.PartyMode.TOURNAMENT);
            plugin.getGUIManager().openPartyKitMenu(player, 0);
        }
    }

    private void handlePartyTeamSizeClick(Player player, int slot) {
        if (slot == 22) {
            player.closeInventory();
            plugin.getGUIManager().openPartyModeMenu(player);
            return;
        }
        dev.epicduels.model.TeamSize size = switch (slot) {
            case 11 -> dev.epicduels.model.TeamSize.TWO_VS_TWO;
            case 13 -> dev.epicduels.model.TeamSize.THREE_VS_THREE;
            case 15 -> dev.epicduels.model.TeamSize.FOUR_VS_FOUR;
            default -> null;
        };
        if (size == null) return;

        dev.epicduels.model.Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
        if (party == null || party.size() < size.getTotalPlayers()) {
Messages.send(player, "party.not-enough-members", Messages.unparsed("size", size.getLabel()));
            return;
        }

        plugin.getGUIManager().setPartyFlowMode(player.getUniqueId(),
                dev.epicduels.model.PartyMode.TEAM_DUEL);
        plugin.getGUIManager().setPartyFlowTeamSize(player.getUniqueId(), size);
        player.closeInventory();
        plugin.getGUIManager().openPartyKitMenu(player, 0);
    }

    private void handlePartyKitClick(Player player, int slot, ItemStack clicked, MenuHolder holder) {
        if (handlePagination(player, slot, holder)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            dev.epicduels.model.PartyMode mode = plugin.getGUIManager()
                    .getPartyFlowMode(player.getUniqueId());
            if (mode == dev.epicduels.model.PartyMode.TEAM_DUEL) {
                plugin.getGUIManager().openPartyTeamSizeMenu(player);
            } else {
                plugin.getGUIManager().openPartyModeMenu(player);
            }
            return;
        }
        String kitName = MenuKeys.tag(clicked, MenuKeys.KIT);
        if (kitName == null) return;
        dev.epicduels.model.Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            Messages.send(player, "general.kit-not-available");
            return;
        }
        plugin.getGUIManager().setPartyFlowKit(player.getUniqueId(), kit.getName());
        player.closeInventory();
        plugin.getGUIManager().openPartyConfirmMenu(player);
    }

    private void handlePartyConfirmClick(Player player, int slot) {
        if (slot == 15) { // Cancel
            player.closeInventory();
            plugin.getGUIManager().clearPartyFlow(player.getUniqueId());
            Messages.send(player, "general.cancelled");
            return;
        }
        if (slot != 11) return; // Only confirm slot

        dev.epicduels.model.PartyMode mode = plugin.getGUIManager().getPartyFlowMode(player.getUniqueId());
        String kitName = plugin.getGUIManager().getPartyFlowKit(player.getUniqueId());
        dev.epicduels.model.Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());

        if (mode == null || kitName == null || party == null) {
            Messages.send(player, "party.missing-data");
            player.closeInventory();
            return;
        }
        if (!party.isOwner(player.getUniqueId())) {
            Messages.send(player, "party.owner-only-start");
            player.closeInventory();
            return;
        }
        dev.epicduels.model.Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            Messages.send(player, "general.kit-gone");
            player.closeInventory();
            return;
        }

        // Re-check busy state — the GUI flow can stay open arbitrarily long
        // after the /party start check, and members may have entered matches
        for (java.util.UUID id : party.getMembers()) {
            if (plugin.getDuelManager().isBusy(id)) {
                Messages.send(player, "party.member-busy");
                player.closeInventory();
                return;
            }
        }

        player.closeInventory();

        if (mode == dev.epicduels.model.PartyMode.TEAM_DUEL) {
            dev.epicduels.model.TeamSize size = plugin.getGUIManager().getPartyFlowTeamSize(player.getUniqueId());
            if (size == null) {
                Messages.send(player, "party.no-team-size");
                return;
            }
            java.util.List<dev.epicduels.model.Arena> ready = plugin.getArenaManager().getReadyArenas();
            if (ready.isEmpty()) {
                party.messageAll(Messages.get("party.no-ready-arenas"));
                return;
            }
            dev.epicduels.model.Arena arena = ready.get(new java.util.Random().nextInt(ready.size()));
            boolean ok = plugin.getTeamDuelManager().startTeamDuel(party.getMembers(), size, arena, kit);
            if (!ok) {
                party.messageAll(Messages.get("party.teamduel-failed"));
            }
        } else {
            boolean ok = plugin.getTournamentManager().startTournament(party, kit);
            if (!ok) {
                party.messageAll(Messages.get("party.tournament-failed"));
            }
        }
        plugin.getGUIManager().clearPartyFlow(player.getUniqueId());
    }
}
