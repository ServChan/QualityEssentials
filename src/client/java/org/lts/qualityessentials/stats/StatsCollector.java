package org.lts.qualityessentials.stats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;

public final class StatsCollector {
    private static final int FRAMES = 240;
    private static final long[] FRAME_NANOS = new long[FRAMES];
    private static final TpsEstimator TPS = new TpsEstimator();
    private static int frameIndex;
    private static int frameCount;
    private static long lastFrame;
    private static ClientLevel lastLevel;
    private static int cachedLows;
    private static long lowsComputedAt;

    private StatsCollector() {
    }

    public static void onFrame() {
        long now = Util.getNanos();
        if (lastFrame != 0) {
            long delta = now - lastFrame;
            if (delta > 0 && delta < 2_000_000_000L) {
                FRAME_NANOS[frameIndex] = delta;
                frameIndex = (frameIndex + 1) % FRAMES;
                frameCount = Math.min(FRAMES, frameCount + 1);
            }
        }
        lastFrame = now;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            lastLevel = level;
            TPS.reset();
        }
    }

    public static void onTimePacket(long gameTime) {
        TPS.sample(gameTime, Util.getNanos());
    }

    public static double estimatedServerTps() {
        return TPS.tps(Util.getNanos());
    }

    public static int onePercentLow() {
        long now = Util.getNanos();
        if (now - lowsComputedAt > 500_000_000L) {
            cachedLows = StatFormat.percentile99Fps(FRAME_NANOS, frameCount);
            lowsComputedAt = now;
        }
        return cachedLows;
    }
}
