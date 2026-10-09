package org.lts.qualityessentials.shulker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class ClientShulkerTooltip implements ClientTooltipComponent {
    public static final int CELL = 18;
    private static final int TOP = 3;
    private static final int CELL_FILL = 0x24FFFFFF;
    private static final int CELL_EDGE = 0x1CFFFFFF;

    private final ShulkerTooltip tooltip;

    public ClientShulkerTooltip(ShulkerTooltip tooltip) {
        this.tooltip = tooltip;
    }

    public static int width(ShulkerTooltip tooltip) {
        return tooltip.columns() * CELL;
    }

    public static int height(ShulkerTooltip tooltip) {
        return TOP + tooltip.rows() * CELL + 2;
    }

    @Override
    public int getHeight(Font font) {
        return height(tooltip) + 2;
    }

    @Override
    public int getWidth(Font font) {
        return width(tooltip);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        draw(graphics, font, tooltip, x, y);
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, ShulkerTooltip tooltip, int x, int y) {
        int columns = tooltip.columns();
        graphics.fill(x, y, x + columns * CELL - 1, y + 1, tooltip.accent());
        int top = y + TOP;
        for (int i = 0; i < tooltip.slots().size(); i++) {
            int cx = x + i % columns * CELL;
            int cy = top + i / columns * CELL;
            graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, CELL_FILL);
            graphics.fill(cx, cy, cx + CELL - 1, cy + 1, CELL_EDGE);
            ItemStack stack = tooltip.slots().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.item(stack, cx + 1, cy + 1);
            int count = tooltip.counts().get(i);
            graphics.itemDecorations(font, stack, cx + 1, cy + 1, count > 1 ? ShulkerContents.countLabel(count) : null);
        }
    }
}
