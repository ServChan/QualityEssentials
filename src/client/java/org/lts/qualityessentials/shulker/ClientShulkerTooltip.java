package org.lts.qualityessentials.shulker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ClientShulkerTooltip implements ClientTooltipComponent {
    public static final int CELL = 18;
    private static final int TOP = 3;
    private static final int FOOTER = 11;
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

    private final ShulkerTooltip tooltip;

    public ClientShulkerTooltip(ShulkerTooltip tooltip) {
        this.tooltip = tooltip;
    }

    public static Component footer(ShulkerTooltip tooltip) {
        if (tooltip.summary()) {
            return Component.translatable("qualityessentials.shulker.footer.summary", tooltip.total(), tooltip.slots().size());
        }
        long used = tooltip.slots().stream().filter(stack -> !stack.isEmpty()).count();
        return Component.translatable("qualityessentials.shulker.footer.grid", tooltip.total(), used, tooltip.slots().size());
    }

    public static int width(Font font, ShulkerTooltip tooltip) {
        return Math.max(tooltip.columns() * CELL, font.width(footer(tooltip)));
    }

    public static int height(ShulkerTooltip tooltip) {
        return TOP + tooltip.rows() * CELL + 2 + FOOTER;
    }

    @Override
    public int getHeight(Font font) {
        return height(tooltip) + 2;
    }

    @Override
    public int getWidth(Font font) {
        return width(font, tooltip);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        draw(graphics, font, tooltip, x, y);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, ShulkerTooltip tooltip, int x, int y) {
        int columns = tooltip.columns();
        graphics.fill(x, y, x + width(font, tooltip), y + 1, tooltip.accent());
        int top = y + TOP;
        for (int i = 0; i < tooltip.slots().size(); i++) {
            int cx = x + i % columns * CELL;
            int cy = top + i / columns * CELL;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, cx, cy, CELL, CELL);
            ItemStack stack = tooltip.slots().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.item(stack, cx + 1, cy + 1);
            int count = tooltip.counts().get(i);
            graphics.itemDecorations(font, stack, cx + 1, cy + 1, count > 1 ? ShulkerContents.countLabel(count) : null);
        }
        graphics.text(font, footer(tooltip), x, top + tooltip.rows() * CELL + 3, 0xFF9AA5B8, false);
    }
}
