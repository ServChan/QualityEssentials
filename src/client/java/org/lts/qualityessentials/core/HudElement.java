package org.lts.qualityessentials.core;

import java.util.Locale;

public enum HudElement {
    LOOK_AT(Module.LOOK_AT, HudLayout.Anchor.TOP_CENTER, 0, -1, 0, 4),
    EQUIPMENT(Module.EQUIPMENT, HudLayout.Anchor.MIDDLE_LEFT, -1, 0, 4, 0),
    STATS(Module.STATS, HudLayout.Anchor.TOP_LEFT, -1, -1, 4, 4);

    private final Module module;
    private final HudLayout.Anchor anchor;
    private final int alignX;
    private final int alignY;
    private final int offsetX;
    private final int offsetY;

    HudElement(Module module, HudLayout.Anchor anchor, int alignX, int alignY, int offsetX, int offsetY) {
        this.module = module;
        this.anchor = anchor;
        this.alignX = alignX;
        this.alignY = alignY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public Module module() {
        return module;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public HudLayout defaultLayout() {
        HudLayout layout = HudLayout.of(anchor, alignX, alignY, offsetX, offsetY);
        if (this == EQUIPMENT) {
            layout.opacity = 0.6f;
        }
        return layout;
    }
}
