package org.lts.qualityessentials.keyboard;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lts.qualityessentials.QualityEssentialsClient;

import java.lang.reflect.Method;

final class LitematicaPlusModifiers implements ModifierSource {
    private final Method entry;
    private final Method entryName;
    private final Method modifiers;
    private final Method setModifiers;

    private LitematicaPlusModifiers(Method entry, Method entryName, Method modifiers, Method setModifiers) {
        this.entry = entry;
        this.entryName = entryName;
        this.modifiers = modifiers;
        this.setModifiers = setModifiers;
    }

    static ModifierSource tryCreate() {
        if (!FabricLoader.getInstance().isModLoaded("litematicaplus")) {
            return null;
        }
        try {
            Class<?> hotkeys = Class.forName("org.lts.litematicaplus.Hotkeys");
            Class<?> entryClass = Class.forName("org.lts.litematicaplus.Hotkeys$Entry");
            return new LitematicaPlusModifiers(
                    hotkeys.getMethod("entry", KeyMapping.class),
                    entryClass.getMethod("name"),
                    hotkeys.getMethod("modifiers", String.class),
                    hotkeys.getMethod("setModifiers", String.class, int.class));
        } catch (ReflectiveOperationException | LinkageError exception) {
            QualityEssentialsClient.LOGGER.warn("Litematica Plus found but its hotkey API is not available", exception);
            return null;
        }
    }

    private String name(KeyMapping mapping) {
        try {
            Object found = entry.invoke(null, mapping);
            return found == null ? null : (String) entryName.invoke(found);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @Override
    public boolean handles(KeyMapping mapping) {
        return name(mapping) != null;
    }

    @Override
    public int modifiers(KeyMapping mapping) {
        String name = name(mapping);
        if (name == null) {
            return 0;
        }
        try {
            return (int) modifiers.invoke(null, name);
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    @Override
    public void setModifiers(KeyMapping mapping, int mask) {
        String name = name(mapping);
        if (name == null) {
            return;
        }
        try {
            setModifiers.invoke(null, name, mask);
        } catch (ReflectiveOperationException exception) {
            QualityEssentialsClient.LOGGER.warn("Could not change Litematica Plus modifiers for {}", name, exception);
        }
    }
}
