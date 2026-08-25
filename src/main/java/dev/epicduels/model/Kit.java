package dev.epicduels.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Kit {

    // Sentinel kit name for "Own Inventory" (no-kit) duels: players fight with
    // what they carry. Contains spaces, so it can never collide with a real
    // kit name (those are created from single command arguments).
    public static final String OWN_INVENTORY = "__own inventory__";
    public static final String OWN_INVENTORY_DISPLAY = "Own Inventory";

    /** The name to show players — resolves the own-inventory sentinel. */
    public static String displayName(String kitName) {
        return OWN_INVENTORY.equals(kitName) ? OWN_INVENTORY_DISPLAY : kitName;
    }

    private String name;
    private ItemStack[] contents;
    private @Nullable ItemStack[] armorContents;
    private @Nullable ItemStack offHand;
    private @Nullable Material icon;

    public Kit(String name, ItemStack[] contents, @Nullable ItemStack[] armorContents, @Nullable ItemStack offHand) {
        this.name = name;
        this.contents = contents;
        this.armorContents = armorContents;
        this.offHand = offHand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public @Nullable ItemStack[] getArmorContents() {
        return armorContents;
    }

    public void setArmorContents(@Nullable ItemStack[] armorContents) {
        this.armorContents = armorContents;
    }

    public @Nullable ItemStack getOffHand() {
        return offHand;
    }

    public void setOffHand(@Nullable ItemStack offHand) {
        this.offHand = offHand;
    }

    public @Nullable Material getIcon() {
        return icon;
    }

    public void setIcon(@Nullable Material icon) {
        this.icon = icon;
    }

    public Material getDisplayIcon() {
        return icon != null ? icon : Material.CHEST;
    }
}
