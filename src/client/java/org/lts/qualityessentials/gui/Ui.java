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

    public static int pill(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int background, int color) {
        int width = font.width(text) + 8;
        graphics.fill(x, y, x + width, y + 11, background);
        graphics.text(font, text, x + 4, y + 2, color, false);
        return width;
    }

    public static void toggle(GuiGraphicsExtractor graphics, int x, int y, boolean on, boolean active, boolean hovered) {
        int track = !active ? Palette.DIVIDER : on ? Palette.ACCENT_DARK : Palette.SURFACE_HOVER;
        int border = !active ? Palette.DIVIDER : on ? Palette.ACCENT : hovered ? Palette.BORDER_STRONG : Palette.BORDER;
        graphics.fill(x, y, x + 22, y + 11, track);
        graphics.outline(x, y, 22, 11, border);
        int knobX = on ? x + 12 : x + 2;
        graphics.fill(knobX, y + 2, knobX + 8, y + 9, !active ? Palette.TEXT_DISABLED : on ? Palette.TEXT_BRIGHT : Palette.TEXT_DIM);
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
