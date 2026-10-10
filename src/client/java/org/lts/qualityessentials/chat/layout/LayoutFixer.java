package org.lts.qualityessentials.chat.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class LayoutFixer {
    static final double LONG_MARGIN = 1.0;
    static final double REVERSE_MARGIN = 1.5;
    static final double REVERSE_KNOWN_MARGIN = 0.5;
    static final double WEAK_MARGIN = 0.0;
    private static final double IMPOSSIBLE = -20.0;
    private static final Set<Character> SINGLE_RUSSIAN = Set.of('а', 'в', 'и', 'к', 'о', 'с', 'у', 'я');
    private static final Set<String> TARGETED_COMMANDS = Set.of("msg", "tell", "w", "whisper", "m", "pm", "dm", "t");
    private static final Set<String> MESSAGE_COMMANDS = Set.of("r", "reply", "me", "say", "teammsg", "tm");
    private static final Pattern DOMAIN = Pattern.compile(
            "(?i)[a-z0-9-]+(\\.[a-z0-9-]+)*\\.(com|net|org|ru|su|io|gg|me|xyz|pro|fun|online|club|top|dev|uk|de|eu|tv|info|space|site|ua|by|kz)");

    private final LanguageModel russian;
    private final LanguageModel english;

    public LayoutFixer(LanguageModel russian, LanguageModel english) {
        this.russian = russian;
        this.english = english;
    }

    public String fixInput(String input, Predicate<String> knownCommand, Predicate<String> playerName, Predicate<String> ignored,
            boolean words, boolean commands) {
        if (input.isEmpty()) {
            return input;
        }
        if (input.startsWith("/")) {
            return "/" + fixCommand(input.substring(1), knownCommand, playerName, ignored, words, commands);
        }
        if (commands && input.startsWith(".")) {
            String body = input.substring(1);
            String latin = latinCommand(firstToken(body), knownCommand);
            if (latin != null) {
                return "/" + fixCommand(body, knownCommand, playerName, ignored, words, true);
            }
        }
        return words ? fixText(input, playerName.or(ignored)) : input;
    }

    public String fixText(String text, Predicate<String> protectedWord) {
        List<String> parts = split(text);
        List<Verdict> verdicts = new ArrayList<>();
        boolean anyToCyrillic = false;
        boolean anyToLatin = false;
        for (String part : parts) {
            Verdict verdict = isSpace(part) ? Verdict.SPACE : judge(part, protectedWord);
            verdicts.add(verdict);
            anyToCyrillic |= verdict == Verdict.TO_CYRILLIC;
            anyToLatin |= verdict == Verdict.TO_LATIN;
        }
        if (!anyToCyrillic && !anyToLatin) {
            return text;
        }
        boolean allWordsMoved = true;
        for (int i = 0; i < parts.size(); i++) {
            Verdict verdict = verdicts.get(i);
            if (verdict == Verdict.WEAK_TO_CYRILLIC && anyToCyrillic) {
                verdicts.set(i, Verdict.TO_CYRILLIC);
            } else if (verdict == Verdict.WEAK_TO_LATIN && anyToLatin) {
                verdicts.set(i, Verdict.TO_LATIN);
            }
            verdict = verdicts.get(i);
            if (verdict != Verdict.SPACE && verdict != Verdict.PUNCTUATION && verdict != Verdict.TO_CYRILLIC && verdict != Verdict.TO_LATIN) {
                allWordsMoved = false;
            }
        }
        boolean punctuationToCyrillic = allWordsMoved && anyToCyrillic && !anyToLatin;
        boolean punctuationToLatin = allWordsMoved && anyToLatin && !anyToCyrillic;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            out.append(switch (verdicts.get(i)) {
                case TO_CYRILLIC -> LayoutMap.toCyrillic(part);
                case TO_LATIN -> LayoutMap.toLatin(part);
                case PUNCTUATION -> punctuationToCyrillic ? LayoutMap.toCyrillic(part) : punctuationToLatin ? LayoutMap.toLatin(part) : part;
                default -> part;
            });
        }
        return out.toString();
    }

    public String flip(String text, Predicate<String> playerName) {
        String prefix = "";
        String body = text;
        if (text.startsWith("/")) {
            int space = text.indexOf(' ');
            String root = space < 0 ? text.substring(1) : text.substring(1, space);
            String rest = space < 0 ? "" : text.substring(space);
            if (hasCyrillic(root)) {
                root = LayoutMap.toLatin(root);
            }
            prefix = "/" + root;
            body = rest;
            if (TARGETED_COMMANDS.contains(commandName(root))) {
                List<String> parts = split(body);
                StringBuilder head = new StringBuilder();
                int index = 0;
                while (index < parts.size() && isSpace(parts.get(index))) {
                    head.append(parts.get(index++));
                }
                if (index < parts.size()) {
                    String target = parts.get(index++);
                    head.append(hasCyrillic(target) ? LayoutMap.toLatin(target) : target);
                }
                prefix += head;
                body = String.join("", parts.subList(index, parts.size()));
            }
        }
        List<String> parts = split(body);
        int latin = 0;
        int cyrillic = 0;
        for (String part : parts) {
            if (!isSpace(part) && !playerName.test(part)) {
                latin += countLatin(part);
                cyrillic += countCyrillic(part);
            }
        }
        if (latin == 0 && cyrillic == 0) {
            return prefix + body;
        }
        boolean toCyrillic = latin >= cyrillic;
        StringBuilder out = new StringBuilder(prefix);
        for (String part : parts) {
            boolean source = toCyrillic ? countLatin(part) > 0 && countCyrillic(part) == 0 : countCyrillic(part) > 0 && countLatin(part) == 0;
            boolean punctuation = countLatin(part) == 0 && countCyrillic(part) == 0;
            if (!isSpace(part) && !playerName.test(part) && (source || punctuation)) {
                out.append(LayoutMap.translate(part, toCyrillic));
            } else {
                out.append(part);
            }
        }
        return out.toString();
    }

    public static String flipSelection(String text) {
        return LayoutMap.translate(text, countLatin(text) >= countCyrillic(text));
    }

    private String fixCommand(String body, Predicate<String> knownCommand, Predicate<String> playerName, Predicate<String> ignored,
            boolean words, boolean commands) {
        String root = firstToken(body);
        if (root.isEmpty()) {
            return body;
        }
        String rest = body.substring(root.length());
        if (commands && !knownCommand.test(root) && hasCyrillic(root)) {
            String latin = latinCommand(root, knownCommand);
            if (latin != null) {
                root = latin;
            }
        }
        if (!words || rest.isEmpty()) {
            return root + rest;
        }
        String name = commandName(root);
        if (MESSAGE_COMMANDS.contains(name)) {
            return root + fixText(rest, playerName.or(ignored));
        }
        if (TARGETED_COMMANDS.contains(name)) {
            List<String> parts = split(rest);
            StringBuilder out = new StringBuilder(root);
            int index = 0;
            while (index < parts.size() && isSpace(parts.get(index))) {
                out.append(parts.get(index++));
            }
            if (index >= parts.size()) {
                return out.toString();
            }
            String target = parts.get(index++);
            if (!playerName.test(target) && hasCyrillic(target) && playerName.test(LayoutMap.toLatin(target))) {
                target = LayoutMap.toLatin(target);
            }
            out.append(target);
            out.append(fixText(String.join("", parts.subList(index, parts.size())), playerName.or(ignored)));
            return out.toString();
        }
        return root + rest;
    }

    private static String latinCommand(String root, Predicate<String> knownCommand) {
        if (root.isEmpty() || countCyrillic(root) == 0 || countLatin(root) > 0) {
            return null;
        }
        String latin = LayoutMap.toLatin(root);
        if (knownCommand.test(latin)) {
            return latin;
        }
        String lower = latin.toLowerCase(Locale.ROOT);
        return knownCommand.test(lower) ? lower : null;
    }

    private static String commandName(String root) {
        int colon = root.indexOf(':');
        return (colon >= 0 ? root.substring(colon + 1) : root).toLowerCase(Locale.ROOT);
    }

    Verdict judge(String token, Predicate<String> protectedWord) {
        int latin = countLatin(token);
        int cyrillic = countCyrillic(token);
        if (latin == 0 && cyrillic == 0) {
            return hasMappable(token) ? Verdict.PUNCTUATION : Verdict.KEEP;
        }
        if (latin > 0 && cyrillic > 0 || untouchable(token) || protectedWord.test(token)) {
            return Verdict.KEEP;
        }
        if (latin > 0) {
            String englishCore = core(token, true);
            if (protectedWord.test(englishCore)) {
                return Verdict.KEEP;
            }
            boolean glued = ",.;".indexOf(token.charAt(0)) >= 0;
            return judgeLatin(glued ? "" : englishCore, core(LayoutMap.toCyrillic(token), false));
        }
        if (!LayoutMap.isCyrillicLetter(token.charAt(0)) || !LayoutMap.isLatinLetter(LayoutMap.toLatin(token.substring(0, 1)).charAt(0))) {
            return Verdict.KEEP;
        }
        return judgeCyrillic(core(token, false), core(LayoutMap.toLatin(token), true));
    }

    private Verdict judgeLatin(String englishCore, String russianCore) {
        String englishWord = lettersOnly(englishCore, true);
        String russianWord = lettersOnly(russianCore, false);
        if (russianWord == null) {
            return Verdict.KEEP;
        }
        if (englishWord != null && english.knows(englishWord)) {
            return Verdict.KEEP;
        }
        if (russianWord.length() == 1) {
            return SINGLE_RUSSIAN.contains(Character.toLowerCase(russianWord.charAt(0))) ? Verdict.WEAK_TO_CYRILLIC : Verdict.KEEP;
        }
        if (russian.knows(russianWord)) {
            return Verdict.TO_CYRILLIC;
        }
        double margin = russian.score(russianWord) - (englishWord == null ? IMPOSSIBLE : english.score(englishWord));
        if (russianWord.length() >= 4 && margin >= LONG_MARGIN) {
            return Verdict.TO_CYRILLIC;
        }
        return margin >= WEAK_MARGIN ? Verdict.WEAK_TO_CYRILLIC : Verdict.KEEP;
    }

    private Verdict judgeCyrillic(String russianCore, String englishCore) {
        String russianWord = lettersOnly(russianCore, false);
        String englishWord = lettersOnly(englishCore, true);
        if (englishWord == null || englishWord.length() < 2) {
            return Verdict.KEEP;
        }
        if (russianWord != null && russian.knows(russianWord)) {
            return Verdict.KEEP;
        }
        double margin = english.score(englishWord) - (russianWord == null ? IMPOSSIBLE : russian.score(russianWord));
        if (english.knows(englishWord) && margin >= REVERSE_KNOWN_MARGIN) {
            return Verdict.TO_LATIN;
        }
        if (englishWord.length() >= 4 && margin >= REVERSE_MARGIN) {
            return Verdict.TO_LATIN;
        }
        return margin >= REVERSE_KNOWN_MARGIN ? Verdict.WEAK_TO_LATIN : Verdict.KEEP;
    }

    private static boolean untouchable(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c) || c == '_' || c == '§' || c == '@' && i == 0) {
                return true;
            }
        }
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.contains("://") || lower.startsWith("www.") || DOMAIN.matcher(lower).matches()) {
            return true;
        }
        if (token.length() >= 2 && token.charAt(0) == '&' && "0123456789abcdefklmnor".indexOf(Character.toLowerCase(token.charAt(1))) >= 0) {
            return true;
        }
        for (int i = 1; i < token.length(); i++) {
            if (Character.isUpperCase(token.charAt(i)) && !isAllUpper(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllUpper(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (Character.isLowerCase(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String core(String token, boolean latin) {
        int start = 0;
        int end = token.length();
        while (start < end && !isLetter(token.charAt(start), latin)) {
            start++;
        }
        while (end > start && !isLetter(token.charAt(end - 1), latin)) {
            end--;
        }
        return token.substring(start, end);
    }

    private static String lettersOnly(String core, boolean latin) {
        if (core.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder(core.length());
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (isLetter(c, latin)) {
                out.append(Character.toLowerCase(c));
            } else if (c != '-' && !(latin && c == '\'')) {
                return null;
            }
        }
        return out.toString();
    }

    private static boolean isLetter(char c, boolean latin) {
        return latin ? LayoutMap.isLatinLetter(c) : LayoutMap.isCyrillicLetter(c);
    }

    private static boolean hasMappable(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (LayoutMap.typedOnLatin(token.charAt(i)) || LayoutMap.typedOnCyrillic(token.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCyrillic(String text) {
        return countCyrillic(text) > 0;
    }

    private static int countLatin(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (LayoutMap.isLatinLetter(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static int countCyrillic(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (LayoutMap.isCyrillicLetter(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static String firstToken(String text) {
        int space = 0;
        while (space < text.length() && !Character.isWhitespace(text.charAt(space))) {
            space++;
        }
        return text.substring(0, space);
    }

    private static boolean isSpace(String part) {
        return !part.isEmpty() && Character.isWhitespace(part.charAt(0));
    }

    static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (i == text.length() || Character.isWhitespace(text.charAt(i)) != Character.isWhitespace(text.charAt(start))) {
                if (i > start) {
                    parts.add(text.substring(start, i));
                }
                start = i;
            }
        }
        return parts;
    }

    enum Verdict {
        SPACE,
        KEEP,
        PUNCTUATION,
        TO_CYRILLIC,
        TO_LATIN,
        WEAK_TO_CYRILLIC,
        WEAK_TO_LATIN
    }
}
