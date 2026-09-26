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
 * Saves personal inventories for every battle mode before equipment changes.
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

    public boolean backup(Player player) {
        if (hasBackup(player.getUniqueId())) return false;
        YamlConfiguration config = new YamlConfiguration();
        config.set("contents", serializeItemStacks(player.getInventory().getStorageContents()));
        config.set("armor", serializeItemStacks(player.getInventory().getArmorContents()));
        config.set("offhand", serializeItemStacks(new ItemStack[]{player.getInventory().getItemInOffHand()}));
        if (config.getString("contents").isEmpty() || config.getString("armor").isEmpty()
                || config.getString("offhand").isEmpty()) return false;
        config.set("game-mode", player.getGameMode().name());
        config.set("return-location", player.getLocation());
        java.nio.file.Path temporary = null;
        try {
            temporary = java.nio.file.Files.createTempFile(directory.toPath(), "inventory-", ".tmp");
            config.save(temporary.toFile());
            java.nio.file.Files.move(temporary, fileOf(player.getUniqueId()).toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory backup for " + player.getName(), e);
            return false;
        } finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); } catch (IOException ignored) {}
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

        if (contents == null || contents.length != 36 || armor == null || armor.length != 4
                || offHand == null || offHand.length != 1) {
            plugin.getLogger().severe("Invalid inventory backup retained for " + player.getUniqueId());
            return false;
        }
        try {
            if (config.contains("game-mode")) org.bukkit.GameMode.valueOf(config.getString("game-mode"));
            if (config.contains("return-location")) config.getLocation("return-location");
        } catch (RuntimeException invalid) { return false; }
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
        if (!hasBackup(player.getUniqueId())) return false;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fileOf(player.getUniqueId()));
        try {
            if (config.contains("game-mode")) org.bukkit.GameMode.valueOf(config.getString("game-mode"));
            if (config.contains("return-location")) config.getLocation("return-location");
        } catch (RuntimeException invalid) { return false; }
        if (!plugin.getConfig().getBoolean("lobby.handle-join", true)) {
            org.bukkit.Location location = config.getLocation("return-location");
            if (location != null && location.getWorld() != null && !player.teleport(location)) return false;
            player.setGameMode(org.bukkit.GameMode.valueOf(config.getString("game-mode", "SURVIVAL")));
        }
        if (!apply(player)) return false;
        try {
            // Persist the applied player state before durably consuming the recovery file.
            player.saveData();
            java.nio.file.Path consumed = new File(directory, player.getUniqueId() + ".consumed").toPath();
            java.nio.file.Files.move(fileOf(player.getUniqueId()).toPath(), consumed,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // A retained .consumed file is never replayed; deletion is only housekeeping.
            try { java.nio.file.Files.deleteIfExists(consumed); } catch (IOException ignored) {}
            return true;
        } catch (RuntimeException | IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Inventory recovery remains protected for " + player.getName(), error);
            return false;
        }
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
