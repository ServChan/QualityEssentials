package org.lts.qualityessentials.keyboard.layout;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardLayout {
    public static final float KEYBOARD_WIDTH = 23f;
    public static final float KEYBOARD_HEIGHT = 6.5f;
    public static final float MOUSE_WIDTH = 3.6f;
    public static final float MOUSE_HEIGHT = 3.65f;

    private static final List<KeyCap> KEYBOARD = buildKeyboard();
    private static final List<KeyCap> MOUSE = buildMouse();
    private static final java.util.Map<Integer, String> RUSSIAN = buildRussian();

    private KeyboardLayout() {
    }

    public static List<KeyCap> keyboard() {
        return KEYBOARD;
    }

    public static List<KeyCap> mouse() {
        return MOUSE;
    }

    private static List<KeyCap> buildKeyboard() {
        List<KeyCap> keys = new ArrayList<>();
        Row row = new Row(keys, 0f);
        row.key("Esc", 41, 0f, 1f);
        String[] functionLabels = {"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};
        float[] functionX = {2f, 3f, 4f, 5f, 6.5f, 7.5f, 8.5f, 9.5f, 11f, 12f, 13f, 14f};
        for (int i = 0; i < functionLabels.length; i++) {
            row.key(functionLabels[i], 58 + i, functionX[i], 1f);
        }
        row.key("PrtSc", 70, 15.5f, 1f);
        row.key("ScrLk", 71, 16.5f, 1f);
        row.key("Pause", 72, 17.5f, 1f);

        row = new Row(keys, 1.5f);
        row.key("`", 53, 0f, 1f);
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (int i = 0; i < digits.length; i++) {
            row.key(digits[i], 30 + i, 1f + i, 1f);
        }
        row.key("-", 45, 11f, 1f);
        row.key("=", 46, 12f, 1f);
        row.key("Bksp", 42, 13f, 2f);
        row.key("Ins", 73, 15.5f, 1f);
        row.key("Home", 74, 16.5f, 1f);
        row.key("PgUp", 75, 17.5f, 1f);
        row.key("Num", 83, 19f, 1f);
        row.key("/", 84, 20f, 1f);
        row.key("*", 85, 21f, 1f);
        row.key("-", 86, 22f, 1f);

        row = new Row(keys, 2.5f);
        row.key("Tab", 43, 0f, 1.5f);
        letters(row, "QWERTYUIOP", 1.5f);
        row.key("[", 47, 11.5f, 1f);
        row.key("]", 48, 12.5f, 1f);
        row.key("\\", 49, 13.5f, 1.5f);
        row.key("Del", 76, 15.5f, 1f);
        row.key("End", 77, 16.5f, 1f);
        row.key("PgDn", 78, 17.5f, 1f);
        row.key("7", 95, 19f, 1f);
        row.key("8", 96, 20f, 1f);
        row.key("9", 97, 21f, 1f);
        keys.add(new KeyCap("+", false, 87, 22f, 2.5f, 1f, 2f));

        row = new Row(keys, 3.5f);
        row.key("Caps", 57, 0f, 1.75f);
        letters(row, "ASDFGHJKL", 1.75f);
        row.key(";", 51, 10.75f, 1f);
        row.key("'", 52, 11.75f, 1f);
        row.key("Enter", 40, 12.75f, 2.25f);
        row.key("4", 92, 19f, 1f);
        row.key("5", 93, 20f, 1f);
        row.key("6", 94, 21f, 1f);

        row = new Row(keys, 4.5f);
        row.key("Shift", 225, 0f, 2.25f);
        letters(row, "ZXCVBNM", 2.25f);
        row.key(",", 54, 9.25f, 1f);
        row.key(".", 55, 10.25f, 1f);
        row.key("/", 56, 11.25f, 1f);
        row.key("Shift", 229, 12.25f, 2.75f);
        row.key("Up", 82, 16.5f, 1f);
        row.key("1", 89, 19f, 1f);
        row.key("2", 90, 20f, 1f);
        row.key("3", 91, 21f, 1f);
        keys.add(new KeyCap("Ent", false, 88, 22f, 4.5f, 1f, 2f));

        row = new Row(keys, 5.5f);
        row.key("Ctrl", 224, 0f, 1.25f);
        row.key("Win", 227, 1.25f, 1.25f);
        row.key("Alt", 226, 2.5f, 1.25f);
        row.key("Space", 44, 3.75f, 6.25f);
        row.key("Alt", 230, 10f, 1.25f);
        row.key("Win", 231, 11.25f, 1.25f);
        row.key("Menu", 101, 12.5f, 1.25f);
        row.key("Ctrl", 228, 13.75f, 1.25f);
        row.key("Left", 80, 15.5f, 1f);
        row.key("Down", 81, 16.5f, 1f);
        row.key("Right", 79, 17.5f, 1f);
        row.key("0", 98, 19f, 2f);
        row.key(".", 99, 21f, 1f);
        return List.copyOf(keys);
    }

    private static java.util.Map<Integer, String> buildRussian() {
        String latin = "QWERTYUIOPASDFGHJKLZXCVBNM";
        String cyrillic = "ЙЦУКЕНГШЩЗФЫВАПРОЛДЯЧСМИТЬ";
        java.util.Map<Integer, String> map = new java.util.HashMap<>();
        for (int i = 0; i < latin.length(); i++) {
            map.put(4 + (latin.charAt(i) - 'A'), String.valueOf(cyrillic.charAt(i)));
        }
        map.put(47, "Х");
        map.put(48, "Ъ");
        map.put(51, "Ж");
        map.put(52, "Э");
        map.put(54, "Б");
        map.put(55, "Ю");
        map.put(53, "Ё");
        return java.util.Map.copyOf(map);
    }

    private static void letters(Row row, String letters, float startX) {
        for (int i = 0; i < letters.length(); i++) {
            char letter = letters.charAt(i);
            row.key(String.valueOf(letter), 4 + (letter - 'A'), startX + i, 1f);
        }
    }

    private static List<KeyCap> buildMouse() {
        List<KeyCap> keys = new ArrayList<>();
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.left", true, 1, 0f, 0f, 1.4f, 1.6f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.middle", true, 2, 1.4f, 0f, 0.8f, 1.6f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.right", true, 3, 2.2f, 0f, 1.4f, 1.6f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.4", true, 4, 0f, 1.85f, 1.8f, 0.8f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.5", true, 5, 1.8f, 1.85f, 1.8f, 0.8f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.6", true, 6, 0f, 2.85f, 1.2f, 0.8f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.7", true, 7, 1.2f, 2.85f, 1.2f, 0.8f));
        keys.add(new KeyCap("qualityessentials.keyboard.mouse.8", true, 8, 2.4f, 2.85f, 1.2f, 0.8f));
        return List.copyOf(keys);
    }

    private static final java.util.Map<String, String> COMPACT = java.util.Map.ofEntries(
            java.util.Map.entry("PrtSc", "PS"), java.util.Map.entry("ScrLk", "SL"), java.util.Map.entry("Pause", "Br"),
            java.util.Map.entry("Home", "Hm"), java.util.Map.entry("PgUp", "PU"), java.util.Map.entry("PgDn", "PD"),
            java.util.Map.entry("Menu", "Mn"), java.util.Map.entry("Left", "<"), java.util.Map.entry("Right", ">"),
            java.util.Map.entry("Up", "^"), java.util.Map.entry("Down", "v"), java.util.Map.entry("Num", "NL"),
            java.util.Map.entry("F10", "10"), java.util.Map.entry("F11", "11"), java.util.Map.entry("F12", "12"),
            java.util.Map.entry("Bksp", "BS"), java.util.Map.entry("Enter", "Ent"), java.util.Map.entry("Caps", "Cap"),
            java.util.Map.entry("Ins", "In"), java.util.Map.entry("Del", "Dl"), java.util.Map.entry("End", "En"));

    public static String compactLabel(KeyCap cap) {
        return cap.mouse() ? null : COMPACT.get(cap.label());
    }

    public static String russianLegend(KeyCap cap) {
        if (cap.mouse()) {
            return null;
        }
        return RUSSIAN.get(cap.code());
    }

    private record Row(List<KeyCap> keys, float y) {
        void key(String label, int code, float x, float width) {
            keys.add(new KeyCap(label, false, code, x, y, width, 1f));
        }
    }
}
