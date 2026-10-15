package org.lts.qualityessentials.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class Ui {
    private Ui() {
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
    }

    public static void scaledText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, float scale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    public static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float fraction, int fill, int background) {
        graphics.fill(x, y, x + width, y + height, background);
        int filled = Math.round(width * Math.max(0f, Math.min(1f, fraction)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, fill);
        }
    }

    public static int withAlpha(int color, float alpha) {
        return ARGB.color(Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f), color & 0xFFFFFF);
    }

    public static float pulse(long millis, float periodMillis) {
        return (float) (Math.sin(millis / periodMillis * Math.PI * 2.0) * 0.5 + 0.5);
    }
}
