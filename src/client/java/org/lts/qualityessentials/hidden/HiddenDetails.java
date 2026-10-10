package org.lts.qualityessentials.hidden;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;

import java.util.List;

public final class HiddenDetails {
    private static boolean revealed;

    private HiddenDetails() {
    }

    private static boolean applies(TooltipDisplay display) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.isSameThread() && QE.active(Module.HIDDEN_DETAILS)
                && !display.hideTooltip() && !display.hiddenComponents().isEmpty();
    }

    public static TooltipDisplay display(TooltipDisplay display) {
        revealed = false;
        if (applies(display) && Minecraft.getInstance().hasShiftDown()) {
            revealed = true;
            return TooltipDisplay.DEFAULT;
        }
        return display;
    }

    public static void finish(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag, List<Component> lines) {
        boolean wasRevealed = revealed;
        revealed = false;
        if (lines.isEmpty()) {
            return;
        }
        if (wasRevealed) {
            lines.add(Component.translatable("qualityessentials.hidden_details.shown").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (!QE.config().hiddenDetailsHint) {
            return;
        }
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (!applies(display)) {
            return;
        }
        int extra = count(stack, context, player, flag, TooltipDisplay.DEFAULT) - count(stack, context, player, flag, display);
        if (extra > 0) {
            lines.add(Component.translatable("qualityessentials.hidden_details.hint", extra).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static int count(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag, TooltipDisplay display) {
        int[] lines = {0};
        stack.addDetailsToTooltip(context, display, player, flag, line -> lines[0]++);
        return lines[0];
    }

    public static List<Component> preview(boolean shift) {
        Component name = Component.translatable("qualityessentials.settings.preview.hidden.name").withStyle(ChatFormatting.AQUA);
        Component lore = Component.translatable("qualityessentials.settings.preview.hidden.lore").withStyle(ChatFormatting.GRAY);
        if (!shift) {
            return List.of(name, lore, Component.translatable("qualityessentials.hidden_details.hint", 3).withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of(name,
                Component.translatable("enchantment.minecraft.efficiency").append(" V").withStyle(ChatFormatting.GRAY),
                Component.translatable("enchantment.minecraft.unbreaking").append(" III").withStyle(ChatFormatting.GRAY),
                Component.translatable("enchantment.minecraft.mending").withStyle(ChatFormatting.GRAY),
                lore,
                Component.translatable("qualityessentials.hidden_details.shown").withStyle(ChatFormatting.DARK_GRAY));
    }
}
