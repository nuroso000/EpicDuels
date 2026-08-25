package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Kit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class KitManager {

    private final EpicDuels plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private final File dataFile;

    public KitManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    public void loadKits() {
        kits.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String name : config.getKeys(false)) {
            try {
                ItemStack[] contents = deserializeItemStacks(config.getString(name + ".contents"));
                ItemStack[] armor = deserializeItemStacks(config.getString(name + ".armor"));
                ItemStack offHand = null;
                String offHandStr = config.getString(name + ".offhand");
                if (offHandStr != null && !offHandStr.isEmpty()) {
                    ItemStack[] offHandArr = deserializeItemStacks(offHandStr);
                    if (offHandArr != null && offHandArr.length > 0) {
                        offHand = offHandArr[0];
                    }
                }
                Kit kit = new Kit(name, contents != null ? contents : new ItemStack[0], armor, offHand);
                String iconStr = config.getString(name + ".icon");
                if (iconStr != null) {
                    try {
                        kit.setIcon(Material.valueOf(iconStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                kits.put(name.toLowerCase(), kit);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load kit: " + name, e);
            }
        }
    }

    public void saveKits() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String name = kit.getName();
            config.set(name + ".contents", serializeItemStacks(kit.getContents()));
            if (kit.getArmorContents() != null) {
                config.set(name + ".armor", serializeItemStacks(kit.getArmorContents()));
            }
            if (kit.getOffHand() != null) {
                config.set(name + ".offhand", serializeItemStacks(new ItemStack[]{kit.getOffHand()}));
            }
            if (kit.getIcon() != null) {
                config.set(name + ".icon", kit.getIcon().name());
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kits.yml", e);
        }
    }

    public Kit createKit(String name, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        String key = name.toLowerCase();
        if (kits.containsKey(key)) return null;

        Kit kit = new Kit(name, contents, armor, offHand);
        kits.put(key, kit);
        saveKits();
        return kit;
    }

    /**
     * Rename a kit. Returns null on success, or an error message on failure.
     */
    public String renameKit(String oldName, String newName) {
        String oldKey = oldName.toLowerCase();
        String newKey = newName.toLowerCase();

        Kit kit = kits.get(oldKey);
        if (kit == null) return "Kit '" + oldName + "' not found.";
        if (kits.containsKey(newKey)) return "A kit named '" + newName + "' already exists.";
        if (!newName.matches("[A-Za-z0-9_-]+")) return "Name may only contain letters, digits, underscore and dash.";

        kits.remove(oldKey);
        kit.setName(newName);
        kits.put(newKey, kit);
        saveKits();
        return null;
    }

    public boolean deleteKit(String name) {
        Kit removed = kits.remove(name.toLowerCase());
        if (removed == null) return false;
        saveKits();
        return true;
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    public List<String> getKitNames() {
        List<String> names = new ArrayList<>();
        for (Kit kit : kits.values()) {
            names.add(kit.getName());
        }
        return names;
    }

    public void updateKit(Kit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
        saveKits();
    }

    /**
     * Serializes items using the Paper API (survives MC version upgrades, unlike Java
     * serialization). Null slots are represented as AIR items since the Paper array
     * (de)serialization does not accept null elements; they are converted back to null
     * on load so slot positions are preserved.
     */
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
            // Fall back to the legacy Java-serialization format for kits.yml files
            // written before the switch to Paper's item (de)serialization.
            return deserializeItemStacksLegacy(data);
        }
    }

    private ItemStack[] deserializeItemStacksLegacy(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize items (legacy fallback also failed)", e);
            return null;
        }
    }
}
