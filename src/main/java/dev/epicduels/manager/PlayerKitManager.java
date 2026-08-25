package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Kit;
import dev.epicduels.model.KitLayout;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Per-player kit personalization: each player can rearrange the items of any
 * kit into their preferred slots (hotbar order, armor stays armor). Layouts
 * are stored in playerkits.yml keyed by player UUID and kit name, and are
 * validated against the base kit both on save and on apply — so a layout
 * that became stale after an admin edited the kit silently falls back to
 * the default arrangement.
 */
public class PlayerKitManager {

    private final EpicDuels plugin;
    private final File dataFile;
    // player UUID -> (kit name lowercase -> layout)
    private final Map<UUID, Map<String, KitLayout>> layouts = new HashMap<>();

    public PlayerKitManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerkits.yml");
        loadLayouts();
    }

    public void loadLayouts() {
        layouts.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidKey : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection playerSection = config.getConfigurationSection(uuidKey);
            if (playerSection == null) continue;

            for (String kitKey : playerSection.getKeys(false)) {
                try {
                    ItemStack[] contents = deserializeItemStacks(playerSection.getString(kitKey + ".contents"));
                    ItemStack[] armor = deserializeItemStacks(playerSection.getString(kitKey + ".armor"));
                    ItemStack offHand = null;
                    String offHandStr = playerSection.getString(kitKey + ".offhand");
                    if (offHandStr != null && !offHandStr.isEmpty()) {
                        ItemStack[] offHandArr = deserializeItemStacks(offHandStr);
                        if (offHandArr != null && offHandArr.length > 0) {
                            offHand = offHandArr[0];
                        }
                    }
                    if (contents == null) continue;
                    layouts.computeIfAbsent(uuid, k -> new HashMap<>())
                            .put(kitKey.toLowerCase(), new KitLayout(contents, armor, offHand));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to load personalized kit layout: " + uuidKey + "/" + kitKey, e);
                }
            }
        }
    }

    public void saveLayouts() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, KitLayout>> playerEntry : layouts.entrySet()) {
            String uuidKey = playerEntry.getKey().toString();
            for (Map.Entry<String, KitLayout> kitEntry : playerEntry.getValue().entrySet()) {
                KitLayout layout = kitEntry.getValue();
                String path = uuidKey + "." + kitEntry.getKey();
                config.set(path + ".contents", serializeItemStacks(layout.getContents()));
                if (layout.getArmor() != null) {
                    config.set(path + ".armor", serializeItemStacks(layout.getArmor()));
                }
                if (layout.getOffHand() != null) {
                    config.set(path + ".offhand", serializeItemStacks(new ItemStack[]{layout.getOffHand()}));
                }
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save playerkits.yml", e);
        }
    }

    public KitLayout getLayout(UUID player, String kitName) {
        Map<String, KitLayout> playerLayouts = layouts.get(player);
        return playerLayouts != null ? playerLayouts.get(kitName.toLowerCase()) : null;
    }

    public boolean hasLayout(UUID player, String kitName) {
        return getLayout(player, kitName) != null;
    }

    public void saveLayout(UUID player, String kitName, KitLayout layout) {
        layouts.computeIfAbsent(player, k -> new HashMap<>()).put(kitName.toLowerCase(), layout);
        saveLayouts();
    }

    /** Removes a player's layout for a kit. Returns true if one existed. */
    public boolean resetLayout(UUID player, String kitName) {
        Map<String, KitLayout> playerLayouts = layouts.get(player);
        if (playerLayouts == null) return false;
        KitLayout removed = playerLayouts.remove(kitName.toLowerCase());
        if (playerLayouts.isEmpty()) layouts.remove(player);
        if (removed == null) return false;
        saveLayouts();
        return true;
    }

    /**
     * The kit as it should be applied to this player: the personalized
     * layout if one exists and still matches the base kit's items,
     * otherwise the base kit itself.
     */
    public Kit getPersonalizedKit(UUID player, Kit base) {
        if (!plugin.isFeatureEnabled("kit-editor")) return base;
        KitLayout layout = getLayout(player, base.getName());
        if (layout == null) return base;
        if (validateLayout(base, layout.getContents(), layout.getArmor(), layout.getOffHand()) != null) {
            // Stale — the base kit was edited since this layout was saved
            return base;
        }
        return new Kit(base.getName(), layout.getContents(), layout.getArmor(), layout.getOffHand());
    }

    /**
     * Checks that a layout is a pure rearrangement of the base kit:
     * exactly the same items (type, meta and total amounts), and armor
     * slots only holding pieces that actually fit there. Returns null if
     * valid, or the message key of a player-facing error.
     */
    public String validateLayout(Kit base, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        // Armor slots must hold matching equipment (boots, leggings, chestplate, helmet)
        EquipmentSlot[] expected = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        if (armor != null) {
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null && armor[i].getType() != Material.AIR
                        && armor[i].getType().getEquipmentSlot() != expected[i]) {
                    return "kiteditor.error-armor-slot";
                }
            }
        }

        Map<ItemStack, Integer> baseItems = countItems(base.getContents(), base.getArmorContents(), base.getOffHand());
        Map<ItemStack, Integer> layoutItems = countItems(contents, armor, offHand);
        if (!baseItems.equals(layoutItems)) {
            return "kiteditor.error-items-changed";
        }
        return null;
    }

    /** Multiset of items (amount-1 stack -> total count) across all slot groups. */
    private Map<ItemStack, Integer> countItems(ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        Map<ItemStack, Integer> counts = new HashMap<>();
        addCounts(counts, contents);
        addCounts(counts, armor);
        if (offHand != null && offHand.getType() != Material.AIR) {
            counts.merge(offHand.asOne(), offHand.getAmount(), Integer::sum);
        }
        return counts;
    }

    private void addCounts(Map<ItemStack, Integer> counts, ItemStack[] items) {
        if (items == null) return;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            counts.merge(item.asOne(), item.getAmount(), Integer::sum);
        }
    }

    // Same Paper-based item serialization as KitManager (no legacy fallback
    // needed — playerkits.yml never existed before this format).

    private String serializeItemStacks(ItemStack[] items) {
        try {
            ItemStack[] safeItems = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) {
                safeItems[i] = items[i] != null ? items[i] : new ItemStack(Material.AIR);
            }
            byte[] bytes = ItemStack.serializeItemsAsBytes(safeItems);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize items", e);
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
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize items", e);
            return null;
        }
    }
}
