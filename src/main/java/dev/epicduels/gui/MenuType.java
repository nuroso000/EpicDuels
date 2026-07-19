package dev.epicduels.gui;

/**
 * Identifies which plugin menu an inventory belongs to. Stored in the
 * inventory's {@link MenuHolder} so GUIs are recognized by their holder
 * instead of their (purely cosmetic) title string.
 */
public enum MenuType {
    MAIN,
    DUELS,
    STATS,
    MATCHMAKING,
    KIT_SELECT,
    ROUNDS_SELECT,
    ARENA_SELECT,
    KIT_EDIT,
    KIT_PREVIEW,
    KIT_CUSTOMIZE,
    KIT_LIST,
    KIT_EDITOR_LIST,
    ARENA_LIST,
    PARTY_MODE,
    PARTY_TEAM_SIZE,
    PARTY_KIT,
    PARTY_CONFIRM,
    SPECTATE
}
