package dev.epicduels.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * PersistentDataContainer keys stamped onto GUI button items so click
 * handlers resolve kits/arenas/players from item data instead of parsing
 * display names. Initialized once in onEnable.
 */
public final class MenuKeys {

    /** Marker for special buttons, e.g. "own-inventory" or "random-map". */
    public static NamespacedKey ACTION;
    /** Kit name a button refers to. */
    public static NamespacedKey KIT;
    /** Arena name a button refers to. */
    public static NamespacedKey ARENA;
    /** Player UUID (as string) a button refers to, e.g. head items. */
    public static NamespacedKey PLAYER;

    private MenuKeys() {
    }

    public static void init(Plugin plugin) {
        ACTION = new NamespacedKey(plugin, "action");
        KIT = new NamespacedKey(plugin, "kit");
        ARENA = new NamespacedKey(plugin, "arena");
        PLAYER = new NamespacedKey(plugin, "player");
    }

    /** Stamps a string tag onto the item and returns it (for chaining). */
    public static ItemStack tag(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    /** Reads a string tag from the item, or null if absent. */
    public static String tag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
