package org.lts.qualityessentials.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MessageSplitter {
    public static final int LIMIT = 256;
    public static final int INPUT_LIMIT = 1280;
    private static final Set<String> TARGETED = Set.of("msg", "tell", "w", "whisper", "m", "pm", "dm", "t");
    private static final Set<String> FREE = Set.of("r", "reply", "me", "say", "teammsg", "tm");

    private MessageSplitter() {
    }

    public static List<String> split(String message, int limit) {
        if (message.length() <= limit) {
            return List.of(message);
        }
        String prefix = prefix(message);
        if (prefix == null) {
            return List.of();
        }
        String body = message.substring(prefix.length()).strip();
        int room = limit - prefix.length();
        if (room < 16) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        while (!body.isEmpty()) {
            if (body.length() <= room) {
                parts.add(prefix + body);
                break;
            }
            int cut = body.lastIndexOf(' ', room);
            if (cut < room / 2) {
                cut = room;
            }
            parts.add(prefix + body.substring(0, cut).strip());
            body = body.substring(cut).strip();
        }
        return parts;
    }

    public static boolean splittable(String message) {
        return prefix(message) != null;
    }

    static String prefix(String message) {
        if (!message.startsWith("/")) {
            return "";
        }
        String[] words = message.substring(1).split(" ", 3);
        String root = words[0];
        int colon = root.indexOf(':');
        String name = (colon >= 0 ? root.substring(colon + 1) : root).toLowerCase(Locale.ROOT);
        if (FREE.contains(name) && words.length >= 2) {
            return "/" + root + " ";
        }
        if (TARGETED.contains(name) && words.length >= 3) {
            return "/" + root + " " + words[1] + " ";
        }
        return null;
    }
}
