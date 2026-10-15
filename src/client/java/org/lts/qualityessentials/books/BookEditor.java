package org.lts.qualityessentials.books;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.lts.qualityessentials.core.EditHistory;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.CodeButton;
import org.lts.qualityessentials.gui.FlatButton;
import org.lts.qualityessentials.gui.Notice;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.ScreenState;
import org.lts.qualityessentials.gui.ToolPanel;
import org.lts.qualityessentials.mixin.BookEditScreenAccessor;
import org.lts.qualityessentials.mixin.MultilineTextFieldAccessor;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;

public final class BookEditor {
    private static final char S = FormatCode.SECTION;
    private static final int LEFT_WIDTH = 64;
    private static final int RIGHT_MAX = 140;
    private static final int BOOK_TEXT_LEFT = 20;
    private static final int BOOK_TEXT_RIGHT = 166;
    private static final int MENU_TOP = 196;

    private final BookEditScreen screen;
    private final EditHistory<State> history = new EditHistory<>(200, 1200L, Util::getMillis, State::sameText);
    private final Notice notice = new Notice();
    private final List<AbstractWidget> own = new ArrayList<>();
    private ToolPanel left;
    private ToolPanel right;
    private FlatButton deleteButton;
    private FlatButton moveLeftButton;
    private FlatButton moveRightButton;
    private FlatButton undoButton;
    private FlatButton redoButton;
    private int statusY;
    private State beforeImport;

    private BookEditor(BookEditScreen screen) {
        this.screen = screen;
    }

    public static void attach(BookEditScreen screen, Minecraft minecraft, int width, int height) {
        BookEditor editor = ScreenState.get(screen, BookEditor.class);
        if (editor == null) {
            editor = new BookEditor(screen);
            ScreenState.set(screen, editor);
        }
        editor.build(minecraft, width);
    }

    private record State(List<String> pages, int page, int cursor, int select) {
        static boolean sameText(State a, State b) {
            return a.pages.equals(b.pages);
        }
    }

    private BookEditScreenAccessor access() {
        return (BookEditScreenAccessor) screen;
    }

    private List<String> pages() {
        return access().qualityessentials$pages();
    }

    private MultiLineEditBox pageBox() {
        return access().qualityessentials$page();
    }

    private MultilineTextField field() {
        return ((RichPage) pageBox()).qualityessentials$field();
    }

    private int selectCursor() {
        return ((MultilineTextFieldAccessor) field()).qualityessentials$selectCursor();
    }

