package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.ModuleRules;
import org.lts.qualityessentials.mount.MountMath;
import org.lts.qualityessentials.newitems.FreshSlots;
import org.lts.qualityessentials.shulker.ShulkerContents;
import org.lts.qualityessentials.stats.StatFormat;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewModulesTest {
    @Test
    void pickedUpItemsBecomeFreshUntilSeen() {
        FreshSlots<String> slots = new FreshSlots<>(4);
        slots.snapshot(new String[]{"dirt", null, null, null}, new int[]{10, 0, 0, 0});
        slots.update(new String[]{"dirt", "iron", null, null}, new int[]{12, 3, 0, 0}, true);
        assertTrue(slots.isFresh(0));
        assertTrue(slots.isFresh(1));
        assertFalse(slots.isFresh(2));
        slots.seen(0);
        assertFalse(slots.isFresh(0));
        assertEquals(1, slots.freshCount());
    }

    @Test
    void movingItemsAroundDoesNotMarkThem() {
        FreshSlots<String> slots = new FreshSlots<>(3);
        slots.snapshot(new String[]{"sword", null, "bread"}, new int[]{1, 0, 5});
        slots.update(new String[]{null, "sword", "bread"}, new int[]{0, 1, 5}, true);
        assertEquals(0, slots.freshCount());
        slots.update(new String[]{null, "sword", "bread"}, new int[]{0, 1, 4}, true);
        assertEquals(0, slots.freshCount());
    }

    @Test
    void itemsGainedWhileMarkingIsOffStayPlain() {
        FreshSlots<String> slots = new FreshSlots<>(2);
        slots.snapshot(new String[]{null, null}, new int[]{0, 0});
        slots.update(new String[]{"stone", null}, new int[]{64, 0}, false);
        assertEquals(0, slots.freshCount());
    }

    @Test
    void freshMarkDropsWhenTheSlotChanges() {
        FreshSlots<String> slots = new FreshSlots<>(2);
        slots.snapshot(new String[]{null, null}, new int[]{0, 0});
        slots.update(new String[]{"apple", null}, new int[]{1, 0}, true);
        assertTrue(slots.isFresh(0));
        slots.update(new String[]{null, null}, new int[]{0, 0}, true);
        assertFalse(slots.isFresh(0));
    }

    @Test
    void shulkerGridKeepsBoxShapeAndMergesInSummary() {
        assertEquals(27, ShulkerContents.gridSize(3, 2, true));
        assertEquals(9, ShulkerContents.gridSize(0, 4, false));
        assertEquals(18, ShulkerContents.gridSize(0, 9, false));
        List<ShulkerContents.Group<String>> groups = ShulkerContents.merge(List.of("iron", "dirt", "iron", "iron"), s -> 64, String::equals);
        assertEquals("iron", groups.get(0).item);
        assertEquals(192, groups.get(0).count);
        assertEquals(64, groups.get(1).count);
        assertEquals("64", ShulkerContents.countLabel(64));
        assertEquals("1.7k", ShulkerContents.countLabel(1728));
        assertEquals("2k", ShulkerContents.countLabel(2000));
    }

    @Test
    void clockAndSleepWindow() {
        assertEquals("06:00", StatFormat.clock(0));
        assertEquals("12:00", StatFormat.clock(6000));
        assertEquals("00:00", StatFormat.clock(18000));
        assertEquals("14:30", StatFormat.clock(8500));
        assertEquals(1, StatFormat.day(23999));
        assertEquals(12, StatFormat.day(11 * 24000 + 100));
        assertFalse(StatFormat.canSleep(6000, false));
        assertTrue(StatFormat.canSleep(6000, true));
        assertTrue(StatFormat.canSleep(18000, false));
        assertTrue(StatFormat.canSleep(5 * 24000 + 13000, false));
    }

    @Test
    void mountSpeedSettlesToZeroWhenStopped() {
        double speed = 0;
        for (int i = 0; i < 30; i++) {
            speed = MountMath.smooth(speed, 10);
        }
        assertEquals(10, speed, 0.01);
        for (int i = 0; i < 10; i++) {
            speed = MountMath.smooth(speed, 0);
        }
        assertEquals(0, speed);
    }

    @Test
    void newModulesYieldToTheirOriginals() {
        assertEquals("shulkerboxtooltip", ModuleRules.blocker(Module.SHULKER, Set.of("shulkerboxtooltip")));
        assertEquals("bettermounthud", ModuleRules.blocker(Module.MOUNT, Set.of("bettermounthud")));
        assertNull(ModuleRules.blocker(Module.SCREENSHOTS, Set.of("shulkerboxtooltip", "bettermounthud")));
    }
}
