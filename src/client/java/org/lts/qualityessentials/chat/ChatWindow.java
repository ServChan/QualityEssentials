package org.lts.qualityessentials.chat;

import org.lts.qualityessentials.core.QEConfig;

public final class ChatWindow {
    static final int MIN_WIDTH = 40;
    static final int MIN_HEIGHT = 20;
    static final int SIDE_MARGIN = 14;
    static final int BOTTOM_MARGIN = 40;
    static final int TOP_MARGIN = 12;

    private ChatWindow() {
    }

    public static int width(int vanilla, double pct, QEConfig.ChatSize size, int guiWidth) {
        return scale(vanilla, pct, size, MIN_WIDTH, guiWidth - SIDE_MARGIN);
    }

    public static int height(int vanilla, double pct, QEConfig.ChatSize size, int guiHeight) {
        return scale(vanilla, pct, size, MIN_HEIGHT, guiHeight - BOTTOM_MARGIN - TOP_MARGIN);
    }

    private static int scale(int vanilla, double pct, QEConfig.ChatSize size, int min, int max) {
        if (size == QEConfig.ChatSize.NORMAL || max <= vanilla) {
            return vanilla;
        }
        if (size == QEConfig.ChatSize.FULL) {
            double clamped = Math.max(0.0, Math.min(1.0, pct));
            return Math.max(vanilla, (int) Math.floor(min + clamped * (max - min)));
        }
        return Math.min(max, (int) Math.round(vanilla * size.factor()));
    }
}
