package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.signs.SignLines;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignLinesTest {
    private static final Predicate<String> TEN = line -> line.length() <= 10;

    @Test
    void partsCleanTheClipboard() {
        assertEquals(List.of("one", "two x", "three"), SignLines.parts("one\r\ntwo\tx§\nthree\n\n"));
        assertEquals(List.of(""), SignLines.parts(""));
    }

    @Test
    void fitTrimsFromTheEnd() {
        assertEquals("0123456789", SignLines.fit("0123456789abc", TEN));
        assertEquals("short", SignLines.fit("short", TEN));
        assertArrayEquals(new String[]{"a", "0123456789", "", ""}, SignLines.fitAll(List.of("a", "0123456789xyz"), TEN));
    }

    @Test
    void multiLinePasteFillsFollowingLines() {
        String[] lines = {"Hello", "old", "keep", "last"};
        String[] pasted = SignLines.paste(lines, 0, 2, 5, "1\n2\n3", TEN);
        assertArrayEquals(new String[]{"He1", "2", "3", "last"}, pasted);
        String[] tail = SignLines.paste(new String[]{"a", "bXc", "", ""}, 1, 1, 2, "one\ntwo\nthree\nfour", TEN);
        assertArrayEquals(new String[]{"a", "bone", "two", "threec"}, tail);
        assertEquals(3, SignLines.lastPastedLine(1, "one\ntwo\nthree\nfour"));
    }

    @Test
    void joinAndBlank() {
        assertEquals("a\n\nb", SignLines.join(new String[]{"a", "", "b", ""}));
        assertTrue(SignLines.blank(new String[]{"", " ", null, ""}));
        assertFalse(SignLines.blank(new String[]{"", "x", "", ""}));
    }
}
