package org.lts.qualityessentials.signs;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.lts.qualityessentials.core.EditHistory;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.CodeButton;
import org.lts.qualityessentials.gui.FlatButton;
import org.lts.qualityessentials.gui.Notice;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.ScreenState;
import org.lts.qualityessentials.gui.ToolPanel;
import org.lts.qualityessentials.gui.Ui;
import org.lts.qualityessentials.mixin.AbstractSignEditScreenAccessor;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SignEditor {
    private static final char AMP = FormatCode.AMPERSAND;
    private static final int SIDE_GAP = 62;
    private static final int LEFT_WIDTH = 64;

    private final AbstractSignEditScreen screen;
    private final EditHistory<Lines> history = new EditHistory<>(100, 1200L, Util::getMillis, Lines::sameText);
    private final Notice notice = new Notice();
    private final List<AbstractWidget> own = new ArrayList<>();
    private boolean filled;
    private boolean confirmPending;
    private ToolPanel left;
    private ToolPanel right;
    private FlatButton recallButton;
    private FlatButton undoButton;
    private FlatButton redoButton;

    private SignEditor(AbstractSignEditScreen screen) {
        this.screen = screen;
    }

    public static void attach(AbstractSignEditScreen screen, Minecraft minecraft, int width) {
        SignEditor editor = ScreenState.get(screen, SignEditor.class);
        if (editor == null) {
            editor = new SignEditor(screen);
            ScreenState.set(screen, editor);
        }
        editor.build(minecraft, width);
    }

    private record Lines(List<String> lines, int line) {
        static boolean sameText(Lines a, Lines b) {
            return a.lines.equals(b.lines);
        }
    }

    private AbstractSignEditScreenAccessor access() {
        return (AbstractSignEditScreenAccessor) screen;
    }

    private String[] messages() {
        return access().qualityessentials$messages();
    }

    private TextFieldHelper field() {
        return access().qualityessentials$signField();
    }

    private Lines state() {
        return new Lines(List.of(messages().clone()), access().qualityessentials$line());
    }

    private Predicate<String> fits() {
        Font font = Minecraft.getInstance().font;
        int max = access().qualityessentials$sign().getMaxTextLineWidth();
        return line -> font.width(line) <= max;
    }

    private void build(Minecraft minecraft, int width) {
        QEConfig config = QE.config();
        own.clear();
        left = null;
        right = null;
        recallButton = null;
        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        int center = width / 2;
        int top = 50;
        if (config.signsPalette) {
            left = new ToolPanel(Math.max(2, center - SIDE_GAP - LEFT_WIDTH), top, LEFT_WIDTH, widget -> {
                widgets.add(widget);
                own.add(widget);
            });
            buildPalette(left);
        }
        if (config.signsTools) {
            int x = center + SIDE_GAP;
            int w = Math.max(64, Math.min(120, width - x - 2));
            x = Math.min(x, width - w - 2);
            right = new ToolPanel(x, top, w, widget -> {
                widgets.add(widget);
                own.add(widget);
            });
            buildTools(right);
        }
        history.observe(state(), false);
        if (!filled) {
            filled = true;
            autoFill(minecraft, config);
            history.observe(state(), false);
        }
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(event));
        ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> history.observe(state(), true));
        ScreenKeyboardEvents.afterCharType(screen).register((s, event) -> history.observe(state(), true));
        ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !clicked(event));
        ScreenMouseEvents.afterMouseClick(screen).register((s, event, consumed) -> {
            if (own.contains(s.getFocused())) {
                s.setFocused(null);
            }
            return consumed;
        });
        ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) -> {
            if (left != null) {
                left.drawBackground(graphics);
            }
            if (right != null) {
                right.drawBackground(graphics);
            }
        });
        ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> drawOverlay(graphics));
        ScreenEvents.afterTick(screen).register(s -> tick());
        refreshButtons();
    }

    private void buildPalette(ToolPanel panel) {
        panel.section(Component.translatable("qualityessentials.signs.codes"));
        List<FormatCode> colors = FormatCode.colors();
        int x = panel.innerX();
        int y = panel.cursorY();
        int cell = (panel.innerWidth() + 2) / 4;
        Component hint = Component.translatable("qualityessentials.signs.code.hint");
        for (int i = 0; i < colors.size(); i++) {
            FormatCode code = colors.get(i);
            panel.place(new CodeButton(x + (i % 4) * cell, y + (i / 4) * cell, cell - 2, cell - 2, code, hint, () -> false, this::insertCode));
        }
        panel.skip(cell * 4 + 3);
        y = panel.cursorY();
        List<FormatCode> styles = FormatCode.styles();
        int wide = (panel.innerWidth() + 2) / 3;
        for (int i = 0; i < styles.size(); i++) {
            panel.place(new CodeButton(x + (i % 3) * wide, y + (i / 3) * 16, wide - 2, 14, styles.get(i), hint, () -> false, this::insertCode));
        }
        panel.skip(32);
        panel.finish();
    }

    private void buildTools(ToolPanel panel) {
        panel.section(Component.translatable("qualityessentials.signs.section.text"));
        panel.row(ToolPanel.Tool.of("qualityessentials.signs.copy", this::copy), ToolPanel.Tool.of("qualityessentials.signs.paste", this::pasteAll));
        panel.row(ToolPanel.Tool.danger("qualityessentials.signs.clear", this::clear));
        recallButton = panel.row(ToolPanel.Tool.of("qualityessentials.signs.recall", this::recall))[0];
        FlatButton[] historyRow = panel.row(ToolPanel.Tool.of("qualityessentials.books.undo", this::undo),
                ToolPanel.Tool.of("qualityessentials.books.redo", this::redo));
        undoButton = historyRow[0];
        redoButton = historyRow[1];
        panel.finish();
    }

    private void refreshButtons() {
        if (recallButton != null) {
            recallButton.active = SignModule.remembered() != null;
        }
        if (undoButton != null) {
            Lines live = state();
            undoButton.active = history.canUndo(live);
            redoButton.active = history.canRedo(live);
        }
    }

    private void tick() {
        refreshButtons();
        if (confirmPending) {
            confirmPending = false;
            screen.onClose();
        }
    }

    private void autoFill(Minecraft minecraft, QEConfig config) {
        String[] remembered = SignModule.remembered();
        if (!config.signsAutoFill || remembered == null || !SignLines.blank(messages()) || minecraft.player == null || !holdsSign(minecraft)) {
            return;
        }
        boolean cut = fill(List.of(remembered));
        if (config.signsAutoConfirm && !cut) {
            confirmPending = true;
        } else if (!cut) {
            notice.show(Component.translatable("qualityessentials.signs.notice.filled"), false);
        }
    }

    private static boolean holdsSign(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.is(ItemTags.SIGNS) || stack.is(ItemTags.HANGING_SIGNS)) {
                return true;
            }
        }
        return false;
    }

    private void drawOverlay(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        if (left != null) {
            left.drawLabels(graphics, font);
        }
        if (right != null) {
            right.drawLabels(graphics, font);
        }
        if (left != null) {
            if (right != null) {
                preview(graphics, font, right.x(), right.bottom() + 4, right.width());
            } else {
                preview(graphics, font, left.x(), left.bottom() + 4, left.width());
            }
        }
        notice.drawBelow(graphics, font, screen.width / 2, 12, 240, screen.height);
    }

    private boolean preview(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
        String[] lines = messages();
        boolean any = false;
        for (String line : lines) {
            any |= FormatCodes.hasCodes(line, AMP);
        }
        if (!any) {
            return false;
        }
        int panelWidth = Math.max(width, 100);
        int px = Math.max(2, Math.min(x, screen.width - panelWidth - 2));
        Ui.panel(graphics, px, y, panelWidth, 54, Palette.CASE, Palette.BORDER);
        graphics.text(font, Component.translatable("qualityessentials.signs.preview"), px + 4, y + 3, Palette.TEXT_DIM, false);
        for (int i = 0; i < lines.length; i++) {
            String shown = FormatCodes.convert(lines[i], AMP, FormatCode.SECTION);
            int w = font.width(shown);
            graphics.text(font, shown, px + (panelWidth - w) / 2, y + 13 + i * 10, Palette.TEXT_BRIGHT, true);
        }
        return true;
    }

    private boolean keyPressed(KeyEvent event) {
        int key = event.shortcutKey();
        if (key == InputConstants.KEY_TAB) {
            moveLine(event.hasShiftDown() ? -1 : 1);
            return true;
        }
        if (!event.hasControlDownWithQuirk() || event.hasAltDown()) {
            return false;
        }
        if (key == InputConstants.KEY_Z) {
            if (event.hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (key == InputConstants.KEY_Y) {
            redo();
            return true;
        }
        if (key == InputConstants.KEY_C && event.hasShiftDown()) {
            copy();
            return true;
        }
        if (key == InputConstants.KEY_V) {
            if (event.hasShiftDown()) {
                pasteAll();
                return true;
            }
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard.indexOf('\n') >= 0 || clipboard.indexOf('\r') >= 0) {
                pasteAt(clipboard);
                return true;
            }
        }
        return false;
    }

    private void moveLine(int step) {
        access().qualityessentials$setLine(Math.floorMod(access().qualityessentials$line() + step, SignLines.LINES));
        field().setCursorToEnd();
    }

    private boolean clicked(MouseButtonEvent event) {
        if (!QE.config().signsMouse || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        Font font = Minecraft.getInstance().font;
        Vector3fc scale = access().qualityessentials$signTextScale();
        float offsetY = access().qualityessentials$signYOffset();
        int lineHeight = access().qualityessentials$sign().getTextLineHeight();
        int maxWidth = access().qualityessentials$sign().getMaxTextLineWidth();
        double localX = (event.x() - screen.width / 2.0) / scale.x();
        double localY = (event.y() - offsetY) / scale.y() + SignLines.LINES * lineHeight / 2.0;
        if (Math.abs(localX) > maxWidth / 2.0 + 4 || localY < 0 || localY >= SignLines.LINES * lineHeight) {
            return false;
        }
        int line = (int) (localY / lineHeight);
        String text = messages()[line];
        double fromLeft = localX + font.width(text) / 2.0;
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i <= text.length(); i++) {
            double distance = Math.abs(font.width(text.substring(0, i)) - fromLeft);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        boolean extend = event.hasShiftDown() && line == access().qualityessentials$line();
        access().qualityessentials$setLine(line);
        field().setCursorPos(best, extend);
        return true;
    }

    private void insertCode(FormatCode code) {
        String code2 = code.with(AMP);
        String line = messages()[access().qualityessentials$line()];
        if (!fits().test(line + code2) && field().getCursorPos() == field().getSelectionPos()) {
            notice.show(Component.translatable("qualityessentials.signs.notice.no_room"), true);
            return;
        }
        field().insertText(code2);
        history.observe(state(), false);
    }

    private void setAll(String[] lines) {
        int line = access().qualityessentials$line();
        for (int i = 0; i < SignLines.LINES; i++) {
            access().qualityessentials$setLine(i);
            access().qualityessentials$setMessage(lines[i] == null ? "" : lines[i]);
        }
        access().qualityessentials$setLine(line);
        field().setCursorToEnd();
    }

    private void copy() {
        String[] lines = messages().clone();
        Minecraft.getInstance().keyboardHandler.setClipboard(SignLines.join(lines));
        if (!SignLines.blank(lines)) {
            SignModule.remember(lines);
        }
        notice.show(Component.translatable("qualityessentials.signs.notice.copied"), false);
    }

    private void pasteAll() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) {
            notice.show(Component.translatable("qualityessentials.signs.notice.clipboard_empty"), true);
            return;
        }
        fill(SignLines.parts(clipboard));
        history.observe(state(), false);
    }

    private boolean fill(List<String> lines) {
        String[] fitted = SignLines.fitAll(lines, fits());
        setAll(fitted);
        boolean cut = false;
        for (int i = 0; i < lines.size(); i++) {
            cut |= i < SignLines.LINES ? !fitted[i].equals(lines.get(i)) : !lines.get(i).isBlank();
        }
        if (cut) {
            notice.show(Component.translatable("qualityessentials.signs.notice.cut"), true);
        }
        return cut;
    }

    private void pasteAt(String clipboard) {
        int line = access().qualityessentials$line();
        String[] lines = SignLines.paste(messages(), line, field().getCursorPos(), field().getSelectionPos(), clipboard, fits());
        setAll(lines);
        access().qualityessentials$setLine(SignLines.lastPastedLine(line, clipboard));
        field().setCursorToEnd();
        history.observe(state(), false);
    }

    private void clear() {
        setAll(new String[SignLines.LINES]);
        access().qualityessentials$setLine(0);
        field().setCursorToEnd();
        history.observe(state(), false);
    }

    private void recall() {
        String[] remembered = SignModule.remembered();
        if (remembered == null) {
            return;
        }
        fill(List.of(remembered));
        history.observe(state(), false);
    }

    private void undo() {
        Lines target = history.undo(state());
        if (target != null) {
            restore(target);
        }
    }

    private void redo() {
        Lines target = history.redo(state());
        if (target != null) {
            restore(target);
        }
    }

    private void restore(Lines lines) {
        setAll(lines.lines().toArray(new String[0]));
        access().qualityessentials$setLine(Math.max(0, Math.min(lines.line(), SignLines.LINES - 1)));
        field().setCursorToEnd();
    }
}
