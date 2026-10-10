package org.lts.qualityessentials.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lts.qualityessentials.chat.tabs.ChatRules;
import org.lts.qualityessentials.chat.tabs.TabRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTabsTest {
    private static final List<String> PROBLEMS = new ArrayList<>();
    private static final TabRules RULES = ChatRules.compile(ChatRules.defaults(), PROBLEMS::add);

    private static boolean in(String tab, String line) {
        return RULES.visible(RULES.indexOf(tab), RULES.classify(line));
    }

    @Test
    void defaultsCompileCleanly() {
        assertTrue(PROBLEMS.isEmpty(), PROBLEMS.toString());
        assertEquals(5, RULES.size());
        assertTrue(RULES.showsEverything(RULES.indexOf("all")));
        assertFalse(RULES.showsEverything(RULES.indexOf("talk")));
    }

    @Test
    void serverLinesLandInTheirTabs() {
        String join = " Nikitombo зашёл на сервер.";
        String leave = " BananBR вышел с сервера.";
        String client = "(VULCAN) Nikitombo присоединился с клиента с именем Fabric!";
        String flag = "(VULCAN) exelent провалил Elytra *(Type N)* [3/12]";
        String noise = "[] Поблизости нет никого, кому можно написать.";
        String talk = "[X] G |  VLAD666AD: как баннеры на карте установить?";
        String whisper = "[LTS_Server -> Вы] привет";

        assertTrue(in("joins", join));
        assertTrue(in("joins", leave));
        assertTrue(in("joins", client));
        assertTrue(in("anticheat", client));
        assertTrue(in("anticheat", flag));
        assertFalse(in("joins", flag));
        assertTrue(in("private", whisper));

        assertTrue(in("talk", talk));
        assertTrue(in("talk", whisper));
        for (String line : List.of(join, leave, client, flag, noise)) {
            assertFalse(in("talk", line), line);
            assertTrue(in("all", line), line);
        }
        assertFalse(in("private", talk));
        assertFalse(in("anticheat", "Steve: vulcan опять ругается"));
    }

    @Test
    void badRulesAreReportedAndSkipped() {
        List<String> problems = new ArrayList<>();
        TabRules rules = new TabRules(
                List.of(new TabRules.Group("a", List.of("(", "foo")), new TabRules.Group("a", List.of("bar"))),
                List.of(new TabRules.Tab("x", "X", List.of("a", "missing"), List.of()), new TabRules.Tab("", null, null, null)),
                problems::add);
        assertEquals(1, rules.size());
        assertTrue(rules.visible(0, rules.classify("foo")));
        assertFalse(rules.visible(0, rules.classify("bar")));
        assertEquals(2, problems.size(), problems.toString());
        TabRules empty = new TabRules(List.of(), List.of(), problems::add);
        assertEquals(1, empty.size());
        assertTrue(empty.showsEverything(0));
    }

    @Test
    void rulesFileIsCreatedAndRead(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("chat_tabs.json");
        ChatRules.FileData created = ChatRules.read(file);
        assertTrue(Files.isRegularFile(file));
        assertEquals(created.tabs.size(), ChatRules.read(file).tabs.size());
        Files.writeString(file, "{\"groups\":[{\"id\":\"trade\",\"patterns\":[\"(?iu)продам|куплю\"]}],"
                + "\"tabs\":[{\"id\":\"trade\",\"name\":\"Торг\",\"show\":[\"trade\"]}]}");
        TabRules rules = ChatRules.compile(ChatRules.read(file), PROBLEMS::add);
        assertEquals("Торг", rules.tab(0).name());
        assertTrue(rules.visible(0, rules.classify("Продам элитры")));
    }

    @Test
    void longMessagesSplitOnWords() {
        String text = "слово ".repeat(100).strip();
        List<String> parts = MessageSplitter.split(text, MessageSplitter.LIMIT);
        assertTrue(parts.size() >= 3);
        for (String part : parts) {
            assertTrue(part.length() <= MessageSplitter.LIMIT, part);
            assertFalse(part.startsWith(" ") || part.endsWith(" "));
        }
        assertEquals(text, String.join(" ", parts));
        assertEquals(List.of("short"), MessageSplitter.split("short", MessageSplitter.LIMIT));
    }

    @Test
    void longCommandsKeepTheirPrefix() {
        String body = "x".repeat(300);
        List<String> parts = MessageSplitter.split("/msg Steve " + body, MessageSplitter.LIMIT);
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).startsWith("/msg Steve x"));
        assertTrue(parts.get(1).startsWith("/msg Steve x"));
        assertEquals(300, parts.stream().mapToInt(part -> part.length() - "/msg Steve ".length()).sum());
        assertTrue(MessageSplitter.split("/r " + body, MessageSplitter.LIMIT).get(1).startsWith("/r "));
        assertEquals(List.of(), MessageSplitter.split("/give @p " + body, MessageSplitter.LIMIT));
        assertFalse(MessageSplitter.splittable("/give @p stone"));
        assertTrue(MessageSplitter.splittable("hello"));
    }
}
