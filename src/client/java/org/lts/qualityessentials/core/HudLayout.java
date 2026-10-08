package org.lts.qualityessentials.core;

public final class HudLayout {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;
    public static final int EDGE = 4;

    public String anchor = Anchor.TOP_LEFT.name();
    public int alignX = -1;
    public int alignY = -1;
    public int offsetX = EDGE;
    public int offsetY = EDGE;
    public float scale = 1f;
    public float opacity = 0.85f;

    public HudLayout() {
    }

    public static HudLayout of(Anchor anchor, int alignX, int alignY, int offsetX, int offsetY) {
        HudLayout layout = new HudLayout();
        layout.anchor = anchor.name();
        layout.alignX = alignX;
        layout.alignY = alignY;
        layout.offsetX = offsetX;
        layout.offsetY = offsetY;
        return layout;
    }

    public HudLayout copy() {
        HudLayout copy = of(anchor(), alignX, alignY, offsetX, offsetY);
        copy.scale = scale;
        copy.opacity = opacity;
        return copy;
    }

    public HudLayout sanitize() {
        anchor = anchor().name();
        alignX = Math.max(-1, Math.min(1, alignX));
        alignY = Math.max(-1, Math.min(1, alignY));
        offsetX = Math.max(-4000, Math.min(4000, offsetX));
        offsetY = Math.max(-4000, Math.min(4000, offsetY));
        scale = clampScale(Float.isFinite(scale) ? scale : 1f);
        opacity = Math.max(0f, Math.min(1f, Float.isFinite(opacity) ? opacity : 0.85f));
        return this;
    }

    public static float clampScale(float value) {
        return Math.round(Math.max(MIN_SCALE, Math.min(MAX_SCALE, value)) * 20f) / 20f;
    }

    public Anchor anchor() {
        for (Anchor value : Anchor.values()) {
            if (value.name().equalsIgnoreCase(anchor)) {
                return value;
            }
        }
        return Anchor.TOP_LEFT;
    }

    public int[] position(int screenWidth, int screenHeight, int width, int height) {
        Anchor a = anchor();
        int ax = a.column * screenWidth / 2;
        int ay = a.row * screenHeight / 2;
        int x = ax + offsetX - (alignX + 1) * width / 2;
        int y = ay + offsetY - (alignY + 1) * height / 2;
        x = Math.max(0, Math.min(screenWidth - width, x));
        y = Math.max(0, Math.min(screenHeight - height, y));
        return new int[]{x, y};
    }

    public static HudLayout fromPosition(int screenWidth, int screenHeight, int x, int y, int width, int height, float scale, float opacity) {
        int column = region(x, width, screenWidth);
        int row = region(y, height, screenHeight);
        int[] horizontal = axis(column, x, width, screenWidth);
        int[] vertical = axis(row, y, height, screenHeight);
        HudLayout layout = of(Anchor.at(column, row), horizontal[0], vertical[0], horizontal[1], vertical[1]);
        layout.scale = clampScale(scale);
        layout.opacity = opacity;
        return layout;
    }

    private static int region(int start, int length, int size) {
        int band = size / 6;
        if (start <= band && start + length < size - band) {
            return 0;
        }
        if (start + length >= size - band && start > band) {
            return 2;
        }
        return 1;
    }

    private static int[] axis(int cell, int start, int length, int size) {
        int anchor = cell * size / 2;
        if (cell == 0) {
            return new int[]{-1, start - anchor};
        }
        if (cell == 2) {
            return new int[]{1, start + length - anchor};
        }
        if (start >= anchor) {
            return new int[]{-1, start - anchor};
        }
        if (start + length <= anchor) {
            return new int[]{1, start + length - anchor};
        }
        return new int[]{0, start + length / 2 - anchor};
    }

    public static int snap(int start, int length, int size, int threshold, int[] guide) {
        int[] targets = {EDGE, size - EDGE - length, size / 2 - length / 2};
        int[] guides = {EDGE, size - EDGE, size / 2};
        for (int i = 0; i < targets.length; i++) {
            if (Math.abs(start - targets[i]) <= threshold) {
                if (guide != null) {
                    guide[0] = guides[i];
                }
                return targets[i];
            }
        }
        if (guide != null) {
            guide[0] = -1;
        }
        return start;
    }

    public enum Anchor {
        TOP_LEFT(0, 0),
        TOP_CENTER(1, 0),
        TOP_RIGHT(2, 0),
        MIDDLE_LEFT(0, 1),
        CENTER(1, 1),
        MIDDLE_RIGHT(2, 1),
        BOTTOM_LEFT(0, 2),
        BOTTOM_CENTER(1, 2),
        BOTTOM_RIGHT(2, 2);

        public final int column;
        public final int row;

        Anchor(int column, int row) {
            this.column = column;
            this.row = row;
        }

        public static Anchor at(int column, int row) {
            for (Anchor anchor : values()) {
                if (anchor.column == column && anchor.row == row) {
                    return anchor;
                }
            }
            return TOP_LEFT;
        }
    }
}
