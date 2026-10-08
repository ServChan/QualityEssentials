package org.lts.qualityessentials.stats;

public final class TpsEstimator {
    private static final long MIN_INTERVAL = 500_000_000L;
    private static final long MAX_INTERVAL = 10_000_000_000L;

    private long lastGameTime = Long.MIN_VALUE;
    private long lastNanos;
    private double tps = -1;
    private long lastSampleNanos;

    public void reset() {
        lastGameTime = Long.MIN_VALUE;
        tps = -1;
    }

    public void sample(long gameTime, long nanos) {
        if (lastGameTime == Long.MIN_VALUE) {
            lastGameTime = gameTime;
            lastNanos = nanos;
            return;
        }
        long realDelta = nanos - lastNanos;
        long gameDelta = gameTime - lastGameTime;
        if (realDelta < MIN_INTERVAL) {
            return;
        }
        lastGameTime = gameTime;
        lastNanos = nanos;
        if (realDelta > MAX_INTERVAL || gameDelta < 0) {
            return;
        }
        double measured = Math.min(20.0, gameDelta / (realDelta / 1_000_000_000.0));
        tps = tps < 0 ? measured : tps * 0.6 + measured * 0.4;
        lastSampleNanos = nanos;
    }

    public double tps(long now) {
        if (tps < 0 || now - lastSampleNanos > 15_000_000_000L) {
            return -1;
        }
        return tps;
    }
}
