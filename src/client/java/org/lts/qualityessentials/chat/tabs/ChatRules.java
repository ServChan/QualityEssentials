package org.lts.qualityessentials.chat.tabs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ChatRules {
    public static final int VERSION = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ChatRules() {
    }

    public static FileData defaults() {
        FileData data = new FileData();
        data.groups.add(group("chat",
                "(?i)^\\s*\\[X\\]",
                "^<[A-Za-z0-9_]{3,16}>\\s",
                "(?U)^\\W*(?:\\[[^\\]]{1,32}\\]\\s*)*(?:\\S{1,2}\\s+)?[A-Za-z0-9_]{3,16}(?:\\s+\\S{1,3})?\\s*:\\s"));
        data.groups.add(group("private",
                "(?iu)личное сообщение",
                "(?iuU)(?:->|→)\\s*(?:вы|вам|мне|я|me|you)(?:\\W|$)",
                "(?iuU)(?:^|\\W)(?:вы|я|me|you)\\s*(?:->|→)",
                "(?iu)(?:шепчет|whispers)",
                "(?iuU)^\\W*\\[(?:лс|пм|pm|msg|dm|whisper)\\]"));
        data.groups.add(group("joins",
                "(?iu)(?:зашёл|зашел|зашла|вошёл|вошел|вошла)\\s+(?:на|в)\\s+(?:сервер|игру)",
                "(?iu)(?:вышел|вышла|покинул|покинула)\\s+(?:с|из)\\s+(?:сервера|игры)",
                "(?iu)присоединил(?:ся|ась)\\s+(?:к\\s+(?:серверу|игре)|с\\s+клиента)",
                "(?iuU)^\\W*\\[[+-]\\]\\s*отслеживаемый игрок",
                "(?i)\\b(?:joined|left) the (?:game|server)\\b"));
        data.groups.add(group("anticheat",
                "(?iU)^\\W*[\\[(]\\s*(?:vulcan|grim|grimac|matrix|spartan|themis|polar|intave|verus|karhu)\\s*[\\])]",
                "(?iU)^\\W*squadfilter\\b"));
        data.groups.add(group("moderation",
                "(?iuU)^\\W*\\[(?:мат|защита|спам|реклама|mute|ban|warn)\\]",
                "(?iuU)^\\W*(?:\\S{1,2}\\s+)?(?:\\[[^\\]]{1,32}\\]\\s*)?[A-Za-z0-9_]{3,16}\\s+(?:кикнул|забанил|замутил|разбанил|размутил|предупредил|kicked|banned|muted|warned)\\s",
                "(?iu)наблюдает за личн"));
        data.groups.add(group("spy",
                "(?U)^\\W*(?:\\S{1,2}\\s+)?(?:\\[[^\\]]{1,32}\\]\\s*)?[A-Za-z0-9_]{3,16}\\s*(?:->|→)\\s*[A-Za-z0-9_]{3,16}\\s*:"));
        data.groups.add(group("coreprotect",
                "(?iU)^\\W*coreprotect\\b",
                "(?iuU)^\\W*[\\d.,]+/[smhdwy]\\s+(?:назад|ago)",
                "(?U)^\\W*\\^\\s*\\(x-?\\d+/y-?\\d+/z-?\\d+/",
                "(?iuU)^\\W*(?:страница|page)\\s+\\d+/\\d+",
                "(?iuU)^\\W*транзакции контейнеров"));
        data.groups.add(group("noise",
                "(?iu)поблизости нет никого",
                "(?i)(?:no ?one|nobody) (?:is )?(?:near|around)",
                "(?iuU)^\\W*авто-очистка сущностей",
                "(?iU)^\\W*\\[server:"));
        data.tabs.add(tab("all", List.of(TabRules.EVERYTHING), List.of()));
        data.tabs.add(tab("talk", List.of("chat", "private"), List.of("anticheat", "moderation", "spy", "coreprotect", "noise")));
        data.tabs.add(tab("private", List.of("private"), List.of()));
        data.tabs.add(tab("joins", List.of("joins"), List.of()));
        data.tabs.add(tab("staff", List.of("anticheat", "moderation", "spy", "coreprotect"), List.of()));
        return data;
    }

    public static TabRules compile(FileData data, Consumer<String> problem) {
        List<TabRules.Group> groups = new ArrayList<>();
        List<TabRules.Tab> tabs = new ArrayList<>();
        if (data != null && data.groups != null) {
            for (GroupData group : data.groups) {
                if (group != null) {
                    groups.add(new TabRules.Group(group.id, group.patterns));
                }
            }
        }
        if (data != null && data.tabs != null) {
            for (TabData tab : data.tabs) {
                if (tab != null) {
                    tabs.add(new TabRules.Tab(tab.id, tab.name, tab.show, tab.hide));
                }
            }
        }
        return new TabRules(groups, tabs, problem);
    }

    public static FileData read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            FileData defaults = defaults();
            write(file, defaults);
            return defaults;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        FileData data = GSON.fromJson(json, FileData.class);
        if (data == null || data.version < VERSION) {
            int old = data == null ? 0 : data.version;
            Files.writeString(file.resolveSibling(file.getFileName() + ".v" + old + ".bak"), json, StandardCharsets.UTF_8);
            FileData defaults = defaults();
            write(file, defaults);
            return defaults;
        }
        return data;
    }

    public static void write(Path file, FileData data) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
    }

    private static GroupData group(String id, String... patterns) {
        GroupData group = new GroupData();
        group.id = id;
        group.patterns = List.of(patterns);
        return group;
    }

    private static TabData tab(String id, List<String> show, List<String> hide) {
        TabData tab = new TabData();
        tab.id = id;
        tab.show = show;
        tab.hide = hide;
        return tab;
    }

    public static final class FileData {
        public int version = VERSION;
        public List<GroupData> groups = new ArrayList<>();
        public List<TabData> tabs = new ArrayList<>();
    }

    public static final class GroupData {
        public String id;
        public List<String> patterns = new ArrayList<>();
    }

    public static final class TabData {
        public String id;
        public String name;
        public List<String> show = new ArrayList<>();
        public List<String> hide = new ArrayList<>();
    }
}
