package org.lts.qualityessentials.stats;

import java.util.Locale;

public final class StatFormat {
    public static final int GOOD = 0xFF7CFC7C;
    public static final int OKAY = 0xFFFFE066;
    public static final int BAD = 0xFFFF6B6B;
    public static final int NEUTRAL = 0xFFE0E0E0;

    private StatFormat() {
    }

    public static int fpsColor(int fps) {
        return fps >= 60 ? GOOD : fps >= 30 ? OKAY : BAD;
    }

    public static int pingColor(int ping) {
        return ping <= 80 ? GOOD : ping <= 180 ? OKAY : BAD;
    }

    public static int tpsColor(double tps, double target) {
        double ratio = target <= 0 ? 1 : tps / target;
        return ratio >= 0.95 ? GOOD : ratio >= 0.75 ? OKAY : BAD;
    }

    public static int msptColor(double mspt, double budget) {
        return mspt <= budget * 0.8 ? GOOD : mspt <= budget ? OKAY : BAD;
    }

    public static int memoryColor(double fraction) {
        return fraction < 0.8 ? GOOD : fraction < 0.92 ? OKAY : BAD;
    }

    public static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String gigabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static int percentile99Fps(long[] frameNanos, int count) {
        if (count <= 0) {
            return 0;
        }
        long[] sorted = java.util.Arrays.copyOf(frameNanos, count);
        java.util.Arrays.sort(sorted);
        int index = Math.min(count - 1, (int) (count * 0.99));
        long worst = sorted[Math.max(0, index)];
        return worst <= 0 ? 0 : (int) Math.round(1_000_000_000.0 / worst);
    }
}
