package org.lts.qualityessentials.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.keyboard.KeyboardModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SettingsScreen extends Screen {
    private static final int HEADER_HEIGHT = 24;
    private static final int LIST_WIDTH = 150;
    private static final int ROW = 22;

    private final Screen parent;
    private Module selected = Module.FOOD;
    private QEConfig working;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("qualityessentials.settings.title"));
        this.parent = parent;
        this.working = QE.config().copy();
    }

    @Override
    protected void init() {
        int y = HEADER_HEIGHT + 10;
        for (Module module : Module.values()) {
            Module target = module;
            addRenderableWidget(new FlatButton(8, y, LIST_WIDTH, 20, Component.translatable(module.translationKey()),
                    FlatButton.Style.SEGMENT, () -> {
                        selected = target;
                        rebuildWidgets();
                    }).selectedWhen(() -> selected == target));
            y += ROW;
        }
        int left = 8 + LIST_WIDTH + 12;
        int width = Math.min(380, this.width - left - 8);
        int optionY = HEADER_HEIGHT + 78;
        String blocker = QE.blocker(selected);
        FlatButton enable = addRenderableWidget(new FlatButton(left, optionY, width, 20, toggleLabel("qualityessentials.settings.enabled",
                working.enabled(selected)), FlatButton.Style.SECONDARY, () -> {
                    working.setEnabled(selected, !working.enabled(selected));
                    apply();
                }).selectedWhen(() -> working.enabled(selected)));
        enable.active = blocker == null;
        optionY += ROW + 6;
        for (Option option : options(selected)) {
            FlatButton button = addRenderableWidget(new FlatButton(left, optionY, width, 20, option.label.apply(working),
                    FlatButton.Style.SECONDARY, () -> {
                        option.action.accept(working);
                        apply();
                    }).selectedWhen(() -> option.selected.test(working)));
            button.active = blocker == null && working.enabled(selected);
            optionY += ROW;
        }
        if (selected == Module.KEYBOARD) {
            FlatButton open = addRenderableWidget(new FlatButton(left, optionY + 4, width, 20,
                    Component.translatable("qualityessentials.keyboard.button.open_short"), FlatButton.Style.PRIMARY,
                    () -> this.minecraft.gui.setScreen(KeyboardModule.createScreen(this))));
            open.active = QE.active(Module.KEYBOARD);
        }
        addRenderableWidget(new FlatButton(this.width / 2 - 75, this.height - 26, 150, 20, Component.translatable("qualityessentials.settings.done"),
                FlatButton.Style.PRIMARY, this::onClose));
    }

    private void apply() {
        QE.update(working);
        working = QE.config().copy();
        rebuildWidgets();
    }

    private Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(on ? "qualityessentials.state.on" : "qualityessentials.state.off"));
    }

    private List<Option> options(Module module) {
        List<Option> list = new ArrayList<>();
        switch (module) {
            case FOOD -> {
                list.add(toggle("qualityessentials.option.food.saturation", c -> c.foodSaturationOverlay, c -> c.foodSaturationOverlay = !c.foodSaturationOverlay));
                list.add(toggle("qualityessentials.option.food.preview", c -> c.foodHeldPreview, c -> c.foodHeldPreview = !c.foodHeldPreview));
                list.add(toggle("qualityessentials.option.food.tooltip", c -> c.foodTooltip, c -> c.foodTooltip = !c.foodTooltip));
            }
            case LOOK_AT -> {
                list.add(toggle("qualityessentials.option.lookat.health", c -> c.lookAtShowHealth, c -> c.lookAtShowHealth = !c.lookAtShowHealth));
                list.add(toggle("qualityessentials.option.lookat.states", c -> c.lookAtShowStates, c -> c.lookAtShowStates = !c.lookAtShowStates));
                list.add(toggle("qualityessentials.option.lookat.harvest", c -> c.lookAtShowHarvest, c -> c.lookAtShowHarvest = !c.lookAtShowHarvest));
                list.add(toggle("qualityessentials.option.lookat.progress", c -> c.lookAtShowProgress, c -> c.lookAtShowProgress = !c.lookAtShowProgress));
                list.add(toggle("qualityessentials.option.lookat.mod", c -> c.lookAtShowMod, c -> c.lookAtShowMod = !c.lookAtShowMod));
            }
            case EFFECTS -> {
                list.add(toggle("qualityessentials.option.effects.timer", c -> c.effectsShowTimer, c -> c.effectsShowTimer = !c.effectsShowTimer));
                list.add(toggle("qualityessentials.option.effects.level", c -> c.effectsShowLevel, c -> c.effectsShowLevel = !c.effectsShowLevel));
            }
            case EQUIPMENT -> {
                list.add(new Option(c -> Component.translatable("qualityessentials.option.equipment.anchor")
                        .append(": ").append(Component.translatable("qualityessentials.anchor." + c.anchor().name().toLowerCase(java.util.Locale.ROOT))),
                        c -> c.equipmentAnchor = c.anchor().next().name(), c -> false));
                list.add(toggle("qualityessentials.option.equipment.percent", c -> c.equipmentShowPercent, c -> c.equipmentShowPercent = !c.equipmentShowPercent));
                list.add(toggle("qualityessentials.option.equipment.ammo", c -> c.equipmentShowAmmo, c -> c.equipmentShowAmmo = !c.equipmentShowAmmo));
                list.add(toggle("qualityessentials.option.equipment.totals", c -> c.equipmentShowStackTotals, c -> c.equipmentShowStackTotals = !c.equipmentShowStackTotals));
            }
            case DARK_SCREEN -> list.add(new Option(c -> Component.translatable("qualityessentials.option.dark.color")
                    .append(": ").append(Component.translatable("qualityessentials.dark." + c.darkPreset().name().toLowerCase(java.util.Locale.ROOT))),
                    c -> c.darkScreenPreset = c.darkPreset().next().name(), c -> false));
            case KEYBOARD -> list.add(toggle("qualityessentials.option.keyboard.controls", c -> c.keyboardControlsButton,
                    c -> c.keyboardControlsButton = !c.keyboardControlsButton));
        }
        return list;
    }

    private Option toggle(String key, Predicate<QEConfig> getter, Consumer<QEConfig> flip) {
        return new Option(c -> toggleLabel(key, getter.test(c)), flip, getter);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.width, this.height, Palette.BACKGROUND);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, Palette.HEADER);
        graphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, Palette.BORDER);
        graphics.text(this.font, Component.literal("QualityEssentials"), 10, 8, Palette.CYAN, true);
        graphics.text(this.font, Component.translatable("qualityessentials.settings.subtitle"), 10 + this.font.width("QualityEssentials") + 8, 8,
                Palette.TEXT_DIM, false);
        int left = 8 + LIST_WIDTH + 12;
        int width = Math.min(380, this.width - left - 8);
        for (int i = 0; i < Module.values().length; i++) {
            Module module = Module.values()[i];
            int dotY = HEADER_HEIGHT + 10 + i * ROW + 7;
            int color = QE.active(module) ? 0xFF55FF55 : QE.blocker(module) != null ? Palette.WARNING : 0xFF5C6B7C;
            graphics.fill(8 + LIST_WIDTH - 10, dotY, 8 + LIST_WIDTH - 4, dotY + 6, color);
        }
        int y = HEADER_HEIGHT + 10;
        graphics.text(this.font, Component.translatable(selected.translationKey()), left, y, Palette.TEXT_BRIGHT, true);
        y += 13;
        graphics.textWithWordWrap(this.font, Component.translatable(selected.translationKey() + ".description"), left, y, width, Palette.TEXT_DIM);
        int statusY = HEADER_HEIGHT + 64;
        String blocker = QE.blocker(selected);
        Component status;
        int statusColor;
        if (blocker != null) {
            status = Component.translatable("qualityessentials.settings.status.yielded", QE.modName(blocker));
            statusColor = Palette.WARNING;
        } else if (QE.active(selected)) {
            status = Component.translatable("qualityessentials.settings.status.active");
            statusColor = 0xFF55FF55;
        } else {
            status = Component.translatable("qualityessentials.settings.status.off");
            statusColor = Palette.TEXT_DISABLED;
        }
        graphics.text(this.font, status, left, statusY, statusColor, false);
        graphics.text(this.font, Component.translatable("qualityessentials.settings.footer"), 8, this.height - 40, Palette.TEXT_DISABLED, false);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private record Option(java.util.function.Function<QEConfig, Component> label, Consumer<QEConfig> action, Predicate<QEConfig> selected) {
    }
}
