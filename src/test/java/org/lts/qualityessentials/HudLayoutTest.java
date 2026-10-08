package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.HudLayout;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.stats.StatFormat;
import org.lts.qualityessentials.stats.TpsEstimator;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {
    @Test
    void defaultsPlaceElementsWhereExpected() {
        assertArrayEquals(new int[]{4, 4}, HudElement.STATS.defaultLayout().position(640, 360, 100, 12));
        assertArrayEquals(new int[]{270, 4}, HudElement.LOOK_AT.defaultLayout().position(640, 360, 100, 40));
        assertArrayEquals(new int[]{4, 150}, HudElement.EQUIPMENT.defaultLayout().position(640, 360, 50, 60));
    }

    @Test
    void positionsRoundTripOnEveryScreenRegion() {
        int width = 960;
        int height = 540;
        int[][] samples = {{4, 4}, {430, 10}, {900, 6}, {10, 250}, {440, 240}, {880, 260}, {6, 500}, {520, 497}, {850, 480}, {300, 497}};
        for (int[] sample : samples) {
            HudLayout layout = HudLayout.fromPosition(width, height, sample[0], sample[1], 50, 30, 1f, 0.5f);
            assertArrayEquals(sample, layout.position(width, height, 50, 30), layout.anchor + " " + sample[0] + "," + sample[1]);
        }
    }

    @Test
    void elementBesideHotbarKeepsItsInnerEdgeWhenItGrows() {
        HudLayout layout = HudLayout.fromPosition(640, 360, 417, 327, 60, 33, 1f, 0.5f);
        assertEquals(HudLayout.Anchor.BOTTOM_CENTER, layout.anchor());
        assertEquals(-1, layout.alignX);
        assertEquals(417, layout.position(640, 360, 60, 33)[0]);
        assertEquals(417, layout.position(640, 360, 140, 33)[0]);
        assertEquals(417, layout.position(1280, 720, 60, 33)[0] - 320);
    }

    @Test
    void positionsStayOnScreen() {
        HudLayout layout = HudLayout.of(HudLayout.Anchor.TOP_RIGHT, 1, -1, 300, -50);
        int[] position = layout.position(640, 360, 80, 20);
        assertEquals(560, position[0]);
        assertEquals(0, position[1]);
    }

    @Test
    void snappingPullsToEdgesAndCenter() {
        int[] guide = new int[1];
        assertEquals(4, HudLayout.snap(7, 40, 640, 5, guide));
        assertEquals(4, guide[0]);
        assertEquals(596, HudLayout.snap(593, 40, 640, 5, guide));
        assertEquals(300, HudLayout.snap(303, 40, 640, 5, guide));
        assertEquals(320, guide[0]);
        assertEquals(200, HudLayout.snap(200, 40, 640, 5, guide));
        assertEquals(-1, guide[0]);
    }

    @Test
    void scaleIsClampedAndRounded() {
        assertEquals(0.5f, HudLayout.clampScale(0.1f));
        assertEquals(2.0f, HudLayout.clampScale(9f));
        assertEquals(1.15f, HudLayout.clampScale(1.149f), 0.0001f);
    }

    @Test
    void oldHotbarAnchorMigratesNextToTheHotbar(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("qualityessentials.json");
        Files.writeString(file, "{\"configVersion\":1,\"equipmentAnchor\":\"HOTBAR_RIGHT\"}");
        QEConfig config = QEConfig.read(file);
        assertTrue(config.equipmentHorizontal);
        HudLayout layout = config.layout(HudElement.EQUIPMENT);
        assertEquals(HudLayout.Anchor.BOTTOM_CENTER, layout.anchor());
        assertEquals(320 + 97, layout.position(640, 360, 100, 33)[0]);
        assertEquals(QEConfig.CURRENT_VERSION, config.configVersion);
    }

    @Test
    void tpsEstimateFollowsServerTicks() {
        TpsEstimator estimator = new TpsEstimator();
        long second = 1_000_000_000L;
        estimator.sample(1000, 0);
        estimator.sample(1020, second);
        assertEquals(20.0, estimator.tps(second), 0.01);
        estimator.sample(1030, 2 * second);
        assertEquals(16.0, estimator.tps(2 * second), 0.01);
        estimator.sample(1031, 2 * second + 100_000_000L);
        assertEquals(16.0, estimator.tps(2 * second), 0.01);
        assertEquals(-1, estimator.tps(40 * second));
    }

    @Test
    void statColorsAndLows() {
        assertEquals(StatFormat.GOOD, StatFormat.fpsColor(144));
        assertEquals(StatFormat.BAD, StatFormat.fpsColor(20));
        assertEquals(StatFormat.OKAY, StatFormat.pingColor(120));
        assertEquals(StatFormat.BAD, StatFormat.tpsColor(12, 20));
        assertEquals(StatFormat.OKAY, StatFormat.msptColor(45, 50));
        long[] frames = new long[100];
        java.util.Arrays.fill(frames, 7_000_000L);
        frames[3] = 40_000_000L;
        assertEquals(25, StatFormat.percentile99Fps(frames, 100));
        assertEquals(0, StatFormat.percentile99Fps(frames, 0));
    }
}
