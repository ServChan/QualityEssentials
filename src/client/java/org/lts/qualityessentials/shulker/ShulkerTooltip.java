package org.lts.qualityessentials.shulker;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ShulkerTooltip(List<ItemStack> slots, List<Integer> counts, int columns, int accent, boolean summary, int total) implements TooltipComponent {
    public int rows() {
        return Math.max(1, (slots.size() + columns - 1) / columns);
    }
}
