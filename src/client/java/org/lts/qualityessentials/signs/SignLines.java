package org.lts.qualityessentials.signs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SignLines {
    public static final int LINES = 4;

    private SignLines() {
    }

    public static List<String> parts(String clipboard) {
        String text = clipboard.replace("\r\n", "\n").replace('\r', '\n');
        List<String> parts = new ArrayList<>();
        for (String part : text.split("\n", -1)) {
            StringBuilder clean = new StringBuilder(part.length());
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c == '\t') {
                    clean.append(' ');
                } else if (c >= ' ' && c != 127 && c != '§') {
                    clean.append(c);
                }
            }
            parts.add(clean.toString());
        }
        while (parts.size() > 1 && parts.getLast().isBlank()) {
            parts.removeLast();
        }
        return parts;
    }

    public static String fit(String line, Predicate<String> fits) {
        String value = line;
        while (!value.isEmpty() && !fits.test(value)) {
            int cut = value.length() - 1;
            if (cut > 0 && Character.isLowSurrogate(value.charAt(cut)) && Character.isHighSurrogate(value.charAt(cut - 1))) {
                cut--;
            }
            value = value.substring(0, cut);
        }
        return value;
    }

    public static String[] fitAll(List<String> lines, Predicate<String> fits) {
        String[] out = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            out[i] = i < lines.size() ? fit(lines.get(i), fits) : "";
        }
        return out;
    }

    public static String[] paste(String[] lines, int line, int cursor, int select, String clipboard, Predicate<String> fits) {
        List<String> parts = parts(clipboard);
        String[] out = lines.clone();
        String current = lines[line];
        int from = Math.max(0, Math.min(Math.min(cursor, select), current.length()));
        int to = Math.max(from, Math.min(Math.max(cursor, select), current.length()));
        String head = current.substring(0, from);
        String tail = current.substring(to);
        for (int i = 0; i < parts.size() && line + i < LINES; i++) {
            String text = parts.get(i);
            if (i == 0) {
                text = head + text;
            }
            if (i == parts.size() - 1 || line + i == LINES - 1) {
                text = text + tail;
            }
            out[line + i] = fit(text, fits);
        }
        return out;
    }

    public static String join(String[] lines) {
        int end = lines.length;
        while (end > 0 && lines[end - 1].isEmpty()) {
            end--;
        }
        return String.join("\n", java.util.Arrays.copyOf(lines, end));
    }

    public static boolean blank(String[] lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static int lastPastedLine(int line, String clipboard) {
        return Math.min(LINES - 1, line + parts(clipboard).size() - 1);
    }
}
