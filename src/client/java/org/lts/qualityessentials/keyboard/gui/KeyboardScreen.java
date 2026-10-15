package org.lts.qualityessentials.keyboard.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lts.qualityessentials.keyboard.ModifierRegistry;
import org.lts.qualityessentials.keyboard.BindingSnapshot;
import org.lts.qualityessentials.keyboard.ModifierSource;
import org.lts.qualityessentials.gui.FlatButton;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.keyboard.layout.KeyCap;
import org.lts.qualityessentials.keyboard.layout.KeyboardLayout;
import org.lts.qualityessentials.keyboard.model.Binding;
import org.lts.qualityessentials.keyboard.model.CategoryColors;
import org.lts.qualityessentials.keyboard.model.ConflictAnalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class KeyboardScreen extends Screen {
    private static final int HEADER_HEIGHT = 24;
    private static final int TOOLBAR_Y = 30;
    private static final int CONTROL_HEIGHT = 18;
    private static final int ROW_HEIGHT = 22;
    private static final int CASE_PADDING = 6;
    private static final float MOUSE_GAP = 0.9f;
    private static final int LEGEND_LINE = 11;

    private final Screen parent;
    private BindingSnapshot snapshot;
    private List<String> names = List.of();
    private KeyCap selected;
    private int layerToggle;
    private boolean onlyConflicts;
    private String query = "";
    private String categoryFilter;
    private Integer waiting;
    private int page;
    private boolean dirty;
    private EditBox search;

    private float unit;
    private float keyboardX;
    private float keyboardY;
    private float mouseX0;
    private float mouseY0;
    private int panelTop;
    private int panelBottom;
    private int detailsRight;
    private int legendLeft;
    private final List<Integer> rowEntries = new ArrayList<>();
    private final List<LegendHit> legendHits = new ArrayList<>();

    public KeyboardScreen(Screen parent) {
        super(Component.translatable("qualityessentials.keyboard.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshSnapshot();
        layoutBoard();

        int x = 8;
        search = new EditBox(this.font, x, TOOLBAR_Y, 150, CONTROL_HEIGHT, Component.translatable("qualityessentials.keyboard.search.hint"));
        search.setHint(Component.translatable("qualityessentials.keyboard.search.hint"));
        search.setValue(query);
        search.setResponder(value -> query = value.trim().toLowerCase(Locale.ROOT));
        addRenderableWidget(search);
        x += 158;
        int[] layers = {0, Binding.CONTROL, Binding.SHIFT, Binding.ALT};
        String[] layerKeys = {"qualityessentials.keyboard.layer.none", "qualityessentials.keyboard.modifier.ctrl", "qualityessentials.keyboard.modifier.shift", "qualityessentials.keyboard.modifier.alt"};
        for (int i = 0; i < layers.length; i++) {
            int layer = layers[i];
            Component label = Component.translatable(layerKeys[i]);
            int width = this.font.width(label) + 14;
            addRenderableWidget(new FlatButton(x, TOOLBAR_Y, width, CONTROL_HEIGHT, label, FlatButton.Style.SEGMENT, () -> {
                layerToggle = layerToggle == layer ? 0 : layer;
            }).selectedWhen(() -> activeLayer() == layer));
            x += width - 1;
        }
        x += 8;
        Component conflictsLabel = Component.translatable("qualityessentials.keyboard.only_conflicts_short");
        addRenderableWidget(new FlatButton(x, TOOLBAR_Y, this.font.width(conflictsLabel) + 16, CONTROL_HEIGHT, conflictsLabel,
                FlatButton.Style.SECONDARY, () -> onlyConflicts = !onlyConflicts).selectedWhen(() -> onlyConflicts));

        buildPanelWidgets();
        addRenderableWidget(new FlatButton(this.width / 2 - 75, this.height - 25, 150, 20,
                Component.translatable("qualityessentials.keyboard.button.done"), FlatButton.Style.PRIMARY, this::onClose));
    }

    private void layoutBoard() {
        float totalUnits = KeyboardLayout.KEYBOARD_WIDTH + MOUSE_GAP + KeyboardLayout.MOUSE_WIDTH;
        int top = TOOLBAR_Y + CONTROL_HEIGHT + 10 + CASE_PADDING;
        int reservedBelow = 30 + 8 + 14 + ROW_HEIGHT * 4;
        unit = Math.min((this.width - 16 - CASE_PADDING * 4) / totalUnits,
                (this.height - top - reservedBelow - CASE_PADDING) / KeyboardLayout.KEYBOARD_HEIGHT);
        unit = Math.max(10f, unit);
        float boardWidth = totalUnits * unit + CASE_PADDING * 2;
        keyboardX = (this.width - boardWidth) / 2f;
        keyboardY = top;
        mouseX0 = keyboardX + (KeyboardLayout.KEYBOARD_WIDTH + MOUSE_GAP) * unit + CASE_PADDING * 2;
        mouseY0 = keyboardY + 1.5f * unit;
        panelTop = Math.round(keyboardY + KeyboardLayout.KEYBOARD_HEIGHT * unit) + CASE_PADDING + 8;
        panelBottom = this.height - 32;
        detailsRight = Math.round(this.width * 0.66f) - 4;
        legendLeft = detailsRight + 8;
    }

    private void refreshSnapshot() {
        snapshot = BindingSnapshot.capture(this.minecraft);
        List<String> translated = new ArrayList<>();
        for (KeyMapping mapping : snapshot.mappings()) {
            translated.add(Component.translatable(mapping.getName()).getString());
        }
        names = translated;
    }

    private List<Integer> panelEntries() {
        if (selected != null) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < snapshot.bindings().size(); i++) {
                if (snapshot.bindings().get(i).onKey(selected.mouse(), selected.code())) {
                    all.add(i);
                }
            }
            return all;
        }
        return snapshot.conflicting();
    }

    private int rowCapacity() {
        return Math.max(1, (panelBottom - panelTop - 18) / ROW_HEIGHT);
    }

    private void buildPanelWidgets() {
        rowEntries.clear();
        List<Integer> entries = panelEntries();
        int capacity = rowCapacity();
        int pages = Math.max(1, (entries.size() + capacity - 1) / capacity);
        page = Math.max(0, Math.min(page, pages - 1));
        int headerRight = detailsRight - 3;
        if (pages > 1) {
            FlatButton next = addRenderableWidget(new FlatButton(headerRight - 16, panelTop + 2, 16, 11, Component.literal(">"),
                    FlatButton.Style.SECONDARY, () -> {
                        page++;
                        rebuildWidgets();
                    }));
            next.active = page < pages - 1;
            FlatButton prev = addRenderableWidget(new FlatButton(headerRight - 34, panelTop + 2, 16, 11, Component.literal("<"),
                    FlatButton.Style.SECONDARY, () -> {
                        page--;
                        rebuildWidgets();
                    }));
            prev.active = page > 0;
            headerRight -= 38;
        }
        if (selected != null) {
            Component back = Component.translatable("qualityessentials.keyboard.button.back");
            int width = this.font.width(back) + 12;
            addRenderableWidget(new FlatButton(headerRight - width, panelTop + 2, width, 11, back, FlatButton.Style.SECONDARY, () -> {
                selected = null;
                page = 0;
                rebuildWidgets();
            }));
        }
        int y = panelTop + 18;
        for (int n = page * capacity; n < Math.min(entries.size(), (page + 1) * capacity); n++) {
            int index = entries.get(n);
            rowEntries.add(index);
            KeyMapping mapping = snapshot.mappings().get(index);
            int x = detailsRight - 4;
            x = rowButton(x, y, 48, "qualityessentials.keyboard.button.unbind", FlatButton.Style.DANGER, !mapping.isUnbound(),
                    () -> applyKey(mapping, InputConstants.UNKNOWN));
            x = rowButton(x, y, 48, "qualityessentials.keyboard.button.reset", FlatButton.Style.SECONDARY, !mapping.isDefault(),
                    () -> applyKey(mapping, mapping.getDefaultKey()));
            boolean isWaiting = waiting != null && waiting == index;
            x = rowButton(x, y, 64, isWaiting ? "qualityessentials.keyboard.button.waiting" : "qualityessentials.keyboard.button.change", FlatButton.Style.PRIMARY, true, () -> {
                waiting = isWaiting ? null : index;
                rebuildWidgets();
            });
            if (selected == null && !mapping.isUnbound()) {
                x = rowButton(x, y, 58, "qualityessentials.keyboard.button.show", FlatButton.Style.SECONDARY, true, () -> {
                    selected = capFor(snapshot.bindings().get(index));
                    page = 0;
                    rebuildWidgets();
                });
            }
            ModifierSource source = ModifierRegistry.sourceFor(mapping);
            if (source != null) {
                int[] bits = {Binding.ALT, Binding.SHIFT, Binding.CONTROL};
                String[] keys = {"qualityessentials.keyboard.modifier.alt", "qualityessentials.keyboard.modifier.shift", "qualityessentials.keyboard.modifier.ctrl"};
                for (int i = 0; i < bits.length; i++) {
                    int bit = bits[i];
                    Component label = Component.translatable(keys[i]);
                    int width = this.font.width(label) + 10;
                    x -= width;
                    addRenderableWidget(new FlatButton(x, y + 2, width, CONTROL_HEIGHT, label, FlatButton.Style.SEGMENT, () -> {
                        source.setModifiers(mapping, source.modifiers(mapping) ^ bit);
                        dirty = true;
                        refreshSnapshot();
                        rebuildWidgets();
                    }).selectedWhen(() -> (source.modifiers(mapping) & bit) != 0));
                    x -= 1;
                }
            }
            y += ROW_HEIGHT;
        }
    }

    private int rowButton(int right, int y, int width, String key, FlatButton.Style style, boolean active, Runnable action) {
        int x = right - width;
        FlatButton button = addRenderableWidget(new FlatButton(x, y + 2, width, CONTROL_HEIGHT, Component.translatable(key), style, action));
        button.active = active;
        return x - 3;
    }

    private KeyCap capFor(Binding binding) {
        List<KeyCap> caps = binding.mouse() ? KeyboardLayout.mouse() : KeyboardLayout.keyboard();
        for (KeyCap cap : caps) {
            if (cap.code() == binding.code()) {
                return cap;
            }
        }
        return null;
    }

    private void applyKey(KeyMapping mapping, InputConstants.Key key) {
        mapping.setKey(key);
        KeyMapping.resetMapping();
        dirty = true;
        waiting = null;
        refreshSnapshot();
        int index = snapshot.mappings().indexOf(mapping);
        if (selected != null && index >= 0 && key != InputConstants.UNKNOWN) {
            KeyCap moved = capFor(snapshot.bindings().get(index));
            if (moved != null) {
                selected = moved;
            }
        }
        rebuildWidgets();
    }

    private int activeLayer() {
        if (layerToggle != 0 || waiting != null || (search != null && search.isFocused())) {
            return layerToggle;
        }
        int held = 0;
        if (InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL)) {
            held |= Binding.CONTROL;
        }
        if (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)) {
            held |= Binding.SHIFT;
        }
        if (InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT)) {
            held |= Binding.ALT;
        }
        return held;
    }

    private float originX(KeyCap cap) {
        return cap.mouse() ? mouseX0 : keyboardX + CASE_PADDING;
    }

    private float originY(KeyCap cap) {
        return cap.mouse() ? mouseY0 : keyboardY;
    }

    private KeyCap capAt(double mx, double my) {
        for (KeyCap cap : KeyboardLayout.keyboard()) {
            if (cap.contains((float) ((mx - originX(cap)) / unit), (float) ((my - originY(cap)) / unit))) {
                return cap;
            }
        }
        for (KeyCap cap : KeyboardLayout.mouse()) {
            if (cap.contains((float) ((mx - originX(cap)) / unit), (float) ((my - originY(cap)) / unit))) {
                return cap;
            }
        }
        return null;
    }

    private boolean matchesFilters(int index) {
        if (categoryFilter != null && !snapshot.bindings().get(index).category().equals(categoryFilter)) {
            return false;
        }
        if (query.isEmpty()) {
            return true;
        }
        return names.get(index).toLowerCase(Locale.ROOT).contains(query)
                || snapshot.mappings().get(index).getCategory().label().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static InputConstants.Key keyOf(KeyCap cap) {
        return (cap.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYBOARD).getOrCreate(cap.code());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        KeyCap cap = capAt(event.x(), event.y());
        if (waiting != null) {
            KeyMapping mapping = snapshot.mappings().get(waiting);
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                if (cap != null) {
                    applyKey(mapping, keyOf(cap));
                    return true;
                }
                if (super.mouseClicked(event, doubleClick)) {
                    return true;
                }
                waiting = null;
                rebuildWidgets();
                return true;
            }
            applyKey(mapping, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (cap != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            selected = cap.equals(selected) ? null : cap;
            page = 0;
            rebuildWidgets();
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (LegendHit hit : legendHits) {
                if (event.x() >= hit.x0 && event.x() < hit.x1 && event.y() >= hit.y0 && event.y() < hit.y1) {
                    categoryFilter = hit.category.equals(categoryFilter) ? null : hit.category;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waiting != null) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                waiting = null;
                rebuildWidgets();
                return true;
            }
            applyKey(snapshot.mappings().get(waiting), InputConstants.getKey(event));
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE && (search == null || !search.isFocused() || search.getValue().isEmpty())) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.width, this.height, Palette.BACKGROUND);
        drawHeader(graphics);
        int layer = activeLayer();
        KeyCap hovered = capAt(mouseX, mouseY);
        drawCases(graphics, layer);
        for (KeyCap cap : KeyboardLayout.keyboard()) {
            drawCap(graphics, cap, layer, cap.equals(hovered));
        }
        for (KeyCap cap : KeyboardLayout.mouse()) {
            drawCap(graphics, cap, layer, cap.equals(hovered));
        }
        drawDetails(graphics);
        drawLegend(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        if (hovered != null && waiting == null) {
            graphics.setTooltipForNextFrame(this.font, tooltip(hovered, layer), Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, Palette.HEADER);
        graphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, Palette.BORDER);
        Component name = Component.translatable("qualityessentials.keyboard.title_short");
        graphics.text(this.font, name, 10, 8, Palette.TEXT_BRIGHT, true);
        graphics.text(this.font, Component.translatable("qualityessentials.keyboard.screen.subtitle"), 10 + this.font.width(name) + 8, 8, Palette.TEXT_DIM, false);
        ConflictAnalyzer.Result conflicts = snapshot.conflicts();
        long bound = snapshot.bindings().stream().filter(binding -> !binding.unbound()).count();
        long hard = conflicts.hardCount();
        long soft = conflicts.softCount();
        int x = this.width - 8;
        x = chip(graphics, x, Component.translatable("qualityessentials.keyboard.chip.soft", soft),
                soft > 0 ? Palette.WARNING : Palette.SURFACE, soft > 0 ? Palette.BADGE_TEXT : Palette.TEXT_DIM);
        x = chip(graphics, x, Component.translatable("qualityessentials.keyboard.chip.hard", hard),
                hard > 0 ? 0xFF8A2A30 : Palette.SURFACE, hard > 0 ? Palette.TEXT_BRIGHT : Palette.TEXT_DIM);
        chip(graphics, x, Component.translatable("qualityessentials.keyboard.chip.bound", bound), Palette.SURFACE, Palette.TEXT_DIM);
    }

    private int chip(GuiGraphicsExtractor graphics, int right, Component text, int background, int color) {
        int width = this.font.width(text) + 10;
        int left = right - width;
        graphics.fill(left, 6, right, 18, background);
        graphics.text(this.font, text, left + 5, 8, color, false);
        return left - 4;
    }

    private void drawCases(GuiGraphicsExtractor graphics, int layer) {
        int x0 = Math.round(keyboardX);
        int y0 = Math.round(keyboardY) - CASE_PADDING;
        int x1 = Math.round(keyboardX + KeyboardLayout.KEYBOARD_WIDTH * unit) + CASE_PADDING * 2;
        int y1 = Math.round(keyboardY + KeyboardLayout.KEYBOARD_HEIGHT * unit) + CASE_PADDING;
        graphics.fill(x0, y0, x1, y1, Palette.CASE);
        graphics.outline(x0, y0, x1 - x0, y1 - y0, Palette.BORDER);
        int mx0 = Math.round(mouseX0) - CASE_PADDING;
        int my0 = Math.round(mouseY0) - CASE_PADDING - 12;
        int mx1 = Math.round(mouseX0 + KeyboardLayout.MOUSE_WIDTH * unit) + CASE_PADDING;
        int my1 = Math.round(mouseY0 + KeyboardLayout.MOUSE_HEIGHT * unit) + CASE_PADDING;
        graphics.fill(mx0, my0, mx1, my1, Palette.CASE);
        graphics.outline(mx0, my0, mx1 - mx0, my1 - my0, Palette.BORDER);
        graphics.text(this.font, Component.translatable("qualityessentials.keyboard.mouse.title"), mx0 + CASE_PADDING, my0 + 4, Palette.TEXT_DIM, false);
        if (layer != 0) {
            Component active = Component.translatable("qualityessentials.keyboard.layer.active", modifierLabel(layer));
            graphics.text(this.font, active, mx0, my1 + 4, Palette.HIGHLIGHT, false);
        }
        if (waiting != null) {
            Component hint = Component.translatable("qualityessentials.keyboard.waiting", names.get(waiting));
            int width = this.font.width(hint) + 12;
            int cx = (x0 + x1) / 2;
            graphics.fill(cx - width / 2, y0 - 3, cx + width / 2, y0 + 9, Palette.ACCENT_DARK);
            graphics.text(this.font, hint, cx - width / 2 + 6, y0 - 1, Palette.TEXT_BRIGHT, false);
        }
    }

    private void drawCap(GuiGraphicsExtractor graphics, KeyCap cap, int layer, boolean hovered) {
        int x0 = Math.round(originX(cap) + cap.x() * unit) + 1;
        int y0 = Math.round(originY(cap) + cap.y() * unit) + 1;
        int x1 = Math.round(originX(cap) + (cap.x() + cap.width()) * unit) - 1;
        int y1 = Math.round(originY(cap) + (cap.y() + cap.height()) * unit) - 1;
        List<Integer> indices = snapshot.indicesOn(cap.mouse(), cap.code(), layer);
        ConflictAnalyzer.Level level = snapshot.worstOn(cap.mouse(), cap.code(), layer);
        boolean held = !cap.mouse() && InputConstants.isKeyDown(cap.code()) || isModifierCap(cap, layer);
        boolean dim = (onlyConflicts && level == ConflictAnalyzer.Level.NONE)
                || ((categoryFilter != null || !query.isEmpty()) && indices.stream().noneMatch(this::matchesFilters));
        int body = indices.isEmpty() ? Palette.CAP_FREE : Palette.CAP;
        int edge = indices.isEmpty() ? Palette.CAP_FREE_EDGE : Palette.CAP_EDGE;
        if (level == ConflictAnalyzer.Level.HARD) {
            body = Palette.CAP_CONFLICT;
            edge = Palette.CAP_CONFLICT_EDGE;
        }
        if (held) {
            body = Palette.CAP_HELD;
            edge = Palette.CAP_HELD_EDGE;
        } else if (hovered) {
            body = Palette.CAP_HOVER;
        }
        graphics.fill(x0, y0, x1, y1, edge);
        graphics.fill(x0, y0, x1, y1 - 2, body);
        if (!indices.isEmpty()) {
            graphics.fill(x0, y0, x1, y0 + 1, Palette.CAP_SHINE);
        }
        if (level == ConflictAnalyzer.Level.HARD) {
            graphics.outline(x0, y0, x1 - x0, y1 - y0, Palette.DANGER);
        } else if (level == ConflictAnalyzer.Level.SOFT) {
            graphics.outline(x0, y0, x1 - x0, y1 - y0, Palette.WARNING);
        }
        if (cap.equals(selected)) {
            graphics.outline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, Palette.HIGHLIGHT);
        }
        if (waiting != null && hovered) {
            graphics.outline(x0, y0, x1 - x0, y1 - y0, 0xFF55FF55);
        }
        int stripes = Math.min(3, indices.size());
        if (stripes > 0) {
            float width = (x1 - x0 - 4) / (float) stripes;
            for (int s = 0; s < stripes; s++) {
                int color = CategoryColors.of(snapshot.bindings().get(indices.get(s)).category());
                int sx0 = Math.round(x0 + 2 + s * width);
                int sx1 = Math.round(x0 + 2 + (s + 1) * width) - (s < stripes - 1 ? 1 : 0);
                graphics.fill(sx0, y1 - 5, sx1, y1 - 3, color);
            }
        }
        String label = cap.mouse() ? Component.translatable(cap.label()).getString() : cap.label();
        int inner = x1 - x0;
        boolean roomy = unit >= 30 && y1 - y0 >= 26;
        int labelColor = indices.isEmpty() ? Palette.TEXT_DISABLED : Palette.TEXT_BRIGHT;
        if (roomy && !indices.isEmpty()) {
            graphics.text(this.font, fit(label, inner - 6), x0 + 3, y0 + 3, labelColor, false);
            String bindName = names.get(indices.getFirst());
            graphics.text(this.font, fit(bindName, inner - 5), x0 + 3, y0 + 13,
                    level == ConflictAnalyzer.Level.HARD ? Palette.DANGER_TEXT : Palette.TEXT_SUBTLE, false);
        } else {
            String compact = KeyboardLayout.compactLabel(cap);
            String shown = this.font.width(label) > inner - 2 && compact != null ? compact : label;
            shown = fit(shown, inner - 2);
            graphics.text(this.font, shown, (x0 + x1 - this.font.width(shown)) / 2, (y0 + y1 - 2 - this.font.lineHeight) / 2 + 1, labelColor, false);
        }
        String russian = isRussian() && unit >= 26 ? KeyboardLayout.russianLegend(cap) : null;
        if (russian != null) {
            int ry = roomy && !indices.isEmpty() ? y0 + 3 : y1 - 13;
            graphics.text(this.font, russian, x1 - this.font.width(russian) - 3, ry, Palette.CAP_SECONDARY, false);
        }
        if (indices.size() > 1) {
            if (unit >= 26) {
                String more = "+" + (indices.size() - 1);
                int bx0 = x1 - this.font.width(more) - 4;
                graphics.fill(bx0, y0 + 1, x1 - 1, y0 + 10, Palette.BADGE);
                graphics.text(this.font, more, bx0 + 2, y0 + 2, Palette.BADGE_TEXT, false);
            } else {
                graphics.fill(x1 - 4, y0 + 1, x1 - 1, y0 + 4, Palette.BADGE);
            }
        }
        if (dim) {
            graphics.fill(x0, y0, x1, y1, Palette.DIM_OVERLAY);
        }
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width));
    }

    private boolean isRussian() {
        return this.minecraft != null && this.minecraft.options.languageCode != null
                && this.minecraft.options.languageCode.startsWith("ru");
    }

    private static boolean isModifierCap(KeyCap cap, int layer) {
        if (cap.mouse() || layer == 0) {
            return false;
        }
        int code = cap.code();
        return ((layer & Binding.CONTROL) != 0 && (code == InputConstants.KEY_LCONTROL || code == InputConstants.KEY_RCONTROL))
                || ((layer & Binding.SHIFT) != 0 && (code == InputConstants.KEY_LSHIFT || code == InputConstants.KEY_RSHIFT))
                || ((layer & Binding.ALT) != 0 && (code == InputConstants.KEY_LALT || code == InputConstants.KEY_RALT));
    }

    private void panelFrame(GuiGraphicsExtractor graphics, int x0, int x1, Component title) {
        graphics.fill(x0, panelTop, x1, panelBottom, Palette.PANEL);
        graphics.outline(x0, panelTop, x1 - x0, panelBottom - panelTop, Palette.BORDER);
        graphics.fill(x0 + 1, panelTop + 1, x1 - 1, panelTop + 15, Palette.HEADER);
        graphics.text(this.font, title, x0 + 6, panelTop + 4, Palette.HIGHLIGHT, false);
    }

    private void drawDetails(GuiGraphicsExtractor graphics) {
        Component title;
        if (selected != null) {
            title = Component.translatable("qualityessentials.keyboard.panel.key", keyOf(selected).getDisplayName(), panelEntries().size());
        } else if (snapshot.conflicting().isEmpty()) {
            title = Component.translatable("qualityessentials.keyboard.panel.no_conflicts");
        } else {
            title = Component.translatable("qualityessentials.keyboard.panel.conflicts", snapshot.conflicting().size());
        }
        panelFrame(graphics, 8, detailsRight, title);
        if (rowEntries.isEmpty()) {
            Component hint = selected != null ? Component.translatable("qualityessentials.keyboard.panel.free")
                    : Component.translatable("qualityessentials.keyboard.panel.hint");
            graphics.textWithWordWrap(this.font, hint, 16, panelTop + 24, detailsRight - 32, Palette.TEXT_DIM);
            return;
        }
        int y = panelTop + 18;
        for (int index : rowEntries) {
            drawRow(graphics, index, y);
            y += ROW_HEIGHT;
        }
    }

    private void drawRow(GuiGraphicsExtractor graphics, int index, int y) {
        Binding binding = snapshot.bindings().get(index);
        KeyMapping mapping = snapshot.mappings().get(index);
        if (waiting != null && waiting == index) {
            graphics.fill(9, y, detailsRight - 1, y + ROW_HEIGHT, Palette.SURFACE_SELECTED);
        }
        graphics.fill(14, y + 4, 20, y + 10, CategoryColors.of(binding.category()));
        ConflictAnalyzer.Level level = snapshot.conflicts().levelOf(index);
        int nameColor = level == ConflictAnalyzer.Level.HARD ? Palette.DANGER : level == ConflictAnalyzer.Level.SOFT ? Palette.WARNING : Palette.TEXT;
        int maxWidth = buttonsLeftEdge(y + 2) - 30;
        graphics.text(this.font, fit(names.get(index), maxWidth), 26, y + 2, nameColor, false);
        MutableComponent sub = Component.empty()
                .append(keyLabel(binding, mapping).copy().withStyle(style -> style.withColor(Palette.TEXT_BRIGHT)))
                .append(Component.literal("  ").append(mapping.getCategory().label()).withStyle(style -> style.withColor(Palette.TEXT_DIM)));
        if (level != ConflictAnalyzer.Level.NONE) {
            List<String> partners = new ArrayList<>();
            for (int partner : snapshot.conflicts().partnersOf(index)) {
                partners.add(names.get(partner));
            }
            sub.append(Component.literal("  ")).append(Component.translatable(
                    level == ConflictAnalyzer.Level.HARD ? "qualityessentials.keyboard.conflict.hard" : "qualityessentials.keyboard.conflict.soft",
                    String.join(", ", partners)).withStyle(style -> style.withColor(nameColor)));
        }
        String plain = sub.getString();
        if (this.font.width(sub) > maxWidth) {
            graphics.text(this.font, fit(plain, maxWidth), 26, y + 12, Palette.TEXT_DIM, false);
        } else {
            graphics.text(this.font, sub, 26, y + 12, Palette.TEXT_DIM, false);
        }
    }

    private int buttonsLeftEdge(int y) {
        int left = detailsRight;
        for (var child : children()) {
            if (child instanceof FlatButton button && button.getY() == y && button.getX() < left) {
                left = button.getX();
            }
        }
        return left;
    }

    private void drawLegend(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x1 = this.width - 8;
        panelFrame(graphics, legendLeft, x1, Component.translatable("qualityessentials.keyboard.legend.title"));
        int y = panelTop + 19;
        int x = legendLeft + 6;
        int[] colors = {Palette.DANGER, Palette.WARNING, Palette.CAP_HELD, Palette.CAP_FREE};
        String[] keys = {"qualityessentials.keyboard.legend.conflict", "qualityessentials.keyboard.legend.soft", "qualityessentials.keyboard.legend.held", "qualityessentials.keyboard.legend.free"};
        int columnWidth = (x1 - legendLeft - 12) / 2;
        for (int i = 0; i < colors.length; i++) {
            int cx = x + (i % 2) * columnWidth;
            int cy = y + (i / 2) * LEGEND_LINE;
            graphics.fill(cx, cy, cx + 8, cy + 8, colors[i]);
            graphics.outline(cx, cy, 8, 8, Palette.BORDER);
            graphics.text(this.font, fit(Component.translatable(keys[i]).getString(), columnWidth - 14), cx + 12, cy, Palette.TEXT, false);
        }
        y += LEGEND_LINE * 2 + 4;
        graphics.text(this.font, Component.translatable(categoryFilter == null ? "qualityessentials.keyboard.legend.categories" : "qualityessentials.keyboard.legend.categories_filtered"),
                x, y, Palette.TEXT_DIM, false);
        y += LEGEND_LINE;
        legendHits.clear();
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, Component> labels = new LinkedHashMap<>();
        for (int i = 0; i < snapshot.bindings().size(); i++) {
            Binding binding = snapshot.bindings().get(i);
            if (binding.unbound()) {
                continue;
            }
            counts.computeIfAbsent(binding.category(), key -> new int[1])[0]++;
            labels.putIfAbsent(binding.category(), snapshot.mappings().get(i).getCategory().label());
        }
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        int rows = Math.max(1, (panelBottom - y - 4) / LEGEND_LINE);
        for (int i = 0; i < sorted.size(); i++) {
            int column = i / rows;
            if (column > 1) {
                break;
            }
            int cx = x + column * columnWidth;
            int cy = y + (i % rows) * LEGEND_LINE;
            String category = sorted.get(i).getKey();
            boolean active = category.equals(categoryFilter);
            boolean hovered = mouseX >= cx && mouseX < cx + columnWidth - 4 && mouseY >= cy - 1 && mouseY < cy + LEGEND_LINE - 1;
            if (active || hovered) {
                graphics.fill(cx - 2, cy - 1, cx + columnWidth - 4, cy + LEGEND_LINE - 1, active ? Palette.SURFACE_SELECTED : Palette.SURFACE_HOVER);
            }
            graphics.fill(cx, cy + 1, cx + 6, cy + 7, CategoryColors.of(category));
            String text = labels.get(category).getString() + " " + sorted.get(i).getValue()[0];
            graphics.text(this.font, fit(text, columnWidth - 16), cx + 10, cy, active ? Palette.HIGHLIGHT : Palette.TEXT, false);
            legendHits.add(new LegendHit(category, cx - 2, cy - 1, cx + columnWidth - 4, cy + LEGEND_LINE - 1));
        }
    }

    private Component keyLabel(Binding binding, KeyMapping mapping) {
        if (binding.unbound()) {
            return Component.translatable("qualityessentials.keyboard.unbound");
        }
        Component key = mapping.getTranslatedKeyMessage();
        if (binding.modifiers() == 0) {
            return key;
        }
        return modifierLabel(binding.modifiers()).copy().append(" + ").append(key);
    }

    private static Component modifierLabel(int mask) {
        MutableComponent text = Component.empty();
        String[] keys = {"qualityessentials.keyboard.modifier.ctrl", "qualityessentials.keyboard.modifier.shift", "qualityessentials.keyboard.modifier.alt"};
        int[] bits = {Binding.CONTROL, Binding.SHIFT, Binding.ALT};
        boolean first = true;
        for (int i = 0; i < bits.length; i++) {
            if ((mask & bits[i]) != 0) {
                if (!first) {
                    text.append(" + ");
                }
                text.append(Component.translatable(keys[i]));
                first = false;
            }
        }
        return text;
    }

    private List<Component> tooltip(KeyCap cap, int layer) {
        List<Component> lines = new ArrayList<>();
        Component keyName = keyOf(cap).getDisplayName();
        lines.add(layer == 0 ? keyName.copy().withStyle(style -> style.withColor(Palette.HIGHLIGHT))
                : modifierLabel(layer).copy().append(" + ").append(keyName).withStyle(style -> style.withColor(Palette.HIGHLIGHT)));
        List<Integer> indices = snapshot.indicesOn(cap.mouse(), cap.code(), layer);
        if (indices.isEmpty()) {
            lines.add(Component.translatable("qualityessentials.keyboard.tooltip.free").withStyle(style -> style.withColor(Palette.TEXT_DIM)));
        }
        for (int index : indices) {
            ConflictAnalyzer.Level level = snapshot.conflicts().levelOf(index);
            int color = level == ConflictAnalyzer.Level.HARD ? Palette.DANGER_TEXT : level == ConflictAnalyzer.Level.SOFT ? Palette.WARNING : Palette.TEXT;
            lines.add(Component.literal("- " + names.get(index)).withStyle(style -> style.withColor(color))
                    .append(Component.literal("  " + snapshot.mappings().get(index).getCategory().label().getString())
                            .withStyle(style -> style.withColor(Palette.TEXT_DIM))));
        }
        lines.add(Component.translatable("qualityessentials.keyboard.tooltip.click").withStyle(style -> style.withColor(Palette.TEXT_DIM)));
        return lines;
    }

    @Override
    public void removed() {
        if (dirty && this.minecraft != null) {
            this.minecraft.options.save();
            dirty = false;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private record LegendHit(String category, int x0, int y0, int x1, int y1) {
    }
}
