package org.lts.qualityessentials.books;

import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BookText {
    public static final String PAGE_BREAK = "---";
    public static final int MAX_PAGES = 100;
    public static final int PAGE_LENGTH = 1024;
    public static final int BOOK_LENGTH = MAX_PAGES * PAGE_LENGTH;
    private static final Pattern BREAK_LINE = Pattern.compile("^[ \\t]*-{3,}[ \\t]*$");

    private BookText() {
    }

    public static String join(List<String> pages, boolean keepCodes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) {
                out.append('\n').append(PAGE_BREAK).append('\n');
            }
            String page = pages.get(i);
            out.append(keepCodes ? page : FormatCodes.strip(page, FormatCode.SECTION));
        }
        return out.toString();
    }

    public static List<String> chunks(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean started = false;
        for (String line : text.split("\n", -1)) {
            if (BREAK_LINE.matcher(line).matches()) {
                chunks.add(current.toString());
                current.setLength(0);
                started = false;
                continue;
            }
            if (started) {
                current.append('\n');
            }
            current.append(line);
            started = true;
        }
        chunks.add(current.toString());
        if (chunks.size() > 1 && chunks.getLast().isEmpty()) {
            chunks.removeLast();
        }
        return chunks;
    }

    public static String clean(String text, boolean ampersandCodes) {
        String value = text.startsWith("﻿") ? text.substring(1) : text;
        value = value.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == FormatCode.SECTION || c >= ' ' && c != 127) {
                out.append(c);
            } else if (c == '\t') {
                out.append("    ");
            }
        }
        String cleaned = out.length() > BOOK_LENGTH ? out.substring(0, BOOK_LENGTH) : out.toString();
        return ampersandCodes ? FormatCodes.convert(cleaned, FormatCode.AMPERSAND, FormatCode.SECTION) : cleaned;
    }

    public static List<String> paginate(String text, Paginator.Fit fit) {
        List<String> pages = new ArrayList<>();
        for (String chunk : chunks(text)) {
            for (Paginator.Page page : Paginator.split(chunk, fit)) {
                pages.add(page.text());
            }
        }
        return pages;
    }

    public static Paginator.Fit lengthFit(Paginator.Fit fit) {
        return page -> page.length() <= PAGE_LENGTH && fit.fits(page);
    }
}
