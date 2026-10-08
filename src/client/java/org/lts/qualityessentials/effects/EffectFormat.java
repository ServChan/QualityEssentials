package org.lts.qualityessentials.effects;

public final class EffectFormat {
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private EffectFormat() {
    }

    public static String duration(int ticks, boolean infinite) {
        if (infinite) {
            return "∞";
        }
        int seconds = Math.max(0, ticks / 20);
        if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        }
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int rest = seconds % 60;
            return minutes + ":" + (rest < 10 ? "0" : "") + rest;
        }
        return seconds + "s";
    }

    public static String level(int amplifier) {
        int level = amplifier + 1;
        if (level <= 1) {
            return "";
        }
        return level < ROMAN.length ? ROMAN[level] : Integer.toString(level);
    }

    public static int durationColor(int ticks, boolean infinite) {
        if (infinite || ticks > 600) {
            return 0xFFFFFFFF;
        }
        if (ticks > 200) {
            return 0xFFFFE066;
        }
        return 0xFFFF6B6B;
    }
}
