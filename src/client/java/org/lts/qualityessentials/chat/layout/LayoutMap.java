package org.lts.qualityessentials.chat.layout;

import java.util.HashMap;
import java.util.Map;

public final class LayoutMap {
    private static final String LATIN = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?@#$^&|";
    private static final String CYRILLIC = "ёйцукенгшщзхъфывапролджэячсмитьбю.ЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,\"№;:?/";
    private static final Map<Character, Character> TO_CYRILLIC = new HashMap<>();
    private static final Map<Character, Character> TO_LATIN = new HashMap<>();

    static {
        for (int i = 0; i < LATIN.length(); i++) {
            TO_CYRILLIC.put(LATIN.charAt(i), CYRILLIC.charAt(i));
            TO_LATIN.put(CYRILLIC.charAt(i), LATIN.charAt(i));
        }
    }

    private LayoutMap() {
    }

    public static String toCyrillic(String text) {
        return translate(text, TO_CYRILLIC);
    }

    public static String toLatin(String text) {
        return translate(text, TO_LATIN);
    }

    public static String translate(String text, boolean toCyrillic) {
        return toCyrillic ? toCyrillic(text) : toLatin(text);
    }

    private static String translate(String text, Map<Character, Character> map) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character mapped = map.get(c);
            out.append(mapped == null ? c : mapped);
        }
        return out.toString();
    }

    public static boolean isLatinLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static boolean isCyrillicLetter(char c) {
        return c >= 'а' && c <= 'я' || c >= 'А' && c <= 'Я' || c == 'ё' || c == 'Ё';
    }

    public static boolean typedOnLatin(char c) {
        return TO_CYRILLIC.containsKey(c);
    }

    public static boolean typedOnCyrillic(char c) {
        return TO_LATIN.containsKey(c);
    }
}
