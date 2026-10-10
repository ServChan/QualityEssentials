package org.lts.qualityessentials.hidden;

import java.util.ArrayList;
import java.util.List;

public final class DetailsText {
    private DetailsText() {
    }

    public static String shorten(String text, int max) {
        String flat = text.replace('\n', ' ').replace('\r', ' ');
        return flat.length() <= max ? flat : flat.substring(0, Math.max(0, max - 1)) + "…";
    }

    public static List<String> wrap(List<String> words, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String piece = line.isEmpty() ? word : ", " + word;
            if (!line.isEmpty() && line.length() + piece.length() > width) {
                lines.add(line.append(',').toString());
                line = new StringBuilder(word);
            } else {
                line.append(piece);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    public static String stripNamespace(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    public static int percent(int left, int max) {
        return max <= 0 ? 0 : Math.round(left * 100f / max);
    }
}
