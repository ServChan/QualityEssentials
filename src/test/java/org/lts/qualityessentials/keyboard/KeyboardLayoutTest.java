package org.lts.qualityessentials.keyboard;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.keyboard.layout.KeyCap;
import org.lts.qualityessentials.keyboard.layout.KeyboardLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardLayoutTest {
    @Test
    void keysDoNotOverlap() {
        List<KeyCap> keys = KeyboardLayout.keyboard();
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                assertFalse(keys.get(i).overlaps(keys.get(j)), keys.get(i) + " overlaps " + keys.get(j));
            }
        }
    }

    @Test
    void keysStayInsideTheBoard() {
        for (KeyCap key : KeyboardLayout.keyboard()) {
            assertTrue(key.x() >= 0 && key.x() + key.width() <= KeyboardLayout.KEYBOARD_WIDTH, key.toString());
            assertTrue(key.y() >= 0 && key.y() + key.height() <= KeyboardLayout.KEYBOARD_HEIGHT, key.toString());
        }
        for (KeyCap key : KeyboardLayout.mouse()) {
            assertTrue(key.x() + key.width() <= KeyboardLayout.MOUSE_WIDTH + 0.001f && key.y() + key.height() <= KeyboardLayout.MOUSE_HEIGHT + 0.001f, key.toString());
        }
    }

    @Test
    void everyScancodeAppearsOnce() {
        Set<Integer> codes = new HashSet<>();
        for (KeyCap key : KeyboardLayout.keyboard()) {
            assertTrue(codes.add(key.code()), "duplicate code " + key.code());
        }
        assertEquals(104, KeyboardLayout.keyboard().size());
    }

    @Test
    void coversLettersDigitsFunctionKeysNumpadAndModifiers() {
        Set<Integer> codes = new HashSet<>();
        for (KeyCap key : KeyboardLayout.keyboard()) {
            codes.add(key.code());
        }
        for (int code = 4; code <= 29; code++) {
            assertTrue(codes.contains(code), "letter " + code);
        }
        for (int code = 30; code <= 39; code++) {
            assertTrue(codes.contains(code), "digit " + code);
        }
        for (int code = 58; code <= 69; code++) {
            assertTrue(codes.contains(code), "F-key " + code);
        }
        for (int code = 83; code <= 99; code++) {
            assertTrue(codes.contains(code), "numpad " + code);
        }
        for (int code = 224; code <= 231; code++) {
            assertTrue(codes.contains(code), "modifier " + code);
        }
    }

    @Test
    void mouseButtonsDoNotOverlap() {
        List<KeyCap> keys = KeyboardLayout.mouse();
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                assertFalse(keys.get(i).overlaps(keys.get(j)), keys.get(i) + " overlaps " + keys.get(j));
            }
        }
    }

    @Test
    void russianLegendsFollowJcukenLayout() {
        String latin = "QWERTYUIOPASDFGHJKLZXCVBNM";
        String cyrillic = "ЙЦУКЕНГШЩЗФЫВАПРОЛДЯЧСМИТЬ";
        for (KeyCap key : KeyboardLayout.keyboard()) {
            int letter = latin.indexOf(key.label());
            if (key.label().length() == 1 && letter >= 0) {
                assertEquals(String.valueOf(cyrillic.charAt(letter)), KeyboardLayout.russianLegend(key), key.label());
            }
            if (key.code() == 47) {
                assertEquals("Х", KeyboardLayout.russianLegend(key));
            }
        }
        assertEquals(null, KeyboardLayout.russianLegend(KeyboardLayout.mouse().getFirst()));
    }

    @Test
    void mouseHasEightButtons() {
        Set<Integer> codes = new HashSet<>();
        for (KeyCap key : KeyboardLayout.mouse()) {
            assertTrue(key.mouse());
            codes.add(key.code());
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8), codes);
    }
}
