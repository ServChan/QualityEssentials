package org.lts.qualityessentials.core;

import java.util.List;
import java.util.Locale;

public enum Module {
    FOOD("AppleSkin", "appleskin"),
    LOOK_AT("Jade / WTHIT", "jade", "wthit", "waila", "hwyla"),
    EFFECTS("Status Effect Timer", "statuseffecttimer", "status-effect-timer", "status_effect_timer", "statuseffectbars",
            "status-effect-bars"),
    EQUIPMENT("Armor HUD", "armorhud", "armor-hud", "armor_hud", "armordurabilityhud", "armor-durability-hud", "durabilityhud"),
    MOUNT("Better Mount HUD", "bettermounthud", "better-mount-hud", "better_mount_hud"),
    STATS("FPS Display", "fpsdisplay", "fps-display", "fps_display"),
    SHULKER("Shulker Box Tooltip", "shulkerboxtooltip", "shulker-box-tooltip", "shulker_box_tooltip"),
    NEW_ITEMS("Item Highlighter", "itemhighlighter", "item-highlighter", "item_highlighter"),
    SCREENSHOTS(null),
    DARK_SCREEN("Dark Loading Screen", "dark-loading-screen", "darkloadingscreen", "dark_loading_screen"),
    KEYBOARD("BindBoard", "bindboard");

    private final String replaces;
    private final List<String> conflictingModIds;

    Module(String replaces, String... conflictingModIds) {
        this.replaces = replaces;
        this.conflictingModIds = List.of(conflictingModIds);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String replaces() {
        return replaces;
    }

    public List<String> conflictingModIds() {
        return conflictingModIds;
    }

    public String translationKey() {
        return "qualityessentials.module." + id();
    }
}
