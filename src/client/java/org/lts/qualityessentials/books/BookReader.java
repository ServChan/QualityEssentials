package org.lts.qualityessentials.books;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.gui.Notice;
import org.lts.qualityessentials.gui.ToolPanel;
import org.lts.qualityessentials.mixin.BookViewScreenAccessor;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;

public final class BookReader {
    private final BookViewScreen screen;
    private final Notice notice = new Notice();
    private ToolPanel panel;

    private BookReader(BookViewScreen screen) {
        this.screen = screen;
    }

    public static void attach(BookViewScreen screen, int width) {
        new BookReader(screen).build(width);
    }

    private BookViewScreenAccessor access() {
        return (BookViewScreenAccessor) screen;
    }

    private void build(int width) {
        if (QE.config().booksReaderTools) {
            int x = (width - 192) / 2 + 170;
            int w = Math.max(64, Math.min(140, width - x - 2));
            x = Math.min(x, width - w - 2);
            List<AbstractWidget> widgets = Screens.getWidgets(screen);
            panel = new ToolPanel(x, 16, w, widgets::add);
            panel.section(Component.translatable("qualityessentials.books.section.copy"));
            panel.row(ToolPanel.Tool.of("qualityessentials.books.copy_page", this::copyPage), ToolPanel.Tool.of("qualityessentials.books.copy_book", this::copyBook));
            panel.section(Component.translatable("qualityessentials.books.section.file"));
            panel.row(ToolPanel.Tool.of("qualityessentials.books.save", this::save));
            panel.section(Component.translatable("qualityessentials.books.section.go"));
            panel.row(ToolPanel.Tool.of("qualityessentials.books.first", () -> jump(0)),
                    ToolPanel.Tool.of("qualityessentials.books.last", () -> jump(pages().size() - 1)));
            panel.finish();
            ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) -> panel.drawBackground(graphics));
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                panel.drawLabels(graphics, Minecraft.getInstance().font);
                notice.drawBelow(graphics, Minecraft.getInstance().font, s.width / 2, 196 + 24, 220, s.height);
            });
        }
        ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
            if (!QE.config().booksWheel || vertical == 0 || panel != null && panel.contains(mouseX, mouseY)) {
                return true;
            }
            if (vertical > 0) {
                access().qualityessentials$pageBack();
            } else {
                access().qualityessentials$pageForward();
            }
            return false;
        });
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(event));
    }

    private boolean keyPressed(KeyEvent event) {
        int key = event.shortcutKey();
        if (key == InputConstants.KEY_HOME) {
            jump(0);
            return true;
        }
        if (key == InputConstants.KEY_END) {
            jump(pages().size() - 1);
            return true;
        }
        if (event.isCopy()) {
            if (event.hasShiftDown()) {
                copyBook();
            } else {
                copyPage();
            }
            return true;
        }
        return false;
    }

    private void jump(int page) {
        if (page >= 0 && page != access().qualityessentials$currentPage()) {
            access().qualityessentials$forcePage(page);
        }
    }

    private List<String> pages() {
        List<String> pages = new ArrayList<>();
        for (Component page : access().qualityessentials$bookAccess().pages()) {
            pages.add(LegacyText.of(page));
        }
        return pages;
    }

    private boolean plain() {
        return Minecraft.getInstance().hasShiftDown();
    }

    private void copyPage() {
        List<String> pages = pages();
        int index = access().qualityessentials$currentPage();
        if (index < 0 || index >= pages.size()) {
            return;
        }
        boolean plain = plain();
        String page = pages.get(index);
        Minecraft.getInstance().keyboardHandler.setClipboard(plain ? FormatCodes.strip(page, FormatCode.SECTION) : page);
        notice.show(Component.translatable(plain ? "qualityessentials.books.notice.copied_page_plain" : "qualityessentials.books.notice.copied_page"), false);
    }

    private void copyBook() {
        List<String> pages = pages();
        boolean plain = plain();
        Minecraft.getInstance().keyboardHandler.setClipboard(BookText.join(pages, !plain));
        notice.show(Component.translatable(plain ? "qualityessentials.books.notice.copied_book_plain" : "qualityessentials.books.notice.copied_book",
                pages.size()), false);
    }

    private void save() {
        List<String> pages = pages();
        String first = pages.isEmpty() ? "" : FormatCodes.strip(pages.getFirst(), FormatCode.SECTION).strip();
        int end = first.indexOf('\n');
        BookFiles.save(end > 0 ? first.substring(0, end) : first, BookText.join(pages, true),
                file -> notice.show(Component.translatable("qualityessentials.books.notice.saved", file.getFileName().toString()), false),
                error -> notice.show(Component.translatable("qualityessentials.books.notice.save_failed"), true));
    }
}
