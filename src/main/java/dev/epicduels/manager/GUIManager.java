package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GUIManager {

    public static final String MAIN_MENU_TITLE = "EpicDuels Menu";
    public static final String ARENA_SELECT_TITLE = "Select Arena";
    public static final String KIT_SELECT_TITLE = "Select Kit";
    public static final String KIT_EDIT_TITLE = "Edit Kit: ";
    public static final String KIT_PREVIEW_TITLE = "Preview Kit: ";
    public static final String PLAYER_SELECT_TITLE = "Challenge Player";

    private final EpicDuels plugin;

    // Track GUI state for challenge flow: player -> target UUID
    private final Map<UUID, UUID> challengeTarget = new HashMap<>();
    // Track GUI state: player -> selected arena
    private final Map<UUID, String> challengeArena = new HashMap<>();

    public GUIManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(MAIN_MENU_TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "&aCh&aallenge Player", "&7Challenge another player to a duel"));
        inv.setItem(12, createItem(Material.BOOK, "&6My Stats", "&7View your duel statistics"));
        inv.setItem(14, createItem(Material.CHEST, "&bKits", "&7View available kits"));
        inv.setItem(16, createItem(Material.GRASS_BLOCK, "&dArenas", "&7View available arenas"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openPlayerSelect(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(player);

        int size = Math.min(54, Math.max(27, ((onlinePlayers.size() / 9) + 1) * 9 + 18));
        Inventory inv = Bukkit.createInventory(null, size, Component.text(PLAYER_SELECT_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        int slot = 10;
        for (Player target : onlinePlayers) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to challenge", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaSelect(Player player, UUID targetPlayer) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);

        List<String> arenaNames = plugin.getArenaManager().getReadyArenaNames();
        int size = Math.max(27, ((arenaNames.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text(ARENA_SELECT_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        int slot = 10;
        for (String name : arenaNames) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(Material.GRASS_BLOCK, "&a" + name, "&7Click to select this arena"));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openKitSelect(Player player, UUID targetPlayer, String arenaName) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);
        challengeArena.put(player.getUniqueId(), arenaName);

        List<String> kitNames = plugin.getKitManager().getKitNames();
        int size = Math.max(27, ((kitNames.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text(KIT_SELECT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int slot = 10;
        for (String name : kitNames) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(Material.IRON_CHESTPLATE, "&b" + name, "&7Click to select this kit"));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openKitEdit(Player player, Kit kit) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(KIT_EDIT_TITLE + kit.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));

        // Place kit contents in top rows
        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        // Armor slots at row 5
        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) {
                    inv.setItem(36 + i, armor[i].clone());
                }
            }
        }

        // Offhand at slot 40
        if (kit.getOffHand() != null) {
            inv.setItem(40, kit.getOffHand().clone());
        }

        // Save button
        inv.setItem(53, createItem(Material.EMERALD, "&aSave Kit", "&7Click to save changes"));

        player.openInventory(inv);
    }

    public void openKitPreview(Player player, Kit kit) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(KIT_PREVIEW_TITLE + kit.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Place kit contents
        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) {
                inv.setItem(i + 9, contents[i].clone()); // offset for border
            }
        }

        // Armor display
        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) {
                    inv.setItem(45 + i, armor[i].clone());
                }
            }
        }

        if (kit.getOffHand() != null) {
            inv.setItem(49, kit.getOffHand().clone());
        }

        player.openInventory(inv);
    }

    public void openKitList(Player player) {
        List<String> kitNames = plugin.getKitManager().getKitNames();
        int size = Math.max(27, ((kitNames.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Kits", NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int slot = 10;
        for (String name : kitNames) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(Material.IRON_CHESTPLATE, "&b" + name, "&7Click to preview"));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaList(Player player) {
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
        int size = Math.max(27, ((arenas.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Arenas", NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        int slot = 10;
        for (Arena arena : arenas) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            String status = arena.isReady() ? "&aReady" : "&cIncomplete";
            inv.setItem(slot, createItem(Material.GRASS_BLOCK, "&a" + arena.getName(), status));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public UUID getChallengeTarget(UUID player) {
        return challengeTarget.get(player);
    }

    public String getChallengeArena(UUID player) {
        return challengeArena.get(player);
    }

    public void clearChallengeData(UUID player) {
        challengeTarget.remove(player);
        challengeArena.remove(player);
    }

    private void fillBorder(Inventory inv, Material pane) {
        ItemStack border = new ItemStack(pane);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        int size = inv.getSize();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border.clone());
        }
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, border.clone());
        }
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, border.clone());
            inv.setItem(i + 8, border.clone());
        }
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name).decoration(TextDecoration.ITALIC, false));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(List.of(colorize(lore).decoration(TextDecoration.ITALIC, false)));
        }
        item.setItemMeta(meta);
        return item;
    }

    private Component colorize(String text) {
        // Simple color code parser for &-codes
        NamedTextColor color = NamedTextColor.WHITE;
        StringBuilder result = new StringBuilder();
        Component component = Component.empty();

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                if (!result.isEmpty()) {
                    component = component.append(Component.text(result.toString(), color));
                    result = new StringBuilder();
                }
                char code = text.charAt(i + 1);
                color = switch (code) {
                    case '0' -> NamedTextColor.BLACK;
                    case '1' -> NamedTextColor.DARK_BLUE;
                    case '2' -> NamedTextColor.DARK_GREEN;
                    case '3' -> NamedTextColor.DARK_AQUA;
                    case '4' -> NamedTextColor.DARK_RED;
                    case '5' -> NamedTextColor.DARK_PURPLE;
                    case '6' -> NamedTextColor.GOLD;
                    case '7' -> NamedTextColor.GRAY;
                    case '8' -> NamedTextColor.DARK_GRAY;
                    case '9' -> NamedTextColor.BLUE;
                    case 'a' -> NamedTextColor.GREEN;
                    case 'b' -> NamedTextColor.AQUA;
                    case 'c' -> NamedTextColor.RED;
                    case 'd' -> NamedTextColor.LIGHT_PURPLE;
                    case 'e' -> NamedTextColor.YELLOW;
                    case 'f' -> NamedTextColor.WHITE;
                    default -> color;
                };
                i++;
            } else {
                result.append(text.charAt(i));
            }
        }
        if (!result.isEmpty()) {
            component = component.append(Component.text(result.toString(), color));
        }
        return component;
    }
}
