package org.lts.qualityessentials.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lts.qualityessentials.text.FormatCode;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class CodeButton extends AbstractButton {
    private final FormatCode code;
    private final Consumer<FormatCode> action;
    private final BooleanSupplier selected;

    public CodeButton(int x, int y, int width, int height, FormatCode code, Component hint, BooleanSupplier selected, Consumer<FormatCode> action) {
        super(x, y, width, height, Component.translatable(code.translationKey()));
        this.code = code;
        this.action = action;
        this.selected = selected;
        setOverrideRenderHighlightedSprite(() -> isHoveredOrFocused() || selected.getAsBoolean());
        MutableComponent tip = Component.translatable(code.translationKey()).copy();
        if (hint != null) {
            tip.append(Component.literal("\n")).append(hint.copy().withStyle(ChatFormatting.GRAY));
        }
        setTooltip(Tooltip.create(tip));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.accept(code);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = this.active && isHoveredOrFocused();
        boolean on = selected.getAsBoolean();
        if (code.isColor()) {
            int edge = on || hovered ? Palette.FOCUS : 0xFF000000;
            graphics.fill(x, y, x + w, y + h, edge);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, this.active ? 0xFF000000 | code.rgb() : Palette.SURFACE);
            if (on) {
                graphics.outline(x + 1, y + 1, w - 2, h - 2, 0xFF000000);
            }
            return;
        }
        extractDefaultSprite(graphics);
        Font font = Minecraft.getInstance().font;
        Component label = label(code);
        int color = !this.active ? Palette.TEXT_DIM : Palette.TEXT_BRIGHT;
        graphics.text(font, label, x + (w - font.width(label) + 1) / 2, y + (h - 8) / 2, color, true);
        if (on) {
            int lineY = y + (h - 8) / 2 + 9;
            graphics.fill(x + 3, lineY, x + w - 3, lineY + 1, color);
        }
    }

    private static Component label(FormatCode code) {
        return switch (code) {
            case BOLD -> Component.literal("B").withStyle(ChatFormatting.BOLD);
            case ITALIC -> Component.literal("I").withStyle(ChatFormatting.ITALIC);
            case UNDERLINE -> Component.literal("U").withStyle(ChatFormatting.UNDERLINE);
            case STRIKETHROUGH -> Component.literal("S").withStyle(ChatFormatting.STRIKETHROUGH);
            case OBFUSCATED -> Component.literal("?").withStyle(ChatFormatting.OBFUSCATED);
            case RESET -> Component.literal("R");
            default -> Component.literal(String.valueOf(code.code()));
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
