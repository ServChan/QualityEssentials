package org.lts.qualityessentials.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.HudLayout;
import org.lts.qualityessentials.core.HudPlacement;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.equipment.EquipmentHud;
import org.lts.qualityessentials.lookat.LookAtHud;
import org.lts.qualityessentials.mount.MountHud;
import org.lts.qualityessentials.stats.StatsHud;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HudEditorScreen extends Screen {
    private static final int SNAP = 5;
    private static final int ROW_HEIGHT = 24;
    private static final int SLIDER_WIDTH = 104;
    private static final int MIN_SLIDER_WIDTH = 56;

    private final Screen parent;
    private final QEConfig original;
    private QEConfig working;
    private HudElement selected;
    private HudElement dragging;
    private double grabX;
    private double grabY;
    private int guideX = -1;
    private int guideY = -1;
    private final Map<HudElement, HudPlacement.Rect> rects = new EnumMap<>(HudElement.class);
    private int toolbarY;
    private int toolbarRows = 1;
    private int nameX;

    public HudEditorScreen(Screen parent, HudElement focus) {
        super(Component.translatable("qualityessentials.editor.title"));
        this.parent = parent;
        this.original = QE.config().copy();
        this.working = QE.config().copy();
        this.selected = focus;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        measureAll();
        boolean elementLow = selected != null && rects.containsKey(selected)
                && rects.get(selected).y() + rects.get(selected).height() / 2 > this.height / 2;
        toolbarRows = leftGroupWidth() + rightGroupWidth() + 24 > this.width ? 2 : 1;
        toolbarY = elementLow ? 30 : this.height - toolbarRows * ROW_HEIGHT - 6;
        int x = 8;
        int y = toolbarY;
        int sliderWidth = sliderWidth();
        if (selected != null) {
            HudElement element = selected;
            nameX = x;
            x += this.font.width(Component.translatable(element.module().translationKey())) + 10;
            addRenderableWidget(new LayoutSlider(x, y, sliderWidth, "qualityessentials.editor.scale", HudLayout.MIN_SCALE, HudLayout.MAX_SCALE, 0.05f,
                    working.layout(element).scale, value -> set(element, layout -> layout.scale = HudLayout.clampScale(value))));
            x += sliderWidth + 4;
            addRenderableWidget(new LayoutSlider(x, y, sliderWidth, "qualityessentials.editor.opacity", 0f, 1f, 0.1f,
                    working.layout(element).opacity, value -> set(element, layout -> layout.opacity = value)));
            x += sliderWidth + 8;
            if (element == HudElement.EQUIPMENT) {
                Component orientation = Component.translatable(working.equipmentHorizontal
                        ? "qualityessentials.editor.horizontal" : "qualityessentials.editor.vertical");
                int width = this.font.width(orientation) + 14;
                addRenderableWidget(new FlatButton(x, y, width, 20, orientation, FlatButton.Style.SECONDARY, () -> {
                    working.equipmentHorizontal = !working.equipmentHorizontal;
                    rebuildWidgets();
                }));
                x += width + 4;
            }
            if (element == HudElement.STATS) {
                Component style = Component.translatable(working.statsCompact ? "qualityessentials.editor.compact" : "qualityessentials.editor.column");
                int width = this.font.width(style) + 14;
                addRenderableWidget(new FlatButton(x, y, width, 20, style, FlatButton.Style.SECONDARY, () -> {
                    working.statsCompact = !working.statsCompact;
                    rebuildWidgets();
                }));
                x += width + 4;
            }
            Component reset = Component.translatable("qualityessentials.editor.reset_one");
            int resetWidth = this.font.width(reset) + 14;
            addRenderableWidget(new FlatButton(x, y, resetWidth, 20, reset, FlatButton.Style.SECONDARY, () -> {
                working.setLayout(element, element.defaultLayout());
                rebuildWidgets();
            }));
        } else {
            nameX = x;
        }
        Component done = Component.translatable("qualityessentials.settings.done");
        Component cancel = Component.translatable("qualityessentials.editor.cancel");
        Component resetAll = Component.translatable("qualityessentials.editor.reset_all");
        int right = this.width - 8;
        y = toolbarRows == 2 ? toolbarY + ROW_HEIGHT : toolbarY;
        int doneWidth = this.font.width(done) + 24;
        addRenderableWidget(new FlatButton(right - doneWidth, y, doneWidth, 20, done, FlatButton.Style.PRIMARY, this::onClose));
        right -= doneWidth + 4;
        int cancelWidth = this.font.width(cancel) + 16;
        addRenderableWidget(new FlatButton(right - cancelWidth, y, cancelWidth, 20, cancel, FlatButton.Style.SECONDARY, () -> {
            working = original.copy();
            QE.update(original);
            this.minecraft.gui.setScreen(parent);
        }));
        right -= cancelWidth + 4;
        int resetAllWidth = this.font.width(resetAll) + 16;
        addRenderableWidget(new FlatButton(right - resetAllWidth, y, resetAllWidth, 20, resetAll, FlatButton.Style.DANGER, () -> {
            for (HudElement element : HudElement.values()) {
                working.setLayout(element, element.defaultLayout());
            }
            rebuildWidgets();
        }));
    }

    private int leftGroupWidth() {
        if (selected == null) {
            return this.font.width(Component.translatable("qualityessentials.editor.pick")) + 14;
        }
        int width = this.font.width(Component.translatable(selected.module().translationKey())) + 10;
        width += SLIDER_WIDTH * 2 + 12;
        if (selected == HudElement.EQUIPMENT || selected == HudElement.STATS) {
            width += this.font.width(Component.translatable("qualityessentials.editor.horizontal")) + 18;
        }
        return width + this.font.width(Component.translatable("qualityessentials.editor.reset_one")) + 14;
    }

    private int sliderWidth() {
        int spare = this.width - 16 - leftGroupWidth();
        return spare >= 0 ? SLIDER_WIDTH : Math.max(MIN_SLIDER_WIDTH, SLIDER_WIDTH + spare / 2);
    }

    private int rightGroupWidth() {
        return this.font.width(Component.translatable("qualityessentials.settings.done")) + 24 + 4
                + this.font.width(Component.translatable("qualityessentials.editor.cancel")) + 16 + 4
                + this.font.width(Component.translatable("qualityessentials.editor.reset_all")) + 16;
    }

    private void set(HudElement element, java.util.function.Consumer<HudLayout> change) {
        HudLayout layout = working.layout(element).copy();
        change.accept(layout);
        working.setLayout(element, layout);
    }

    private void adjust(HudElement element, java.util.function.Consumer<HudLayout> change) {
        HudLayout layout = working.layout(element).copy();
        change.accept(layout);
        working.setLayout(element, layout);
        rebuildWidgets();
    }

    private int[] contentSize(HudElement element) {
        return switch (element) {
            case LOOK_AT -> {
                LookAtHud.Info info = LookAtHud.preview(working);
                yield new int[]{LookAtHud.measure(this.font, info), LookAtHud.height(info)};
            }
            case EQUIPMENT -> EquipmentHud.size(this.font, entries(), working.equipmentHorizontal);
            case STATS -> StatsHud.size(this.font, stats(), working.statsCompact);
            case MOUNT -> MountHud.size(this.font, mount());
        };
    }

    private List<EquipmentHud.Entry> entries() {
        if (this.minecraft != null && this.minecraft.player != null) {
            List<EquipmentHud.Entry> live = EquipmentHud.collect(this.minecraft.player, working);
            if (!live.isEmpty()) {
                return live;
            }
        }
        return EquipmentHud.preview(working);
    }

    private List<StatsHud.Stat> stats() {
        if (this.minecraft != null && this.minecraft.player != null) {
            List<StatsHud.Stat> live = StatsHud.collect(this.minecraft, working);
            if (!live.isEmpty()) {
                return live;
            }
        }
        boolean singleplayer = this.minecraft != null && this.minecraft.hasSingleplayerServer();
        return StatsHud.sample(working, singleplayer);
    }

    private MountHud.Info mount() {
        if (this.minecraft != null && this.minecraft.player != null) {
            MountHud.Info live = MountHud.collect(this.minecraft.player, working);
            if (live != null) {
                return live;
            }
        }
        return MountHud.preview(working);
    }

    private void measureAll() {
        rects.clear();
        for (HudElement element : HudElement.values()) {
            int[] size = contentSize(element);
            rects.put(element, HudPlacement.rect(working.layout(element), this.width, this.height, size[0], size[1]));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null || this.minecraft.level == null) {
            graphics.fill(0, 0, this.width, this.height, 0xFF101010);
            graphics.outline(this.width / 2 - 91, this.height - 22, 182, 22, Palette.BORDER_STRONG);
        } else {
            graphics.fill(0, 0, this.width, this.height, 0x30000000);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x18FFFFFF);
        graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x18FFFFFF);
        measureAll();
        for (HudElement element : HudElement.values()) {
            drawElement(graphics, element, mouseX, mouseY);
        }
        if (guideX >= 0) {
            graphics.fill(guideX, 0, guideX + 1, this.height, Palette.HIGHLIGHT);
        }
        if (guideY >= 0) {
            graphics.fill(0, guideY, this.width, guideY + 1, Palette.HIGHLIGHT);
        }
        int bandY = toolbarY - 4;
        int bandHeight = toolbarRows * ROW_HEIGHT + 4;
        boolean top = toolbarY < this.height / 2;
        boolean inWorld = this.minecraft != null && this.minecraft.level != null;
        graphics.fill(0, bandY, this.width, bandY + bandHeight, Palette.HEADER);
        if (top) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR,
                    0, bandY + bandHeight, 0.0F, 0.0F, this.width, 2, 32, 2);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR,
                    0, bandY - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        }
        Component lead = selected == null ? Component.translatable("qualityessentials.editor.pick")
                : Component.translatable(selected.module().translationKey());
        graphics.text(this.font, lead, nameX, toolbarY + 6, selected == null ? Palette.TEXT_DIM : Palette.TEXT_BRIGHT, true);
        Component hint = Component.translatable("qualityessentials.editor.hint");
        List<net.minecraft.util.FormattedCharSequence> hintLines = this.font.split(hint, this.width - 24);
        int hintHeight = hintLines.size() * 10 + 4;
        int nearY = top ? bandY + bandHeight + 6 : bandY - hintHeight - 2;
        int farY = top ? this.height - hintHeight - 4 : 6;
        int hintY = hintFree(nearY, hintHeight) ? nearY : hintFree(farY, hintHeight) ? farY : -1;
        if (hintY >= 0) {
            graphics.fill(8, hintY - 2, this.width - 8, hintY + hintHeight - 2, Palette.BACKGROUND);
            for (int i = 0; i < hintLines.size(); i++) {
                graphics.centeredText(this.font, hintLines.get(i), this.width / 2, hintY + i * 10, Palette.TEXT_DIM);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    private boolean hintFree(int hintY, int hintHeight) {
        int top = hintY - 2;
        int bottom = hintY + hintHeight - 2;
        if (top < 0 || bottom > this.height) {
            return false;
        }
        for (HudPlacement.Rect rect : rects.values()) {
            if (rect.y() < bottom && rect.y() + rect.height() > top && rect.x() < this.width - 8 && rect.x() + rect.width() > 8) {
                return false;
            }
        }
        return true;
    }

    private void drawElement(GuiGraphicsExtractor graphics, HudElement element, int mouseX, int mouseY) {
        HudPlacement.Rect rect = rects.get(element);
        HudLayout layout = working.layout(element);
        boolean active = QE.active(element.module());
        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.x(), rect.y());
        graphics.pose().scale(rect.scale(), rect.scale());
        switch (element) {
            case LOOK_AT -> {
                LookAtHud.Info info = LookAtHud.preview(working);
                LookAtHud.draw(graphics, this.font, info, 0, 0, LookAtHud.measure(this.font, info), layout.opacity);
            }
            case EQUIPMENT -> EquipmentHud.draw(graphics, this.font, entries(), working.equipmentHorizontal, layout.opacity);
            case STATS -> StatsHud.draw(graphics, this.font, stats(), working.statsCompact, layout.opacity);
            case MOUNT -> MountHud.draw(graphics, this.font, mount(), layout.opacity);
        }
        graphics.pose().popMatrix();
        boolean isSelected = element == selected;
        boolean hovered = rect.contains(mouseX, mouseY);
        if (isSelected) {
            graphics.outline(rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, Palette.FOCUS);
        } else {
            dashed(graphics, rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2, hovered ? 0xC0FFFFFF : 0x60FFFFFF);
        }
        if (!active) {
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0x90000000);
        }
        if (isSelected || hovered || !active) {
            Component label = Component.translatable(element.module().translationKey())
                    .append(active ? "" : " - ").append(active ? Component.empty() : Component.translatable("qualityessentials.settings.card.off"))
                    .append("  " + Math.round(layout.scale * 100) + "%");
            int labelWidth = this.font.width(label) + 8;
            int labelY = rect.y() >= 14 ? rect.y() - 13 : rect.y() + rect.height() + 2;
            int labelX = Math.max(0, Math.min(this.width - labelWidth, rect.x()));
            graphics.fill(labelX, labelY, labelX + labelWidth, labelY + 11, Palette.SURFACE_SELECTED);
            graphics.text(this.font, label, labelX + 4, labelY + 2, isSelected ? Palette.TEXT_BRIGHT : Palette.TEXT_SUBTLE, true);
        }
    }

    private static void dashed(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        for (int i = 0; i < width; i += 4) {
            int end = Math.min(i + 2, width);
            graphics.fill(x + i, y, x + end, y + 1, color);
            graphics.fill(x + i, y + height - 1, x + end, y + height, color);
        }
        for (int i = 0; i < height; i += 4) {
            int end = Math.min(i + 2, height);
            graphics.fill(x, y + i, x + 1, y + end, color);
            graphics.fill(x + width - 1, y + i, x + width, y + end, color);
        }
    }

    private HudElement elementAt(double x, double y) {
        if (selected != null && rects.containsKey(selected) && rects.get(selected).contains(x, y)) {
            return selected;
        }
        for (HudElement element : HudElement.values()) {
            if (rects.get(element).contains(x, y)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        HudElement element = elementAt(event.x(), event.y());
        if (element != selected) {
            selected = element;
            rebuildWidgets();
        }
        if (element != null) {
            HudPlacement.Rect rect = rects.get(element);
            dragging = element;
            grabX = event.x() - rect.x();
            grabY = event.y() - rect.y();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging == null) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        HudPlacement.Rect rect = rects.get(dragging);
        int x = (int) Math.round(event.x() - grabX);
        int y = (int) Math.round(event.y() - grabY);
        int[] guide = new int[1];
        boolean free = InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT);
        if (!free) {
            x = HudLayout.snap(x, rect.width(), this.width, SNAP, guide);
            guideX = guide[0];
            y = HudLayout.snap(y, rect.height(), this.height, SNAP, guide);
            guideY = guide[0];
        } else {
            guideX = -1;
            guideY = -1;
        }
        x = Math.max(0, Math.min(this.width - rect.width(), x));
        y = Math.max(0, Math.min(this.height - rect.height(), y));
        HudLayout current = working.layout(dragging);
        working.setLayout(dragging, HudLayout.fromPosition(this.width, this.height, x, y, rect.width(), rect.height(), current.scale, current.opacity));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            guideX = -1;
            guideY = -1;
            rebuildWidgets();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudElement element = elementAt(mouseX, mouseY);
        if (element == null) {
            element = selected;
        }
        if (element == null || scrollY == 0) {
            return false;
        }
        selected = element;
        HudElement target = element;
        adjust(target, layout -> layout.scale = HudLayout.clampScale(layout.scale + (scrollY > 0 ? 0.05f : -0.05f)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (selected != null) {
            int step = event.hasShiftDown() ? 10 : 1;
            int dx = event.key() == InputConstants.KEY_LEFT ? -step : event.key() == InputConstants.KEY_RIGHT ? step : 0;
            int dy = event.key() == InputConstants.KEY_UP ? -step : event.key() == InputConstants.KEY_DOWN ? step : 0;
            if (dx != 0 || dy != 0) {
                HudPlacement.Rect rect = rects.get(selected);
                HudLayout current = working.layout(selected);
                int x = Math.max(0, Math.min(this.width - rect.width(), rect.x() + dx));
                int y = Math.max(0, Math.min(this.height - rect.height(), rect.y() + dy));
                working.setLayout(selected, HudLayout.fromPosition(this.width, this.height, x, y, rect.width(), rect.height(), current.scale, current.opacity));
                return true;
            }
            if (event.key() == InputConstants.KEY_TAB) {
                HudElement[] all = HudElement.values();
                selected = all[(selected.ordinal() + 1) % all.length];
                rebuildWidgets();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        QE.update(working);
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private static final class LayoutSlider extends AbstractSliderButton {
        private final String key;
        private final float min;
        private final float max;
        private final float step;
        private final java.util.function.Consumer<Float> apply;

        LayoutSlider(int x, int y, int width, String key, float min, float max, float step, float current, java.util.function.Consumer<Float> apply) {
            super(x, y, width, 20, Component.empty(), (Math.max(min, Math.min(max, current)) - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = step;
            this.apply = apply;
            updateMessage();
        }

        private float current() {
            return Math.round((min + (float) this.value * (max - min)) / step) * step;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key).append(": " + Math.round(current() * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            apply.accept(current());
        }
    }

    public static String anchorName(HudLayout layout) {
        return layout.anchor().name().toLowerCase(Locale.ROOT);
    }
}
