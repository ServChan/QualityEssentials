package org.lts.qualityessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class FlatButton extends AbstractButton {
    public enum Style {
        PRIMARY,
        SECONDARY,
        DANGER,
        SEGMENT
    }

    private final Runnable action;
    private final Style style;
    private BooleanSupplier selected = () -> false;

    public FlatButton(int x, int y, int width, int height, Component message, Style style, Runnable action) {
        super(x, y, width, height, message);
        this.style = style;
        this.action = action;
        refreshTooltip();
    }

    public FlatButton selectedWhen(BooleanSupplier supplier) {
        this.selected = supplier;
        return this;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        refreshTooltip();
    }

    private void refreshTooltip() {
        Font font = Minecraft.getInstance().font;
        setTooltip(font.width(getMessage()) > getWidth() - 6 ? Tooltip.create(getMessage()) : null);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = this.active && isHoveredOrFocused();
        boolean on = selected.getAsBoolean();
        int fill;
        int border;
        int text;
        switch (style) {
            case PRIMARY -> {
                fill = !active ? Palette.SURFACE : hovered ? Palette.ACCENT_HOVER : Palette.ACCENT_DARK;
                border = active ? Palette.ACCENT_DARK : Palette.DIVIDER;
                text = active ? Palette.ACCENT_TEXT : Palette.TEXT_DISABLED;
            }
            case DANGER -> {
                fill = hovered ? Palette.DANGER_DARK : Palette.SURFACE;
                border = active ? Palette.DANGER : Palette.DIVIDER;
                text = !active ? Palette.TEXT_DISABLED : hovered ? Palette.TEXT_BRIGHT : Palette.DANGER;
            }
            case SEGMENT -> {
                fill = on ? Palette.ACCENT : hovered ? Palette.SURFACE_HOVER : Palette.SURFACE;
                border = Palette.BORDER;
                text = on ? Palette.ACCENT_TEXT : hovered ? Palette.TEXT_BRIGHT : Palette.TEXT;
            }
            default -> {
                fill = !active ? Palette.SURFACE : on ? Palette.SURFACE_SELECTED : hovered ? Palette.SURFACE_HOVER : Palette.SURFACE;
                border = !active ? Palette.DIVIDER : on ? Palette.ACCENT_DARK : hovered ? Palette.BORDER_STRONG : Palette.BORDER;
                text = !active ? Palette.TEXT_DISABLED : on ? Palette.ACCENT : hovered ? Palette.TEXT_BRIGHT : Palette.TEXT;
            }
        }
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.outline(x, y, w, h, border);
        String label = getMessage().getString();
        if (font.width(label) > w - 6) {
            label = font.plainSubstrByWidth(label, Math.max(0, w - 10)) + "..";
        }
        graphics.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, text, false);
        if (isFocused()) {
            graphics.outline(x - 1, y - 1, w + 2, h + 2, Palette.FOCUS);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
