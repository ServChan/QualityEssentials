package org.lts.qualityessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class SwitchRow extends AbstractButton {
    public static final int HEIGHT = 26;
    public static final int COMPACT_HEIGHT = 20;

    private final Component hint;
    private final BooleanSupplier state;
    private final Runnable action;

    public SwitchRow(int x, int y, int width, int height, Component label, Component hint, BooleanSupplier state, Runnable action) {
        super(x, y, width, height, label);
        this.hint = height >= HEIGHT ? hint : null;
        if (this.hint == null && hint != null) {
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(hint));
        }
        this.state = state;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = active && isHoveredOrFocused();
        if (hovered) {
            graphics.fill(x, y, x + w, y + h - 2, Palette.SURFACE_HOVER);
        }
        graphics.fill(x, y + h - 1, x + w, y + h, Palette.DIVIDER);
        int textWidth = w - 40;
        graphics.text(font, fit(font, getMessage().getString(), textWidth), x + 6, hint == null ? y + (h - 9) / 2 : y + 4,
                active ? Palette.TEXT_BRIGHT : Palette.TEXT_DISABLED, false);
        if (hint != null) {
            graphics.text(font, fit(font, hint.getString(), textWidth), x + 6, y + 14, active ? Palette.TEXT_DIM : Palette.TEXT_DISABLED, false);
        }
        Ui.toggle(graphics, x + w - 28, y + (h - 12) / 2, state.getAsBoolean(), active, hovered);
        if (isFocused()) {
            graphics.outline(x - 1, y - 1, w + 2, h + 1, Palette.FOCUS);
        }
    }

    private static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - 6)) + "..";
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        output.add(NarratedElementType.HINT, Component.translatable(state.getAsBoolean() ? "qualityessentials.state.on" : "qualityessentials.state.off"));
    }
}
