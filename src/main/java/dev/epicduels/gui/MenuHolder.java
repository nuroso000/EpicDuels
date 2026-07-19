package dev.epicduels.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder attached to every plugin GUI. Carries the menu identity
 * ({@link MenuType}), the current page for paginated menus and an optional
 * context string (the kit name for the kit edit/preview/customize GUIs) so
 * no data has to be parsed back out of titles or item display names.
 */
public class MenuHolder implements InventoryHolder {

    private final MenuType type;
    private final String context;
    private final int page;
    private Inventory inventory;

    public MenuHolder(MenuType type) {
        this(type, null, 0);
    }

    public MenuHolder(MenuType type, int page) {
        this(type, null, page);
    }

    public MenuHolder(MenuType type, String context, int page) {
        this.type = type;
        this.context = context;
        this.page = page;
    }

    public MenuType getType() {
        return type;
    }

    /** Kit name for the KIT_EDIT / KIT_PREVIEW / KIT_CUSTOMIZE menus, else null. */
    public String getContext() {
        return context;
    }

    public int getPage() {
        return page;
    }

    /** Creates the backing inventory and links it to this holder. */
    public static Inventory createInventory(MenuHolder holder, int size, Component title) {
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.inventory = inv;
        return inv;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
