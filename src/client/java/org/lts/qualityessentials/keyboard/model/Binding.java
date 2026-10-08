package org.lts.qualityessentials.keyboard.model;

public record Binding(String name, String category, boolean mouse, int code, int modifiers, boolean unbound) {
    public static final int CONTROL = 1;
    public static final int SHIFT = 2;
    public static final int ALT = 4;
    public static final int ALL = CONTROL | SHIFT | ALT;

    public Binding {
        name = name == null ? "" : name;
        category = category == null ? "" : category;
        modifiers = modifiers & ALL;
    }

    public boolean sameKey(Binding other) {
        return !unbound && !other.unbound && mouse == other.mouse && code == other.code;
    }

    public boolean onKey(boolean mouseKey, int keyCode) {
        return !unbound && mouse == mouseKey && code == keyCode;
    }

    public boolean visibleOnLayer(int layer) {
        if (layer == 0) {
            return modifiers == 0;
        }
        return (modifiers & layer) == layer;
    }
}
