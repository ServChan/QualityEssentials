package org.lts.qualityessentials.lookat;

import java.util.Locale;
import java.util.Set;

public final class LookAtFormat {
    public static final Set<String> STATE_PROPERTIES = Set.of("age", "power", "honey_level", "level", "bites", "charges",
            "note", "delay", "moisture", "pickles", "eggs", "candles", "layers", "flower_amount", "segment_amount", "dusted");

    private LookAtFormat() {
    }

    public static boolean shows(String property) {
        return STATE_PROPERTIES.contains(property);
    }

    public static int percent(int value, int min, int max) {
        if (max <= min) {
            return 100;
        }
        return Math.round((value - min) * 100f / (max - min));
    }

    public static String hearts(float health, float max) {
        return String.format(Locale.ROOT, "%.1f / %.1f", Math.max(0f, health), max);
    }

    public static double horseSpeed(double movementAttribute) {
        return movementAttribute * 42.16;
    }

    public static double horseJumpHeight(double strength) {
        return -0.1817584952 * strength * strength * strength + 3.689713992 * strength * strength
                + 2.128599134 * strength - 0.343930367;
    }

    public static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
