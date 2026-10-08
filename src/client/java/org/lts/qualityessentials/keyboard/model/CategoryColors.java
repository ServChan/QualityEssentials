package org.lts.qualityessentials.keyboard.model;

import java.util.Map;

public final class CategoryColors {
    private static final Map<String, Integer> KNOWN = Map.of(
            "minecraft:movement", 0xFF4FC3F7,
            "minecraft:gameplay", 0xFF81C784,
            "minecraft:inventory", 0xFFFFB74D,
            "minecraft:creative", 0xFFBA68C8,
            "minecraft:multiplayer", 0xFF4DB6AC,
            "minecraft:misc", 0xFF90A4AE,
            "minecraft:spectator", 0xFFF06292);
    private static final int[] PALETTE = {0xFFE57373, 0xFFFFD54F, 0xFFAED581, 0xFF7986CB, 0xFF4DD0E1, 0xFFF48FB1,
            0xFFA1887F, 0xFFDCE775, 0xFF9575CD, 0xFF64B5F6, 0xFFFF8A65, 0xFF4DB6AC};

    private CategoryColors() {
    }

    public static int of(String categoryId) {
        Integer known = KNOWN.get(categoryId);
        if (known != null) {
            return known;
        }
        int hash = categoryId == null ? 0 : categoryId.hashCode();
        return PALETTE[Math.floorMod(hash, PALETTE.length)];
    }
}
