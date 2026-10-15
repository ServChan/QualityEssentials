package org.lts.qualityessentials.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum FormatCode {
    BLACK('0', 0x000000),
    DARK_BLUE('1', 0x0000AA),
    DARK_GREEN('2', 0x00AA00),
    DARK_AQUA('3', 0x00AAAA),
    DARK_RED('4', 0xAA0000),
    DARK_PURPLE('5', 0xAA00AA),
    GOLD('6', 0xFFAA00),
    GRAY('7', 0xAAAAAA),
    DARK_GRAY('8', 0x555555),
    BLUE('9', 0x5555FF),
    GREEN('a', 0x55FF55),
    AQUA('b', 0x55FFFF),
    RED('c', 0xFF5555),
    LIGHT_PURPLE('d', 0xFF55FF),
    YELLOW('e', 0xFFFF55),
    WHITE('f', 0xFFFFFF),
    OBFUSCATED('k', -1),
    BOLD('l', -1),
    STRIKETHROUGH('m', -1),
    UNDERLINE('n', -1),
    ITALIC('o', -1),
    RESET('r', -1);

    public static final char SECTION = '§';
    public static final char AMPERSAND = '&';

    private final char code;
    private final int rgb;

    FormatCode(char code, int rgb) {
        this.code = code;
        this.rgb = rgb;
    }

    public char code() {
        return code;
    }

    public int rgb() {
        return rgb;
    }

    public boolean isColor() {
        return rgb >= 0;
    }

    public boolean isFormat() {
        return rgb < 0 && this != RESET;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "qualityessentials.format." + id();
    }

    public String with(char prefix) {
        return new String(new char[]{prefix, code});
    }

    public static FormatCode byCode(char c) {
        char lower = Character.toLowerCase(c);
        for (FormatCode value : values()) {
            if (value.code == lower) {
                return value;
            }
        }
        return null;
    }

    public static List<FormatCode> colors() {
        List<FormatCode> list = new ArrayList<>();
        for (FormatCode value : values()) {
            if (value.isColor()) {
                list.add(value);
            }
        }
        return list;
    }

    public static List<FormatCode> styles() {
        return List.of(BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, OBFUSCATED, RESET);
    }

    public static FormatCode nearestColor(int rgb) {
        FormatCode best = BLACK;
        long bestDistance = Long.MAX_VALUE;
        for (FormatCode value : colors()) {
            int dr = ((value.rgb >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
            int dg = ((value.rgb >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
            int db = (value.rgb & 0xFF) - (rgb & 0xFF);
            long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = value;
            }
        }
        return best;
    }
}
