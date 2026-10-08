package org.lts.qualityessentials.keyboard;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.keyboard.model.Binding;
import org.lts.qualityessentials.keyboard.model.CategoryColors;
import org.lts.qualityessentials.keyboard.model.ConflictAnalyzer;
import org.lts.qualityessentials.keyboard.model.ConflictAnalyzer.Level;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConflictAnalyzerTest {
    private static Binding key(String name, int code, int modifiers) {
        return new Binding(name, "minecraft:misc", false, code, modifiers, false);
    }

    @Test
    void sameKeyWithoutModifiersIsHard() {
        assertEquals(Level.HARD, ConflictAnalyzer.between(key("a", 14, 0), key("b", 14, 0)));
    }

    @Test
    void differentKeysOrUnboundNeverConflict() {
        assertEquals(Level.NONE, ConflictAnalyzer.between(key("a", 14, 0), key("b", 15, 0)));
        Binding unbound = new Binding("c", "", false, 14, 0, true);
        assertEquals(Level.NONE, ConflictAnalyzer.between(key("a", 14, 0), unbound));
        Binding mouse = new Binding("m", "", true, 14, 0, false);
        assertEquals(Level.NONE, ConflictAnalyzer.between(key("a", 14, 0), mouse));
    }

    @Test
    void modifierCombosFollowLitematicaMatching() {
        assertEquals(Level.SOFT, ConflictAnalyzer.between(key("plain", 14, 0), key("ctrl", 14, Binding.CONTROL)));
        assertEquals(Level.HARD, ConflictAnalyzer.between(key("ctrl1", 14, Binding.CONTROL), key("ctrl2", 14, Binding.CONTROL)));
        assertEquals(Level.SOFT, ConflictAnalyzer.between(key("ctrl", 14, Binding.CONTROL),
                key("ctrlShift", 14, Binding.CONTROL | Binding.SHIFT)));
        assertEquals(Level.NONE, ConflictAnalyzer.between(key("ctrl", 14, Binding.CONTROL), key("shift", 14, Binding.SHIFT)));
    }

    @Test
    void holdActivatorsAreOnlySoft() {
        assertEquals(Level.SOFT, ConflictAnalyzer.between(key("key.saveToolbarActivator", 6, 0), key("zoomify.key.zoom", 6, 0)));
        Binding pick = new Binding("key.pickItem", "minecraft:gameplay", true, 2, 0, false);
        Binding spectator = new Binding("key.spectatorHotbar", "minecraft:spectator", true, 2, 0, false);
        assertEquals(Level.SOFT, ConflictAnalyzer.between(pick, spectator));
    }

    @Test
    void analyzeMarksWorstLevelAndPartners() {
        List<Binding> bindings = List.of(
                key("emotes", 5, 0),
                key("waypoint", 5, 0),
                key("menu", 14, Binding.CONTROL),
                key("shaders", 14, 0),
                key("free", 20, 0));
        ConflictAnalyzer.Result result = ConflictAnalyzer.analyze(bindings);
        assertEquals(Level.HARD, result.levelOf(0));
        assertEquals(Level.HARD, result.levelOf(1));
        assertEquals(Level.SOFT, result.levelOf(2));
        assertEquals(Level.SOFT, result.levelOf(3));
        assertEquals(Level.NONE, result.levelOf(4));
        assertEquals(1, result.hardCount());
        assertEquals(1, result.softCount());
        assertEquals(List.of(1), result.partnersOf(0));
    }

    @Test
    void layersShowMatchingModifiers() {
        Binding plain = key("plain", 14, 0);
        Binding ctrlShift = key("cs", 14, Binding.CONTROL | Binding.SHIFT);
        assertTrue(plain.visibleOnLayer(0));
        assertTrue(ctrlShift.visibleOnLayer(Binding.CONTROL));
        assertTrue(ctrlShift.visibleOnLayer(Binding.SHIFT));
        assertTrue(!ctrlShift.visibleOnLayer(0) && !plain.visibleOnLayer(Binding.ALT));
    }

    @Test
    void categoryColorsAreStableAndOpaque() {
        assertEquals(CategoryColors.of("litematicaplus:main"), CategoryColors.of("litematicaplus:main"));
        assertNotEquals(CategoryColors.of("minecraft:movement"), CategoryColors.of("minecraft:inventory"));
        assertEquals(0xFF, CategoryColors.of("anything:else") >>> 24);
    }
}
