package org.lts.qualityessentials.books;

import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;

public final class Paginator {
    private Paginator() {
    }

    public interface Fit {
        boolean fits(String page);
    }

    public record Page(String text, int start, int end, int prefix) {
        public boolean contains(int index) {
            return index >= start && index <= end;
        }

        public int local(int index) {
            return prefix + Math.max(0, Math.min(index, end) - start);
        }
    }

    public static List<Page> split(String text, Fit fit) {
        List<Page> pages = new ArrayList<>();
        if (text.isEmpty() || fit.fits(text)) {
            pages.add(new Page(text, 0, text.length(), 0));
            return pages;
        }
        int pos = 0;
        while (pos < text.length()) {
            String carry = pages.isEmpty() ? "" : FormatCodes.active(text, pos, FormatCode.SECTION);
            if (!fit.fits(carry)) {
                carry = "";
            }
            int end = longestFit(text, pos, carry, fit);
            int next = end;
            if (end < text.length()) {
                int breakAt = lastBreak(text, pos, end);
                if (breakAt > pos) {
                    end = breakAt;
                    next = breakAt + 1;
                } else if (end > pos + 1 && FormatCodes.isCode(text, end - 1, FormatCode.SECTION)) {
                    end--;
                    next = end;
                }
            }
            pages.add(new Page(carry + text.substring(pos, end), pos, end, carry.length()));
            pos = next;
        }
        return pages;
    }

    private static int longestFit(String text, int pos, String carry, Fit fit) {
        int low = pos + 1;
        int high = text.length();
        if (fit.fits(carry + text.substring(pos, high))) {
            return high;
        }
        int best = pos + 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (fit.fits(carry + text.substring(pos, mid))) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        if (best < text.length() && Character.isLowSurrogate(text.charAt(best)) && best > pos + 1) {
            best--;
        }
        return best;
    }

    private static int lastBreak(String text, int pos, int end) {
        if (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\n')) {
            return end;
        }
        for (int i = end - 1; i > pos; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == ' ') {
                return i;
            }
        }
        return -1;
    }
}
