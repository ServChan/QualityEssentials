package org.lts.qualityessentials.equipment;

public final class DurabilityFormat {
    private DurabilityFormat() {
    }

    public static String text(int max, int damage, boolean percent) {
        int left = Math.max(0, max - damage);
        if (percent) {
            return Math.round(fraction(max, damage) * 100f) + "%";
        }
        return Integer.toString(left);
    }

    public static float fraction(int max, int damage) {
        if (max <= 0) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, (max - damage) / (float) max));
    }

    public static int color(float fraction) {
        if (fraction > 0.5f) {
            return 0xFF7CFC7C;
        }
        if (fraction > 0.25f) {
            return 0xFFFFE066;
        }
        if (fraction > 0.1f) {
            return 0xFFFFA040;
        }
        return 0xFFFF5050;
    }

    public static String count(int count) {
        if (count >= 10000) {
            return (count / 1000) + "k";
        }
        return Integer.toString(count);
    }
}
