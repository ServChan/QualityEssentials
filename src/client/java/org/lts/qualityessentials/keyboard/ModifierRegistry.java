package org.lts.qualityessentials.keyboard;

import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModifierRegistry {
    private static final List<ModifierSource> SOURCES = new CopyOnWriteArrayList<>();

    private ModifierRegistry() {
    }

    public static void register(ModifierSource source) {
        if (source != null && !SOURCES.contains(source)) {
            SOURCES.add(source);
        }
    }

    public static ModifierSource sourceFor(KeyMapping mapping) {
        for (ModifierSource source : SOURCES) {
            if (source.handles(mapping)) {
                return source;
            }
        }
        return null;
    }
}
