package org.lts.qualityessentials.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudPlacement {
    private HudPlacement() {
    }

    public static Rect rect(HudLayout layout, int screenWidth, int screenHeight, int width, int height) {
        int scaledWidth = Math.max(1, Math.round(width * layout.scale));
        int scaledHeight = Math.max(1, Math.round(height * layout.scale));
        int[] position = layout.position(screenWidth, screenHeight, scaledWidth, scaledHeight);
        return new Rect(position[0], position[1], scaledWidth, scaledHeight, layout.scale);
    }

    public static Rect draw(GuiGraphicsExtractor graphics, HudLayout layout, int width, int height, int extraTop, Drawer drawer) {
        Rect rect = rect(layout, graphics.guiWidth(), graphics.guiHeight(), width, height);
        int y = rect.y;
        if (extraTop > 0 && layout.anchor().row == 0 && rect.y < extraTop) {
            y = extraTop;
        }
        Rect placed = new Rect(rect.x, y, rect.width, rect.height, rect.scale);
        graphics.pose().pushMatrix();
        graphics.pose().translate(placed.x, placed.y);
        graphics.pose().scale(placed.scale, placed.scale);
        drawer.draw(graphics);
        graphics.pose().popMatrix();
        return placed;
    }

    public interface Drawer {
        void draw(GuiGraphicsExtractor graphics);
    }

    public record Rect(int x, int y, int width, int height, float scale) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
