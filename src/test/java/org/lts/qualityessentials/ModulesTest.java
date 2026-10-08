package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.ModuleRules;
import org.lts.qualityessentials.core.QEConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulesTest {
    @Test
    void modulesYieldToTheOriginalMods() {
        Map<Module, String> blockers = ModuleRules.blockers(Set.of("appleskin", "jade", "sodium", "bindboard"));
        assertEquals("appleskin", blockers.get(Module.FOOD));
        assertEquals("jade", blockers.get(Module.LOOK_AT));
        assertEquals("bindboard", blockers.get(Module.KEYBOARD));
        assertNull(blockers.get(Module.EFFECTS));
        assertFalse(ModuleRules.active(Module.FOOD, true, blockers));
        assertTrue(ModuleRules.active(Module.EFFECTS, true, blockers));
        assertFalse(ModuleRules.active(Module.EFFECTS, false, blockers));
    }

    @Test
    void cleanPackRunsEverything() {
        Map<Module, String> blockers = ModuleRules.blockers(Set.of("sodium", "iris", "voxy", "infomod"));
        for (Module module : Module.values()) {
            assertTrue(ModuleRules.active(module, true, blockers), module.id());
        }
    }

    @Test
    void wthitAlsoBlocksLookAt() {
        assertEquals("wthit", ModuleRules.blocker(Module.LOOK_AT, Set.of("wthit")));
    }

    @Test
    void configDefaultsEnableEveryModule() {
        QEConfig config = new QEConfig().sanitize();
        for (Module module : Module.values()) {
            assertTrue(config.enabled(module), module.id());
        }
        assertEquals(QEConfig.EquipmentAnchor.LEFT_CENTER, config.anchor());
        assertEquals(QEConfig.DarkPreset.SLATE, config.darkPreset());
    }

    @Test
    void configSanitizesUnknownValuesAndRoundTrips(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("qualityessentials.json");
        Files.writeString(file, "{\"modules\":{\"food\":false,\"unknown\":true},\"equipmentAnchor\":\"NOPE\",\"darkScreenPreset\":\"black\"}");
        QEConfig config = QEConfig.read(file);
        assertFalse(config.enabled(Module.FOOD));
        assertTrue(config.enabled(Module.LOOK_AT));
        assertFalse(config.modules.containsKey("unknown"));
        assertEquals(QEConfig.EquipmentAnchor.LEFT_CENTER, config.anchor());
        assertEquals(QEConfig.DarkPreset.BLACK, config.darkPreset());
        config.setEnabled(Module.KEYBOARD, false);
        assertTrue(QEConfig.write(file, config));
        QEConfig again = QEConfig.read(file);
        assertFalse(again.enabled(Module.KEYBOARD));
        assertFalse(again.enabled(Module.FOOD));
    }

    @Test
    void brokenConfigFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("qualityessentials.json");
        Files.writeString(file, "{ not json");
        QEConfig config = QEConfig.read(file);
        assertTrue(config.enabled(Module.FOOD));
        assertTrue(Files.exists(dir.resolve("qualityessentials.json.broken")));
    }

    @Test
    void cyclesWrapAround() {
        assertEquals(QEConfig.EquipmentAnchor.LEFT_CENTER, QEConfig.EquipmentAnchor.TOP_LEFT.next());
        assertEquals(QEConfig.DarkPreset.SLATE, QEConfig.DarkPreset.GRAPHITE.next());
    }
}
