package org.lts.qualityessentials.text;

import java.util.EnumSet;
import java.util.Set;

public final class FormatCodes {
    private FormatCodes() {
    }

    public static boolean isCode(String text, int index, char prefix) {
        return index >= 0 && index + 1 < text.length() && text.charAt(index) == prefix && FormatCode.byCode(text.charAt(index + 1)) != null;
    }

    public static boolean hasCodes(String text, char prefix) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (isCode(text, i, prefix)) {
                return true;
            }
        }
        return false;
    }

    public static State stateAt(String text, int end, char prefix) {
        FormatCode color = null;
        Set<FormatCode> formats = EnumSet.noneOf(FormatCode.class);
        int limit = Math.min(end, text.length());
        for (int i = 0; i + 1 < limit; i++) {
            if (!isCode(text, i, prefix)) {
                continue;
            }
            FormatCode code = FormatCode.byCode(text.charAt(i + 1));
            if (code.isColor()) {
                color = code;
                formats.clear();
            } else if (code == FormatCode.RESET) {
                color = null;
                formats.clear();
            } else {
                formats.add(code);
            }
            i++;
        }
        return new State(color, formats);
    }

    public static String active(String text, int end, char prefix) {
        return stateAt(text, end, prefix).codes(prefix);
    }

    public static String strip(String text, char prefix) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (isCode(text, i, prefix)) {
                i++;
                continue;
            }
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    public static String convert(String text, char from, char to) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (isCode(text, i, from)) {
                out.append(to).append(Character.toLowerCase(text.charAt(i + 1)));
                i++;
                continue;
            }
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    public static int afterCode(String text, int cursor, char prefix) {
        return cursor > 0 && isCode(text, cursor - 1, prefix) ? cursor + 1 : cursor;
    }

    public static int stepLeft(String text, int cursor, char prefix) {
        int c = afterCode(text, Math.min(cursor, text.length()), prefix);
        while (c >= 2 && isCode(text, c - 2, prefix)) {
            c -= 2;
        }
        if (c <= 0) {
            return 0;
        }
        c--;
        if (c > 0 && Character.isLowSurrogate(text.charAt(c)) && Character.isHighSurrogate(text.charAt(c - 1))) {
            c--;
        }
        return c;
    }

    public static int stepRight(String text, int cursor, char prefix) {
        int c = afterCode(text, Math.max(0, cursor), prefix);
        while (isCode(text, c, prefix)) {
            c += 2;
        }
        if (c >= text.length()) {
            return text.length();
        }
        c++;
        if (c < text.length() && Character.isLowSurrogate(text.charAt(c)) && Character.isHighSurrogate(text.charAt(c - 1))) {
            c++;
        }
        return c;
    }

    public static Edit deleteBack(String text, int cursor, char prefix) {
        int c = afterCode(text, Math.min(cursor, text.length()), prefix);
        int p = c;
        while (p >= 2 && isCode(text, p - 2, prefix)) {
            p -= 2;
        }
        if (p <= 0) {
            if (c == 0) {
                return new Edit(text, 0, 0);
            }
            return new Edit(text.substring(c), 0, 0);
        }
        int from = p - 1;
        if (from > 0 && Character.isLowSurrogate(text.charAt(from)) && Character.isHighSurrogate(text.charAt(from - 1))) {
            from--;
        }
        String value = text.substring(0, from) + text.substring(p);
        return new Edit(value, from, from);
    }

    public static Edit deleteForward(String text, int cursor, char prefix) {
        int c = afterCode(text, Math.max(0, cursor), prefix);
        int p = c;
        while (isCode(text, p, prefix)) {
            p += 2;
        }
        if (p >= text.length()) {
            return new Edit(text.substring(0, c), c, c);
        }
        int to = p + 1;
        if (to < text.length() && Character.isLowSurrogate(text.charAt(to)) && Character.isHighSurrogate(text.charAt(p))) {
            to++;
        }
        return new Edit(text.substring(0, p) + text.substring(to), c, c);
    }

    public static Edit applyColor(String text, int start, int end, FormatCode color, char prefix) {
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        State before = stateAt(text, from, prefix);
        String opening = color.with(prefix) + before.formatCodes(prefix);
        if (from == to) {
            String value = text.substring(0, from) + opening + text.substring(from);
            int cursor = from + opening.length();
            return new Edit(value, cursor, cursor);
        }
        String closing = restore(stateAt(text, to, prefix), prefix);
        String value = text.substring(0, from) + opening + text.substring(from, to) + closing + text.substring(to);
        return new Edit(value, from + opening.length(), to + opening.length());
    }

    public static Edit applyFormat(String text, int start, int end, FormatCode format, char prefix) {
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        State before = stateAt(text, from, prefix);
        String opening = before.has(format)
                ? FormatCode.RESET.with(prefix) + before.without(format).codes(prefix)
                : format.with(prefix);
        if (from == to) {
            String value = text.substring(0, from) + opening + text.substring(from);
            int cursor = from + opening.length();
            return new Edit(value, cursor, cursor);
        }
        String closing = restore(stateAt(text, to, prefix), prefix);
        String value = text.substring(0, from) + opening + text.substring(from, to) + closing + text.substring(to);
        return new Edit(value, from + opening.length(), to + opening.length());
    }

    public static Edit clear(String text, int start, int end, char prefix) {
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        if (from == to) {
            if (!hasCodes(text, prefix)) {
                return new Edit(text, from, from);
            }
            String stripped = strip(text, prefix);
            int cursor = strip(text.substring(0, afterCode(text, from, prefix)), prefix).length();
            return new Edit(stripped, cursor, cursor);
        }
        while (from >= 2 && isCode(text, from - 2, prefix)) {
            from -= 2;
        }
        while (isCode(text, to, prefix)) {
            to += 2;
        }
        State before = stateAt(text, from, prefix);
        String opening = before.isPlain() ? "" : FormatCode.RESET.with(prefix);
        String middle = strip(text.substring(from, to), prefix);
        String closing = stateAt(text, to, prefix).codes(prefix);
        String value = text.substring(0, from) + opening + middle + closing + text.substring(to);
        int selectionStart = from + opening.length();
        return new Edit(value, selectionStart, selectionStart + middle.length());
    }

    public static Edit insertReset(String text, int cursor, char prefix) {
        String code = FormatCode.RESET.with(prefix);
        String value = text.substring(0, cursor) + code + text.substring(cursor);
        return new Edit(value, cursor + code.length(), cursor + code.length());
    }

    private static String restore(State state, char prefix) {
        return FormatCode.RESET.with(prefix) + state.codes(prefix);
    }

    public record Edit(String value, int cursor, int select) {
    }

    public record State(FormatCode color, Set<FormatCode> formats) {
        public State {
            formats = formats.isEmpty() ? EnumSet.noneOf(FormatCode.class) : EnumSet.copyOf(formats);
        }

        public boolean has(FormatCode code) {
            return code.isColor() ? code == color : formats.contains(code);
        }

        public boolean isPlain() {
            return color == null && formats.isEmpty();
        }

        public State without(FormatCode code) {
            Set<FormatCode> copy = EnumSet.noneOf(FormatCode.class);
            copy.addAll(formats);
            copy.remove(code);
            return new State(code == color ? null : color, copy);
        }

        public String codes(char prefix) {
            StringBuilder out = new StringBuilder();
            if (color != null) {
                out.append(color.with(prefix));
            }
            out.append(formatCodes(prefix));
            return out.toString();
        }

        public String formatCodes(char prefix) {
            StringBuilder out = new StringBuilder();
            for (FormatCode code : formats) {
                out.append(code.with(prefix));
            }
            return out.toString();
        }
    }
}
