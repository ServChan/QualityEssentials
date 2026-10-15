package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.books.BookFiles;
import org.lts.qualityessentials.books.BookText;
import org.lts.qualityessentials.books.Paginator;
import org.lts.qualityessentials.core.EditHistory;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooksTest {
    private static final char S = FormatCode.SECTION;
    private static final Paginator.Fit TWENTY = page -> FormatCodes.strip(page, S).length() <= 20;

    private static String s(String text) {
        return text.replace('&', S);
    }

    @Test
    void stateFollowsLegacyRules() {
        FormatCodes.State state = FormatCodes.stateAt(s("&l&cred &obold"), 100, S);
        assertEquals(FormatCode.RED, state.color());
        assertTrue(state.has(FormatCode.ITALIC));
        assertFalse(state.has(FormatCode.BOLD));
        assertEquals(s("&c&o"), FormatCodes.active(s("&l&cred &obold"), 100, S));
        assertEquals("", FormatCodes.active(s("&cred&r plain"), 100, S));
        assertEquals(s("&c"), FormatCodes.active(s("&cred"), 2, S));
        assertEquals("", FormatCodes.active(s("&cred"), 1, S));
    }

    @Test
    void stripAndConvertOnlyTouchRealCodes() {
        assertEquals("Tom & Jerry", FormatCodes.convert("Tom & Jerry", '&', S));
        assertEquals(s("R&bock"), FormatCodes.convert("R&Bock", '&', S));
        assertEquals(s("&bblue") + " & more &z", FormatCodes.convert("&Bblue & more &z", '&', S));
        assertEquals("hello world", FormatCodes.strip(s("&chello &lworld"), S));
        assertFalse(FormatCodes.hasCodes("100% &", S));
    }

    @Test
    void colorWrapsSelectionAndRestoresTheRest() {
        String text = s("&lHello world");
        FormatCodes.Edit edit = FormatCodes.applyColor(text, 8, 13, FormatCode.RED, S);
        assertEquals(s("&lHello &c&lworld&r&l"), edit.value());
        assertEquals("world", edit.value().substring(edit.cursor(), edit.select()));
        FormatCodes.Edit typed = FormatCodes.applyColor("abc", 1, 1, FormatCode.GOLD, S);
        assertEquals(s("a&6bc"), typed.value());
        assertEquals(3, typed.cursor());
    }

    @Test
    void formatTogglesOffWhenAlreadyActive() {
        FormatCodes.Edit on = FormatCodes.applyFormat("abc", 3, 3, FormatCode.BOLD, S);
        assertEquals(s("abc&l"), on.value());
        FormatCodes.Edit off = FormatCodes.applyFormat(s("&c&labc"), 7, 7, FormatCode.BOLD, S);
        assertEquals(s("&c&labc&r&c"), off.value());
        FormatCodes.Edit wrap = FormatCodes.applyFormat(s("one two"), 4, 7, FormatCode.ITALIC, S);
        assertEquals(s("one &otwo&r"), wrap.value());
    }

    @Test
    void clearRemovesCodesInSelectionOrWholePage() {
        FormatCodes.Edit selection = FormatCodes.clear(s("&cred &lbold&r end"), 0, 12, S);
        assertEquals(s("red bold end"), selection.value());
        FormatCodes.Edit middle = FormatCodes.clear(s("&cred &lbold tail"), 6, 12, S);
        assertEquals(s("&cred &rbold&c&l tail"), middle.value());
        FormatCodes.Edit all = FormatCodes.clear(s("&ca&lb c"), 6, 6, S);
        assertEquals("ab c", all.value());
        assertEquals(2, all.cursor());
    }

    @Test
    void cursorStepsAndDeletesSkipInvisibleCodes() {
        String text = s("ab&cde");
        assertEquals(1, FormatCodes.stepLeft(text, 4, S));
        assertEquals(5, FormatCodes.stepRight(text, 2, S));
        assertEquals(5, FormatCodes.stepRight(text, 3, S));
        FormatCodes.Edit back = FormatCodes.deleteBack(text, 4, S);
        assertEquals(s("a&cde"), back.value());
        assertEquals(1, back.cursor());
        FormatCodes.Edit forward = FormatCodes.deleteForward(text, 2, S);
        assertEquals(s("ab&ce"), forward.value());
        assertEquals(s("rest"), FormatCodes.deleteBack(s("&c&lrest"), 4, S).value());
        assertEquals("ab", FormatCodes.deleteForward(s("ab&c"), 2, S).value());
    }

    @Test
    void paginatorBreaksAtSpacesAndCarriesFormatting() {
        String text = s("&cone two three four five six seven eight nine ten");
        List<Paginator.Page> pages = Paginator.split(text, TWENTY);
        assertTrue(pages.size() > 1);
        for (Paginator.Page page : pages) {
            assertTrue(TWENTY.fits(page.text()), page.text());
            assertFalse(page.text().startsWith(" "), page.text());
        }
        assertTrue(pages.get(1).text().startsWith(s("&c")));
        StringBuilder joined = new StringBuilder();
        for (Paginator.Page page : pages) {
            if (!joined.isEmpty()) {
                joined.append(' ');
            }
            joined.append(FormatCodes.strip(page.text(), S));
        }
        assertEquals(FormatCodes.strip(text, S), joined.toString());
        int index = text.indexOf("ten");
        Paginator.Page last = pages.getLast();
        assertTrue(last.contains(index));
        assertEquals("ten", last.text().substring(last.local(index), last.local(index) + 3));
    }

    @Test
    void paginatorCutsLongWordsAndKeepsShortTextWhole() {
        List<Paginator.Page> pages = Paginator.split("x".repeat(45), TWENTY);
        assertEquals(List.of(20, 20, 5), pages.stream().map(page -> page.text().length()).toList());
        assertEquals(1, Paginator.split("short", TWENTY).size());
        assertEquals(1, Paginator.split("", TWENTY).size());
    }

    @Test
    void pageBreakLinesSplitChunksAndRoundTrip() {
        List<String> pages = List.of(s("&cfirst\nline"), "second", "", "fourth\n");
        String joined = BookText.join(pages, true);
        assertEquals(pages, BookText.chunks(joined));
        assertEquals(List.of("a", "b"), BookText.chunks("a\n---\nb\n-----\n"));
        assertEquals(List.of("plain"), BookText.chunks("plain"));
        assertEquals("first\nline\n---\nsecond", BookText.join(List.of(s("&cfirst\nline"), "second"), false));
    }

    @Test
    void cleanDropsControlCharactersAndConvertsAmpersands() {
        assertEquals("a\nb    c", BookText.clean("﻿a\r\nb\tc\u0007", false));
        assertEquals(s("&aGreen"), BookText.clean("&aGreen", true));
        assertEquals("&aGreen", BookText.clean("&aGreen", false));
    }

    @Test
    void paginateHonoursBreaksAndPageLength() {
        List<String> pages = BookText.paginate("one\n---\n" + "y".repeat(30), TWENTY);
        assertEquals(List.of("one", "y".repeat(20), "y".repeat(10)), pages);
        Paginator.Fit limited = BookText.lengthFit(page -> true);
        assertTrue(limited.fits("z".repeat(BookText.PAGE_LENGTH)));
        assertFalse(limited.fits("z".repeat(BookText.PAGE_LENGTH + 1)));
    }

    @Test
    void fileNamesAreSafe() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 11, 9, 5, 3);
        assertEquals("My_book_2026-10-11_09-05-03.txt", BookFiles.fileName("My book!", time));
        assertEquals("book_2026-10-11_09-05-03.txt", BookFiles.fileName("../..", time));
        assertEquals("Книга_2026-10-11_09-05-03.txt", BookFiles.fileName("Книга", time));
    }

    @Test
    void historyMergesTypingAndUndoesStructuralSteps() {
        AtomicLong clock = new AtomicLong();
        EditHistory<String> history = new EditHistory<>(10, 1000L, clock::get, String::equals);
        history.observe("", false);
        history.observe("a", true);
        clock.addAndGet(100);
        history.observe("ab", true);
        clock.addAndGet(100);
        history.observe("ab|page", false);
        assertEquals("ab", history.undo("ab|page"));
        assertEquals("", history.undo("ab"));
        assertNull(history.undo(""));
        assertEquals("ab", history.redo(""));
        history.observe("abc", true);
        assertNull(history.redo("abc"));
        assertTrue(history.canUndo("abc"));
    }

    @Test
    void historyIsBounded() {
        EditHistory<Integer> history = new EditHistory<>(3, 0L, () -> 0L, Integer::equals);
        for (int i = 0; i < 10; i++) {
            history.observe(i, false);
        }
        assertEquals(8, history.undo(9));
        assertEquals(7, history.undo(8));
        assertEquals(6, history.undo(7));
        assertNull(history.undo(6));
    }
}