    private State state() {
        return new State(List.copyOf(pages()), access().qualityessentials$currentPage(), field().cursor(), selectCursor());
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private void build(Minecraft minecraft, int width) {
        QEConfig config = QE.config();
        ((RichPage) pageBox()).qualityessentials$setRich(true, config.booksFormatting && config.booksCodeMarks);
        own.clear();
        left = null;
        right = null;
        history.observe(state(), false);
        int bookLeft = (width - 192) / 2;
        int top = 16;
        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        if (config.booksFormatting) {
            left = new ToolPanel(Math.max(2, bookLeft + BOOK_TEXT_LEFT - 2 - LEFT_WIDTH), top, LEFT_WIDTH, widget -> {
                widgets.add(widget);
                own.add(widget);
            });
            buildFormatting(left);
        }
        if (config.booksPageTools) {
            int x = bookLeft + BOOK_TEXT_RIGHT + 4;
            int w = Math.max(64, Math.min(RIGHT_MAX, width - x - 2));
            x = Math.min(x, width - w - 2);
            right = new ToolPanel(x, top, w, widget -> {
                widgets.add(widget);
                own.add(widget);
            });
            buildTools(right);
        }
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(event));
        ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> afterInput());
        ScreenKeyboardEvents.afterCharType(screen).register((s, event) -> afterInput());
        ScreenMouseEvents.afterMouseClick(screen).register((s, event, consumed) -> {
            if (own.contains(s.getFocused())) {
                s.setFocused(pageBox());
            }
            normalizeCursor();
            return consumed;
        });
        ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> !scrolled(mouseX, mouseY, vertical));
        ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) -> {
            if (left != null) {
                left.drawBackground(graphics);
            }
            if (right != null) {
                right.drawBackground(graphics);
            }
        });
        ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> drawOverlay(graphics));
        ScreenEvents.afterTick(screen).register(s -> refreshButtons());
        refreshButtons();
    }

    private void buildFormatting(ToolPanel panel) {
        panel.section(Component.translatable("qualityessentials.books.colors"));
        List<FormatCode> colors = FormatCode.colors();
        int x = panel.innerX();
        int y = panel.cursorY();
        int cell = (panel.innerWidth() + 2) / 4;
        Component colorHint = Component.translatable("qualityessentials.books.color.hint");
        for (int i = 0; i < colors.size(); i++) {
            FormatCode code = colors.get(i);
            panel.place(new CodeButton(x + (i % 4) * cell, y + (i / 4) * cell, cell - 2, cell - 2, code, colorHint, () -> active(code), this::apply));
        }
        panel.skip(cell * 4 + 3);
        panel.section(Component.translatable("qualityessentials.books.styles"));
        List<FormatCode> styles = FormatCode.styles();
        y = panel.cursorY();
        int wide = (panel.innerWidth() + 2) / 3;
        for (int i = 0; i < styles.size(); i++) {
            FormatCode code = styles.get(i);
            panel.place(new CodeButton(x + (i % 3) * wide, y + (i / 3) * 16, wide - 2, 14, code, styleHint(code), () -> active(code), this::apply));
        }
        panel.skip(32);
        panel.finish();
    }

    private static Component styleHint(FormatCode code) {
        return switch (code) {
            case BOLD -> Component.literal("Ctrl+B");
            case ITALIC -> Component.literal("Ctrl+I");
            case UNDERLINE -> Component.literal("Ctrl+U");
            case RESET -> Component.translatable("qualityessentials.books.reset.hint");
            default -> null;
        };
    }

    private void buildTools(ToolPanel panel) {
        panel.section(Component.translatable("qualityessentials.books.section.page"));
        FlatButton[] pageRow = panel.row(ToolPanel.Tool.of("qualityessentials.books.insert", this::insertPage),
                ToolPanel.Tool.danger("qualityessentials.books.delete", this::deletePage));
        deleteButton = pageRow[1];
        FlatButton[] moveRow = panel.row(ToolPanel.Tool.of("qualityessentials.books.move_left", () -> movePage(-1)),
                ToolPanel.Tool.of("qualityessentials.books.move_right", () -> movePage(1)));
        moveLeftButton = moveRow[0];
        moveRightButton = moveRow[1];
        panel.section(Component.translatable("qualityessentials.books.section.copy"));
        panel.row(ToolPanel.Tool.of("qualityessentials.books.copy_page", this::copyPage), ToolPanel.Tool.of("qualityessentials.books.copy_book", this::copyBook));
        panel.row(ToolPanel.Tool.of("qualityessentials.books.paste", () -> paste(Minecraft.getInstance().keyboardHandler.getClipboard())));
        panel.section(Component.translatable("qualityessentials.books.section.file"));
        panel.row(ToolPanel.Tool.of("qualityessentials.books.save", this::save), ToolPanel.Tool.of("qualityessentials.books.load", this::openImport));
        panel.section(Component.translatable("qualityessentials.books.section.edit"));
        FlatButton[] historyRow = panel.row(ToolPanel.Tool.of("qualityessentials.books.undo", this::undo),
                ToolPanel.Tool.of("qualityessentials.books.redo", this::redo));
        undoButton = historyRow[0];
        redoButton = historyRow[1];
        statusY = panel.cursorY() + 2;
        if (statusY + 30 <= MENU_TOP - 4) {
            panel.skip(30);
        } else {
            statusY = -1;
        }
        panel.finish();
    }

    private void refreshButtons() {
        if (right == null) {
            return;
        }
        int current = access().qualityessentials$currentPage();
        int count = pages().size();
        deleteButton.active = count > 1 || !field().value().isEmpty();
        moveLeftButton.active = current > 0;
        moveRightButton.active = current < count - 1;
        State live = state();
        undoButton.active = history.canUndo(live);
        redoButton.active = history.canRedo(live);
    }

    private void drawOverlay(GuiGraphicsExtractor graphics) {
        Font font = font();
        if (left != null) {
            left.drawLabels(graphics, font);
        }
        if (right != null) {
            right.drawLabels(graphics, font);
        }
        notice.drawBelow(graphics, font, screen.width / 2, MENU_TOP + 24, 220, screen.height);
        if (right != null && statusY >= 0) {
            String value = field().value();
            int lines = RichPages.lineCount(font, value);
            int x = right.innerX();
            graphics.text(font, Component.translatable("qualityessentials.books.status.chars", value.length(), BookText.PAGE_LENGTH), x, statusY,
                    value.length() >= BookText.PAGE_LENGTH - 24 ? Palette.WARNING : Palette.TEXT_SUBTLE, false);
            graphics.text(font, Component.translatable("qualityessentials.books.status.lines", lines, RichPages.LINE_LIMIT), x, statusY + 10,
                    lines >= RichPages.LINE_LIMIT ? Palette.WARNING : Palette.TEXT_SUBTLE, false);
            graphics.text(font, Component.translatable("qualityessentials.books.status.pages", pages().size(), BookText.MAX_PAGES), x, statusY + 20,
                    pages().size() >= BookText.MAX_PAGES ? Palette.WARNING : Palette.TEXT_SUBTLE, false);
        }
    }

    private boolean scrolled(double mouseX, double mouseY, double vertical) {
        if (!QE.config().booksWheel || vertical == 0 || left != null && left.contains(mouseX, mouseY) || right != null && right.contains(mouseX, mouseY)) {
            return false;
        }
        goTo(access().qualityessentials$currentPage() + (vertical > 0 ? -1 : 1));
        return true;
    }

    private void afterInput() {
        normalizeCursor();
        history.observe(state(), true);
    }

    private void normalizeCursor() {
        MultilineTextField field = field();
        String value = field.value();
        int cursor = field.cursor();
        int fixed = FormatCodes.afterCode(value, cursor, S);
        if (fixed != cursor) {
            MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
            boolean selecting = selectCursor() != cursor;
            access.qualityessentials$setCursor(fixed);
            if (!selecting) {
                access.qualityessentials$setSelectCursor(fixed);
            }
        }
    }

    private boolean keyPressed(KeyEvent event) {
        QEConfig config = QE.config();
        int key = event.shortcutKey();
        boolean pageFocused = screen.getFocused() == pageBox();
        if (event.hasControlDownWithQuirk() && !event.hasAltDown()) {
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
            if (key == InputConstants.KEY_PAGEUP) {
                goTo(0);
                return true;
            }
            if (key == InputConstants.KEY_PAGEDOWN) {
                goTo(pages().size() - 1);
                return true;
            }
            if (!pageFocused) {
                return false;
            }
            if (key == InputConstants.KEY_V && config.booksSmartPaste) {
                paste(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                pageBreak();
                return true;
            }
            if (config.booksFormatting) {
                FormatCode style = key == InputConstants.KEY_B ? FormatCode.BOLD
                        : key == InputConstants.KEY_I ? FormatCode.ITALIC
                        : key == InputConstants.KEY_U ? FormatCode.UNDERLINE : null;
                if (style != null) {
                    apply(style);
                    return true;
                }
            }
            return false;
        }
        if (!pageFocused || event.hasAltDown() || event.hasControlDownWithQuirk()) {
            return false;
        }
        MultilineTextField field = field();
        String value = field.value();
        if (!FormatCodes.hasCodes(value, S)) {
            return false;
        }
        int cursor = field.cursor();
        boolean selection = selectCursor() != cursor;
        if (key == InputConstants.KEY_BACKSPACE && !selection) {
            setPage(FormatCodes.deleteBack(value, cursor, S));
            afterInput();
            return true;
        }
        if (key == InputConstants.KEY_DELETE && !selection) {
            setPage(FormatCodes.deleteForward(value, cursor, S));
            afterInput();
            return true;
        }
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) {
            int target = key == InputConstants.KEY_LEFT ? FormatCodes.stepLeft(value, cursor, S) : FormatCodes.stepRight(value, cursor, S);
            MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
            access.qualityessentials$setCursor(target);
            if (!event.hasShiftDown()) {
                access.qualityessentials$setSelectCursor(target);
            }
            return true;
        }
        return false;
    }

    private boolean active(FormatCode code) {
        MultilineTextField field = field();
        String value = field.value();
        return code != FormatCode.RESET && FormatCodes.stateAt(value, FormatCodes.afterCode(value, field.cursor(), S), S).has(code);
    }

    private void apply(FormatCode code) {
        MultilineTextField field = field();
        String value = field.value();
        int cursor = FormatCodes.afterCode(value, field.cursor(), S);
        int select = FormatCodes.afterCode(value, selectCursor(), S);
        FormatCodes.Edit edit;
        if (code == FormatCode.RESET) {
            if (Minecraft.getInstance().hasShiftDown()) {
                edit = FormatCodes.clear(value, cursor, cursor, S);
            } else if (cursor != select) {
                edit = FormatCodes.clear(value, select, cursor, S);
            } else {
                edit = FormatCodes.insertReset(value, cursor, S);
            }
        } else if (code.isColor()) {
            edit = FormatCodes.applyColor(value, select, cursor, code, S);
        } else {
            edit = FormatCodes.applyFormat(value, select, cursor, code, S);
        }
        screen.setFocused(pageBox());
        if (edit.value().equals(value)) {
            return;
        }
        if (!RichPages.fit(font()).fits(edit.value())) {
            notice.show(Component.translatable("qualityessentials.books.notice.no_room"), true);
            return;
        }
        setPage(edit);
        history.observe(state(), false);
    }

    private void setPage(FormatCodes.Edit edit) {
        MultilineTextField field = field();
        field.setValue(edit.value(), true);
        setCursor(edit.cursor(), edit.select());
    }

    private void setCursor(int cursor, int select) {
        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field();
        int length = field().value().length();
        access.qualityessentials$setCursor(Math.max(0, Math.min(cursor, length)));
        access.qualityessentials$setSelectCursor(Math.max(0, Math.min(select, length)));
    }

    private void goTo(int index) {
        int target = Math.max(0, Math.min(index, pages().size() - 1));
        access().qualityessentials$setCurrentPage(target);
        access().qualityessentials$updatePageContent();
        access().qualityessentials$updateButtonVisibility();
        screen.setFocused(pageBox());
    }

    private void insertPage() {
        if (pages().size() >= BookText.MAX_PAGES) {
            notice.show(Component.translatable("qualityessentials.books.notice.full", BookText.MAX_PAGES), true);
            return;
        }
        int current = access().qualityessentials$currentPage();
        pages().add(current + 1, "");
        goTo(current + 1);
        history.observe(state(), false);
    }

    private void deletePage() {
        int current = access().qualityessentials$currentPage();
        if (pages().size() <= 1) {
            pages().set(0, "");
            goTo(0);
        } else {
            pages().remove(current);
            goTo(Math.min(current, pages().size() - 1));
        }
        history.observe(state(), false);
        notice.show(Component.translatable("qualityessentials.books.notice.deleted"), false);
    }

    private void movePage(int direction) {
        int current = access().qualityessentials$currentPage();
        int target = current + direction;
        if (target < 0 || target >= pages().size()) {
            return;
        }
        List<String> pages = pages();
        String moved = pages.get(current);
        pages.set(current, pages.get(target));
        pages.set(target, moved);
        goTo(target);
        history.observe(state(), false);
    }

    private void pageBreak() {
        if (pages().size() >= BookText.MAX_PAGES) {
            notice.show(Component.translatable("qualityessentials.books.notice.full", BookText.MAX_PAGES), true);
            return;
        }
        String value = field().value();
        int cursor = FormatCodes.afterCode(value, field().cursor(), S);
        String carry = FormatCodes.active(value, cursor, S);
        if (carry.length() + value.length() - cursor > BookText.PAGE_LENGTH) {
            carry = "";
        }
        String after = carry + value.substring(cursor);
        int current = access().qualityessentials$currentPage();
        pages().set(current, value.substring(0, cursor));
        pages().add(current + 1, after);
        goTo(current + 1);
        setCursor(carry.length(), carry.length());
        history.observe(state(), false);
    }

    private void copyPage() {
        boolean plain = Minecraft.getInstance().hasShiftDown();
        String value = field().value();
        Minecraft.getInstance().keyboardHandler.setClipboard(plain ? FormatCodes.strip(value, S) : value);
        notice.show(Component.translatable(plain ? "qualityessentials.books.notice.copied_page_plain" : "qualityessentials.books.notice.copied_page"), false);
    }

    private void copyBook() {
        boolean plain = Minecraft.getInstance().hasShiftDown();
        Minecraft.getInstance().keyboardHandler.setClipboard(BookText.join(trimmed(pages()), !plain));
        notice.show(Component.translatable(plain ? "qualityessentials.books.notice.copied_book_plain" : "qualityessentials.books.notice.copied_book",
                trimmed(pages()).size()), false);
    }

    static List<String> trimmed(List<String> pages) {
        int end = pages.size();
        while (end > 1 && pages.get(end - 1).isEmpty()) {
            end--;
        }
        return List.copyOf(pages.subList(0, end));
    }

    private void paste(String raw) {
        String clip = BookText.clean(raw, QE.config().booksAmpersand);
        screen.setFocused(pageBox());
        if (clip.isEmpty()) {
            return;
        }
        MultilineTextField field = field();
        String value = field.value();
        int from = FormatCodes.afterCode(value, Math.min(field.cursor(), selectCursor()), S);
        int to = Math.max(from, FormatCodes.afterCode(value, Math.max(field.cursor(), selectCursor()), S));
        Paginator.Fit fit = RichPages.fit(font());
        List<String> chunks = BookText.chunks(clip);
        String head = value.substring(0, from);
        String tail = value.substring(to);
        if (chunks.size() == 1) {
            String combined = head + clip + tail;
            if (fit.fits(combined)) {
                int cursor = from + clip.length();
                setPage(new FormatCodes.Edit(combined, cursor, cursor));
                history.observe(state(), false);
                return;
            }
        }
        int cursorSource = chunks.size() == 1 ? head.length() + chunks.getFirst().length() : chunks.getLast().length();
        List<String> result = new ArrayList<>();
        int cursorPage = 0;
        int cursorLocal = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String text = (i == 0 ? head : "") + chunks.get(i) + (i == chunks.size() - 1 ? tail : "");
            List<Paginator.Page> split = Paginator.split(text, fit);
            if (i == chunks.size() - 1) {
                for (int j = 0; j < split.size(); j++) {
                    if (split.get(j).contains(cursorSource)) {
                        cursorPage = result.size() + j;
                        cursorLocal = split.get(j).local(cursorSource);
                        break;
                    }
                }
            }
            for (Paginator.Page page : split) {
                result.add(page.text());
            }
        }
        replaceCurrent(result, cursorPage, cursorLocal);
    }

    private void replaceCurrent(List<String> result, int cursorPage, int cursorLocal) {
        int room = BookText.MAX_PAGES - (pages().size() - 1);
        boolean cut = result.size() > room;
        if (cut) {
            result = new ArrayList<>(result.subList(0, room));
            if (cursorPage >= room) {
                cursorPage = room - 1;
                cursorLocal = result.get(cursorPage).length();
            }
        }
        int current = access().qualityessentials$currentPage();
        pages().remove(current);
        pages().addAll(current, result);
        goTo(current + cursorPage);
        setCursor(cursorLocal, cursorLocal);
        history.observe(state(), false);
        if (cut) {
            notice.show(Component.translatable("qualityessentials.books.notice.cut", BookText.MAX_PAGES), true);
        } else if (result.size() > 1) {
            notice.show(Component.translatable("qualityessentials.books.notice.spread", result.size()), false);
        }
    }

    private void undo() {
        State target = history.undo(state());
        if (target == null) {
            notice.show(Component.translatable("qualityessentials.books.notice.nothing_to_undo"), false);
            return;
        }
        restore(target);
    }

    private void redo() {
        State target = history.redo(state());
        if (target == null) {
            notice.show(Component.translatable("qualityessentials.books.notice.nothing_to_redo"), false);
            return;
        }
        restore(target);
    }

    private void restore(State state) {
        pages().clear();
        pages().addAll(state.pages());
        goTo(state.page());
        setCursor(state.cursor(), state.select());
    }

    private String baseName() {
        ItemStack book = access().qualityessentials$book();
        if (book != null && book.has(DataComponents.CUSTOM_NAME)) {
            return book.getHoverName().getString();
        }
        String first = FormatCodes.strip(pages().getFirst(), S).strip();
        int end = first.indexOf('\n');
        return end > 0 ? first.substring(0, end) : first;
    }

    private void save() {
        String content = BookText.join(trimmed(pages()), true);
        BookFiles.save(baseName(), content,
                file -> notice.show(Component.translatable("qualityessentials.books.notice.saved", file.getFileName().toString()), false),
                error -> notice.show(Component.translatable("qualityessentials.books.notice.save_failed"), true));
    }

    private void openImport() {
        beforeImport = state();
        Minecraft.getInstance().gui.setScreen(new BookImportScreen(screen, this));
    }

    void importText(String text, boolean replace) {
        if (beforeImport != null && !replace) {
            goTo(beforeImport.page());
            setCursor(beforeImport.cursor(), beforeImport.select());
        }
        beforeImport = null;
        if (!replace) {
            paste(text);
            return;
        }
        List<String> result = BookText.paginate(BookText.clean(text, QE.config().booksAmpersand), RichPages.fit(font()));
        boolean cut = result.size() > BookText.MAX_PAGES;
        if (cut) {
            result = new ArrayList<>(result.subList(0, BookText.MAX_PAGES));
        }
        pages().clear();
        pages().addAll(result.isEmpty() ? List.of("") : result);
        goTo(0);
        history.observe(state(), false);
        notice.show(cut ? Component.translatable("qualityessentials.books.notice.cut", BookText.MAX_PAGES)
                : Component.translatable("qualityessentials.books.notice.loaded", pages().size()), cut);
    }
}
