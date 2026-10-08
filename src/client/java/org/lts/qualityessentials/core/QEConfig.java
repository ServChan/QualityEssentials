package org.lts.qualityessentials.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QEConfig {
    public static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int configVersion = CURRENT_VERSION;
    public Map<String, Boolean> modules = new LinkedHashMap<>();

    public boolean foodSaturationOverlay = true;
    public boolean foodHeldPreview = true;
    public boolean foodTooltip = true;

    public boolean lookAtShowMod = true;
    public boolean lookAtShowHealth = true;
    public boolean lookAtShowStates = true;
    public boolean lookAtShowHarvest = true;
    public boolean lookAtShowProgress = true;

    public boolean effectsShowTimer = true;
    public boolean effectsShowLevel = true;

    public String equipmentAnchor = EquipmentAnchor.LEFT_CENTER.name();
    public boolean equipmentShowPercent = false;
    public boolean equipmentShowAmmo = true;
    public boolean equipmentShowStackTotals = true;

    public String darkScreenPreset = DarkPreset.SLATE.name();

    public boolean keyboardControlsButton = true;

    public QEConfig() {
        for (Module module : Module.values()) {
            modules.put(module.id(), true);
        }
    }

    public boolean enabled(Module module) {
        Boolean value = modules.get(module.id());
        return value == null || value;
    }

    public void setEnabled(Module module, boolean value) {
        modules.put(module.id(), value);
    }

    public EquipmentAnchor anchor() {
        return EquipmentAnchor.parse(equipmentAnchor);
    }

    public DarkPreset darkPreset() {
        return DarkPreset.parse(darkScreenPreset);
    }

    public QEConfig copy() {
        return GSON.fromJson(GSON.toJson(this), QEConfig.class).sanitize();
    }

    public QEConfig sanitize() {
        if (modules == null) {
            modules = new LinkedHashMap<>();
        }
        Map<String, Boolean> cleaned = new LinkedHashMap<>();
        for (Module module : Module.values()) {
            Boolean value = modules.get(module.id());
            cleaned.put(module.id(), value == null || value);
        }
        modules = cleaned;
        equipmentAnchor = EquipmentAnchor.parse(equipmentAnchor).name();
        darkScreenPreset = DarkPreset.parse(darkScreenPreset).name();
        configVersion = CURRENT_VERSION;
        return this;
    }

    public static QEConfig read(Path path) {
        if (!Files.isRegularFile(path)) {
            return new QEConfig().sanitize();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            QEConfig loaded = GSON.fromJson(reader, QEConfig.class);
            return loaded == null ? new QEConfig().sanitize() : loaded.sanitize();
        } catch (Exception exception) {
            Path broken = path.resolveSibling(path.getFileName() + ".broken");
            try {
                Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
            return new QEConfig().sanitize();
        }
    }

    public static boolean write(Path path, QEConfig config) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception exception) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public enum EquipmentAnchor {
        LEFT_CENTER,
        HOTBAR_RIGHT,
        TOP_LEFT;

        static EquipmentAnchor parse(String value) {
            for (EquipmentAnchor anchor : values()) {
                if (anchor.name().equalsIgnoreCase(value)) {
                    return anchor;
                }
            }
            return LEFT_CENTER;
        }

        public EquipmentAnchor next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum DarkPreset {
        SLATE(0x0D141F),
        BLACK(0x000000),
        GRAPHITE(0x1A1C20);

        private final int rgb;

        DarkPreset(int rgb) {
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }

        static DarkPreset parse(String value) {
            for (DarkPreset preset : values()) {
                if (preset.name().equalsIgnoreCase(value)) {
                    return preset;
                }
            }
            return SLATE;
        }

        public DarkPreset next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
