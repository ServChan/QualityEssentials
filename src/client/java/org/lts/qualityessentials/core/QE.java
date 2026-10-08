package org.lts.qualityessentials.core;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class QE {
    private static volatile QEConfig config = new QEConfig().sanitize();
    private static volatile Map<Module, String> blockers = Map.of();

    private QE() {
    }

    public static void load() {
        config = QEConfig.read(path());
        Set<String> loaded = new HashSet<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            loaded.add(container.getMetadata().getId());
        }
        blockers = ModuleRules.blockers(loaded);
    }

    public static QEConfig config() {
        return config;
    }

    public static void update(QEConfig updated) {
        QEConfig snapshot = updated.copy();
        config = snapshot;
        CompletableFuture.runAsync(() -> QEConfig.write(path(), snapshot));
    }

    public static boolean active(Module module) {
        return ModuleRules.active(module, config.enabled(module), blockers);
    }

    public static String blocker(Module module) {
        return blockers.get(module);
    }

    public static String modName(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(container -> container.getMetadata().getName())
                .orElse(id);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("qualityessentials.json");
    }
}
