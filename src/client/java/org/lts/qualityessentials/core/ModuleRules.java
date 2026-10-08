package org.lts.qualityessentials.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ModuleRules {
    private ModuleRules() {
    }

    public static String blocker(Module module, Set<String> loadedModIds) {
        for (String id : module.conflictingModIds()) {
            if (loadedModIds.contains(id)) {
                return id;
            }
        }
        return null;
    }

    public static Map<Module, String> blockers(Set<String> loadedModIds) {
        Map<Module, String> result = new EnumMap<>(Module.class);
        for (Module module : Module.values()) {
            String blocker = blocker(module, loadedModIds);
            if (blocker != null) {
                result.put(module, blocker);
            }
        }
        return result;
    }

    public static boolean active(Module module, boolean enabled, Map<Module, String> blockers) {
        return enabled && !blockers.containsKey(module);
    }
}
