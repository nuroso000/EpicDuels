package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Kit;
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

    private String serializeItemStacks(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize items", e);
            return "";
        }
    }

    private ItemStack[] deserializeItemStacks(String data) {
        if (data == null || data.isEmpty()) return null;
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
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize items", e);
            return null;
        }
    }
}
