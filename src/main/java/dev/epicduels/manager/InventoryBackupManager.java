package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Saves player inventories to disk for "Own Inventory" (no-kit) duels.
 * One file per player under plugin/inventories/ so the backup survives
 * disconnects and server crashes; leftover backups are restored on the
 * player's next join.
 */
public class InventoryBackupManager {

    private final EpicDuels plugin;
    private final File directory;

    public InventoryBackupManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "inventories");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create inventory backup directory: " + directory);
        }
    }

    public void backup(Player player) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("contents", serializeItemStacks(player.getInventory().getStorageContents()));
        config.set("armor", serializeItemStacks(player.getInventory().getArmorContents()));
        config.set("offhand", serializeItemStacks(new ItemStack[]{player.getInventory().getItemInOffHand()}));
        try {
            config.save(fileOf(player.getUniqueId()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory backup for " + player.getName(), e);
        }
    }

    public boolean hasBackup(UUID playerId) {
        return fileOf(playerId).exists();
    }

    /**
     * Applies the saved inventory to the player, keeping the backup file
     * (used for round resets in best-of-N duels).
     * Returns false if there is no backup.
     */
    public boolean apply(Player player) {
        File file = fileOf(player.getUniqueId());
        if (!file.exists()) return false;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemStack[] contents = deserializeItemStacks(config.getString("contents"));
        ItemStack[] armor = deserializeItemStacks(config.getString("armor"));
        ItemStack[] offHand = deserializeItemStacks(config.getString("offhand"));

        player.getInventory().clear();
        if (contents != null) player.getInventory().setStorageContents(contents);
        if (armor != null) player.getInventory().setArmorContents(armor);
        if (offHand != null && offHand.length > 0 && offHand[0] != null) {
            player.getInventory().setItemInOffHand(offHand[0]);
        }
        return true;
    }

    /**
     * Restores the saved inventory and deletes the backup (duel end, or
     * rejoin after a disconnect/crash). Returns false if there is no backup.
     */
    public boolean restore(Player player) {
        if (!apply(player)) return false;
        if (!fileOf(player.getUniqueId()).delete()) {
            plugin.getLogger().warning("Could not delete inventory backup for " + player.getName());
        }
        return true;
    }

    private File fileOf(UUID playerId) {
        return new File(directory, playerId + ".yml");
    }

    private String serializeItemStacks(ItemStack[] items) {
        try {
            ItemStack[] safeItems = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) {
                safeItems[i] = items[i] != null ? items[i] : new ItemStack(Material.AIR);
            }
            byte[] bytes = ItemStack.serializeItemsAsBytes(safeItems);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize inventory items", e);
            return "";
        }
    }

    private ItemStack[] deserializeItemStacks(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(bytes);
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null && items[i].getType() == Material.AIR) {
                    items[i] = null;
                }
            }
            return items;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize inventory items", e);
            return null;
        }
    }
}
