package org.lts.qualityessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
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
    private Component hint;

    public FlatButton(int x, int y, int width, int height, Component message, Style style, Runnable action) {
        super(x, y, width, height, message);
        this.style = style;
        this.action = action;
        setOverrideRenderHighlightedSprite(() -> isHoveredOrFocused() || selected.getAsBoolean());
        refreshTooltip();
    }

    public FlatButton selectedWhen(BooleanSupplier supplier) {
        this.selected = supplier;
        return this;
    }

    public FlatButton withHint(Component hint) {
        this.hint = hint;
        refreshTooltip();
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
        boolean truncated = font.width(getMessage()) > getWidth() - 6;
        if (hint != null) {
            setTooltip(Tooltip.create(truncated ? getMessage().copy().append("\n").append(hint) : hint));
        } else {
            setTooltip(truncated ? Tooltip.create(getMessage()) : null);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractDefaultSprite(graphics);
        Component label = style == Style.DANGER && this.active ? getMessage().copy().withColor(Palette.DANGER) : getMessage();
        extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), label, 2);
        if (selected.getAsBoolean()) {
            Font font = Minecraft.getInstance().font;
            int width = Math.min(font.width(getMessage()), getWidth() - 6);
            int left = getX() + (getWidth() - width) / 2;
            int top = getY() + (getHeight() - 8) / 2 + 9;
            graphics.fill(left, top, left + width, top + 1, this.active ? Palette.TEXT_BRIGHT : Palette.TEXT_DIM);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        if (style == Style.SEGMENT && selected.getAsBoolean()) {
            output.add(NarratedElementType.HINT, Component.translatable("qualityessentials.state.on"));
        }
    }
}
