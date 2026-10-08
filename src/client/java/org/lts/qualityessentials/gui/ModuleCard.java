package org.lts.qualityessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;

import java.util.function.BooleanSupplier;

public final class ModuleCard extends AbstractButton {
    public static final int HEIGHT = 30;

    private final Module module;
    private final ItemStack icon;
    private final BooleanSupplier selected;
    private final Runnable action;

    public ModuleCard(int x, int y, int width, Module module, ItemStack icon, BooleanSupplier selected, Runnable action) {
        super(x, y, width, HEIGHT, Component.translatable(module.translationKey()));
        this.module = module;
        this.icon = icon;
        this.selected = selected;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    public static Component status(Module module) {
        String blocker = QE.blocker(module);
        if (blocker != null) {
            return Component.translatable("qualityessentials.settings.card.yielded", QE.modName(blocker));
        }
        return Component.translatable(QE.active(module) ? "qualityessentials.settings.card.on" : "qualityessentials.settings.card.off");
    }

    public static int statusColor(Module module) {
        if (QE.blocker(module) != null) {
            return Palette.WARNING;
        }
        return QE.active(module) ? Palette.SUCCESS : Palette.TEXT_DISABLED;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        boolean on = selected.getAsBoolean();
        boolean hovered = isHoveredOrFocused();
        graphics.fill(x, y, x + w, y + HEIGHT - 2, on ? Palette.SURFACE_SELECTED : hovered ? Palette.SURFACE_HOVER : Palette.SURFACE);
        if (on) {
            graphics.fill(x, y, x + 2, y + HEIGHT - 2, Palette.ACCENT);
        }
        graphics.item(icon, x + 6, y + 6);
        int textX = x + 28;
        int maxWidth = w - textX + x - 6;
        String name = getMessage().getString();
        if (font.width(name) > maxWidth) {
            name = font.plainSubstrByWidth(name, maxWidth - 6) + "..";
        }
        graphics.text(font, name, textX, y + 5, on ? Palette.TEXT_BRIGHT : Palette.TEXT, false);
        int color = statusColor(module);
        graphics.fill(textX, y + 18, textX + 4, y + 22, color);
        String status = status(module).getString();
        if (font.width(status) > maxWidth - 8) {
            status = font.plainSubstrByWidth(status, maxWidth - 14) + "..";
        }
        graphics.text(font, status, textX + 7, y + 16, color, false);
        if (isFocused()) {
            graphics.outline(x - 1, y - 1, w + 2, HEIGHT, Palette.FOCUS);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        output.add(NarratedElementType.HINT, status(module));
    }
}
