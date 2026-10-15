package org.lts.qualityessentials.books;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public final class RichPages {
    public static final int TEXT_WIDTH = 114;
    public static final int LINE_LIMIT = 14;

    private RichPages() {
    }

    public static List<int[]> lines(Font font, String value, int width) {
        List<int[]> lines = new ArrayList<>();
        if (value.isEmpty()) {
            lines.add(new int[]{0, 0});
            return lines;
        }
        font.getSplitter().splitLines(value, width, Style.EMPTY, false, (style, start, end) -> lines.add(new int[]{start, end}));
        if (value.charAt(value.length() - 1) == '\n') {
            lines.add(new int[]{value.length(), value.length()});
        }
        return lines;
    }

    public static int lineOf(List<int[]> lines, int cursor) {
        for (int i = 0; i < lines.size(); i++) {
            if (cursor >= lines.get(i)[0] && cursor <= lines.get(i)[1]) {
                return i;
            }
        }
        return lines.size() - 1;
    }

    public static int lineCount(Font font, String page) {
        return font.getSplitter().splitLines(page, TEXT_WIDTH, Style.EMPTY).size() + (page.endsWith("\n") ? 1 : 0);
    }

    public static Paginator.Fit fit(Font font) {
        return BookText.lengthFit(page -> lineCount(font, page) <= LINE_LIMIT);
    }
}
