package dev.epicduels.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A player's personalized arrangement of a kit's items: same items as the
 * base kit, but in the slots the player prefers. Stored per player and kit
 * by the PlayerKitManager.
 */
public class KitLayout {

    private final ItemStack[] contents;          // 36 main inventory slots
    private final @Nullable ItemStack[] armor;   // boots, leggings, chestplate, helmet
    private final @Nullable ItemStack offHand;

    public KitLayout(ItemStack[] contents, @Nullable ItemStack[] armor, @Nullable ItemStack offHand) {
        this.contents = contents;
        this.armor = armor;
        this.offHand = offHand;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public @Nullable ItemStack[] getArmor() {
        return armor;
    }

    public @Nullable ItemStack getOffHand() {
        return offHand;
    }
}
