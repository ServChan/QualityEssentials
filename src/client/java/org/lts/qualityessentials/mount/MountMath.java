package org.lts.qualityessentials.mount;

public final class MountMath {
    private MountMath() {
    }

    public static double smooth(double previous, double sample) {
        if (sample < 0.05) {
            return previous < 0.2 ? 0 : previous * 0.5;
        }
        return previous * 0.7 + sample * 0.3;
    }
}
