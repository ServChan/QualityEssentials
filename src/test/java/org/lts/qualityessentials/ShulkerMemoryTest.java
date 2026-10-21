package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.shulker.ShulkerMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShulkerMemoryTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final String BOX = "minecraft:shulker_box";

    @Test
    void entriesAreKeptPerDimensionAndPosition() {
        ShulkerMemory<String> memory = new ShulkerMemory<>();
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, 10L, BOX, "diamonds", 100L));
        memory.put(new ShulkerMemory.Entry<>(NETHER, 10L, BOX, "quartz", 100L));
        assertEquals("diamonds", memory.get(OVERWORLD, 10L).contents());
        assertEquals("quartz", memory.get(NETHER, 10L).contents());
        assertNull(memory.get(OVERWORLD, 11L));
    }

    @Test
    void olderDataDoesNotReplaceNewer() {
        ShulkerMemory<String> memory = new ShulkerMemory<>();
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, 1L, BOX, "now", 200L));
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, 1L, BOX, "from disk", 100L));
        assertEquals("now", memory.get(OVERWORLD, 1L).contents());
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, 1L, BOX, "later", 300L));
        assertEquals("later", memory.get(OVERWORLD, 1L).contents());
    }

    @Test
    void fullMemoryForgetsTheOldestBox() {
        ShulkerMemory<String> memory = new ShulkerMemory<>();
        for (int i = 0; i < ShulkerMemory.MAX_ENTRIES; i++) {
            memory.put(new ShulkerMemory.Entry<>(OVERWORLD, i, BOX, "box", 1000L + i));
        }
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, -1L, BOX, "new", 5_000_000L));
        assertEquals(ShulkerMemory.MAX_ENTRIES, memory.size());
        assertNull(memory.get(OVERWORLD, 0L));
        assertEquals("new", memory.get(OVERWORLD, -1L).contents());
    }

    @Test
    void removeForgetsTheBox() {
        ShulkerMemory<String> memory = new ShulkerMemory<>();
        memory.put(new ShulkerMemory.Entry<>(OVERWORLD, 5L, BOX, "box", 1L));
        assertTrue(memory.remove(OVERWORLD, 5L));
        assertFalse(memory.remove(OVERWORLD, 5L));
        assertNull(memory.get(OVERWORLD, 5L));
    }

    @Test
    void ageIsRoundedToReadableUnits() {
        long minute = 60_000L;
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.NOW, 0), ShulkerMemory.age(0, 59_000L));
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.MINUTES, 5), ShulkerMemory.age(0, 5 * minute + 10));
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.HOURS, 3), ShulkerMemory.age(0, 200 * minute));
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.HOURS, 47), ShulkerMemory.age(0, 47 * 60 * minute + 5));
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.DAYS, 3), ShulkerMemory.age(0, 3 * 24 * 60 * minute));
        assertEquals(new ShulkerMemory.Age(ShulkerMemory.Age.Unit.NOW, 0), ShulkerMemory.age(1000L, 0L));
    }
}
