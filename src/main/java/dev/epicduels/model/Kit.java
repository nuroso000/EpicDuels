package dev.epicduels.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Kit {

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
