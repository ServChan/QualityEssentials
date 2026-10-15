package org.lts.qualityessentials.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.List;

public final class Notice {
    private static final long DURATION = 3500L;

    private Component text;
    private boolean problem;
    private long until;

    public void show(Component text, boolean problem) {
        this.text = text;
        this.problem = problem;
        this.until = Util.getMillis() + DURATION;
    }

    public boolean visible() {
        return text != null && Util.getMillis() < until;
    }

    public void drawBelow(GuiGraphicsExtractor graphics, Font font, int centerX, int y, int width, int screenHeight) {
        if (!visible()) {
            return;
        }
        int height = font.split(text, Math.max(20, width - 10)).size() * 10 + 6;
        draw(graphics, font, centerX - width / 2, Math.max(0, Math.min(y, screenHeight - height - 2)), width);
    }

    public int draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
        if (!visible()) {
            return 0;
        }
        List<FormattedCharSequence> lines = font.split(text, Math.max(20, width - 10));
        int height = lines.size() * 10 + 6;
        graphics.fill(x, y, x + width, y + height, Palette.SURFACE_SELECTED);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), x + 5, y + 4 + i * 10, problem ? Palette.DANGER_TEXT : Palette.TEXT_BRIGHT, true);
        }
        return height;
    }
}
