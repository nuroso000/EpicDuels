package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.gui.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Decorates existing inventories without replacing their holder or domain PDC tags. */
public final class MenuPresentationManager {
    private final EpicDuels plugin;
    private YamlConfiguration config;
    private final Set<String> warnings = new HashSet<>();
    public MenuPresentationManager(EpicDuels plugin) { this.plugin = plugin; reload(); }
    public void reload() { config = PresentationConfig.load(plugin, "menus.yml", config); warnings.clear(); }
    private String path(MenuType type) { return "menus." + type.name().toLowerCase(Locale.ROOT).replace('_', '-'); }
    private void warn(String key, String reason) {
        if (warnings.add(key)) plugin.getLogger().warning("menus.yml: " + key + ": " + reason + "; using original layout/value.");
    }
    public int size(MenuType type, int original) {
        String key = path(type) + ".size";
        int size = config.getInt(key, original);
        // Pagination and kit storage have fixed physical slot contracts.
        if (size < original || size > 54 || size % 9 != 0 || (original == 54 && size != 54)) {
            warn(key, "size must be a multiple of 9 between " + original + " and 54"); return original;
        }
        return size;
    }
    public Component title(MenuType type, String context, Component original) {
        String key = path(type) + ".title";
        if (!config.isString(key)) return original;
        try { return PresentationText.render(config.getString(key), Map.of("kit", context == null ? "" : context)); }
        catch (RuntimeException error) { warn(key, "invalid title"); return original; }
    }
    public void apply(Inventory inventory) {
        MenuHolder holder = (MenuHolder) inventory.getHolder(false);
        MenuType type = holder.getType();
        if (type == MenuType.KIT_PREVIEW) return; // Actual equipment must not be decorated.
        boolean editor = type == MenuType.KIT_EDIT || type == MenuType.KIT_CUSTOMIZE;
        ItemStack[] original = inventory.getContents();
        Map<Integer, Integer> destinations = new HashMap<>();
        boolean paginated = switch (type) {
            case DUELS, MATCHMAKING, KIT_SELECT, ARENA_SELECT, KIT_LIST,
                 KIT_EDITOR_LIST, ARENA_LIST, SPECTATE, PARTY_KIT -> true;
            default -> false;
        };
        for (int slot = editor ? 41 : 0; slot < original.length; slot++) {
            ItemStack item = original[slot];
            if (item == null || !item.hasItemMeta() || Component.empty().equals(item.getItemMeta().displayName())) continue;
            String key = path(type) + ".buttons.slot-" + slot;
            int target = config.getInt(key + ".slot", slot);
            destinations.put(slot, target);
        }
        boolean valid = MenuLayout.valid(editor, paginated, inventory.getSize(), destinations);
        // Reserve every potential content and navigation position, including currently empty entries.
        for (var entry : destinations.entrySet()) {
            int target = entry.getValue();
            if (target >= 0 && target < original.length && target != entry.getKey() && !destinations.containsKey(target)
                    && original[target] != null && !Component.empty().equals(original[target].getItemMeta().displayName())) valid = false;
        }
        if (!valid) warn(path(type), "duplicate or out-of-range button slots");
        // Clear all source positions before placing moved buttons, allowing safe swaps.
        for (int slot : destinations.keySet()) inventory.setItem(slot, null);
        for (var entry : destinations.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = original[slot].clone();
            String key = path(type) + ".buttons.slot-" + slot;
            if (!config.getBoolean(key + ".visible", true)) continue;
            var meta = item.getItemMeta();
            Map<String, String> values = new HashMap<>();
            values.put("name", meta.displayName() == null ? "" : PlainTextComponentSerializer.plainText().serialize(meta.displayName()));
            values.put("kit", Optional.ofNullable(MenuKeys.tag(item, MenuKeys.KIT)).orElse(Optional.ofNullable(holder.getContext()).orElse("")));
            values.put("arena", Optional.ofNullable(MenuKeys.tag(item, MenuKeys.ARENA)).orElse(""));
            if (config.contains(key + ".material")) {
                Material material = Material.matchMaterial(config.getString(key + ".material", ""));
                if (material == null || !material.isItem() || material.isAir()) warn(key, "invalid item material");
                else {
                    var previousMeta = meta;
                    item.setType(material);
                    meta = item.getItemMeta();
                    previousMeta.getPersistentDataContainer().copyTo(meta.getPersistentDataContainer(), true);
                }
            }
            try {
                if (config.isString(key + ".name")) meta.displayName(PresentationText.render(config.getString(key + ".name"), values).decoration(TextDecoration.ITALIC, false));
                if (config.isList(key + ".lore")) meta.lore(config.getStringList(key + ".lore").stream().map(line -> PresentationText.render(line, values).decoration(TextDecoration.ITALIC, false)).toList());
            } catch (RuntimeException error) { warn(key, "invalid text template"); }
            item.setItemMeta(meta);
            MenuKeys.tag(item, MenuKeys.BUTTON, "slot-" + slot);
            inventory.setItem(valid ? entry.getValue() : slot, item);
        }
    }
    /** Resolve the immutable ID stamped when the inventory was opened, even after reload. */
    public static int actionSlot(ItemStack item) {
        String id = MenuKeys.tag(item, MenuKeys.BUTTON);
        if (id == null || !id.startsWith("slot-")) return -1;
        try { return Integer.parseInt(id.substring(5)); } catch (NumberFormatException error) { return -1; }
    }
}
