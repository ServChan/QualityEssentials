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
        assertTrue(in("staff", client));
        assertTrue(in("staff", flag));
        assertFalse(in("joins", flag));
        assertTrue(in("private", whisper));

        assertTrue(in("talk", talk));
        assertTrue(in("talk", whisper));
        for (String line : List.of(join, leave, client, flag, noise)) {
            assertFalse(in("talk", line), line);
            assertTrue(in("all", line), line);
        }
        assertFalse(in("private", talk));
        assertFalse(in("staff", "Steve: vulcan опять ругается"));
    }

    @Test
    void realServerFormattingIsUnderstood() {
        String fancy = "\u276a\u1d20\u1d1c\u029f\u1d04\u1d00\u0274\u276b [vilochka head]vilochka провалил Speed (Type C) [1/6]";
        assertTrue(in("staff", fancy));
        assertFalse(in("talk", fancy));
        assertTrue(in("staff", "[Мат] [paincore head]paincore: майонезный"));
        assertFalse(in("talk", "[Мат] [paincore head]paincore: майонезный"));
        assertTrue(in("staff", "[Защита] Вы теперь в режиме модератора!"));
        assertTrue(in("joins", "[-] Отслеживаемый игрок \ua45d m1stresss вышел с сервера"));
        assertFalse(in("talk", "Авто-очистка сущностей: 34"));
        assertTrue(in("talk", "[X] \ua418 | [skyimpossible head]\ua41b skyimpossible: а че в чат пишешь"));
        assertTrue(in("talk", "[X]\ufffcLTS_Server\ud83d\udc09: Ну какой гад воду на спавне разлил"));
        assertEquals("(vulcan)", TabRules.normalize("\u276a\u1d20\u1d1c\u029f\u1d04\u1d00\u0274\u276b"));
    }

    @Test
    void talkKeepsOnlyPeople() {
        List<String> talk = List.of(
                "[X] \ua418 | [Heromattt head]\ua41b Heromattt \u2726: Найму человека разобрать ферму, делов на час",
                "\n\u256d\u2500 \ua422 Личное сообщение \u2022 [Merfych head]Merfych \u2192 вам\n\u2570 \u21b3 лады)\n",
                "\n\u256d\u2500 \ua422 Личное сообщение \u2022 вы \u2192 [Apel head]Apel\n\u2570 \u21b3 Приветики\n",
                "<Torvald> кто идёт в энд вечером?",
                "Torvald: участвую");
        List<String> staff = List.of(
                "\u276a\u1d20\u1d1c\u029f\u1d04\u1d00\u0274\u276b [Terrible_Fish head]Terrible_Fish провалил Elytra *(Type N)* [2/12]",
                "SquadFilter \u25ba SpoGi was detected using 'VPNGate' and got kicked! (104.28.232.199)",
                "SquadFilter \u25ba PassPigeon попытался подключиться, но ранее был заблокирован. (188.243.183.110)",
                "\ua425  [Red_yozh head]Red_yozh -> Teminick: хай",
                "[\u2714] [LTS_Server head]LTS_Server теперь наблюдает за личные сообщения.",
                "[Мат] [Defoltyx head]Defoltyx: /msg Erik400 всё иди",
                "[Защита] [21:45:50] [nikita_5641 head]nikita_5641 взял Diamond Axe в (-74 70 -5084 в Обычный мир)",
                "\n \u2139 [LTS_Server head]LTS_Server кикнул V0id_ (ID 2079)\n   \u270e Причина Багоюз.",
                "0,13/m назад + #bamboo установил bamboo.",
                "\u00a7f                 \u00a77^ (x395/y123/z-4227/world)\u00a77\u00a7o (a:block)",
                "\u00a7fСтраница \u00a7f1/10243 \u25b6 \u00a77(\u00a7f\u00a7n1\u00a7r \u00a77| \u00a7f2)",
                "CoreProtect - Выполняется поиск. Пожалуйста, подождите...",
                "----- CoreProtect |  Результаты Поиска -----");
        List<String> neither = List.of(
                "\ua429 [deksus head]deksus загрызен Зомби.",
                "\u203a Лицензионный аккаунт подтверждён. Вход выполнен автоматически.",
                "Применён эффект \u00abРегенерация\u00bb к 929 целям",
                "Путни\u1d0b м\u1d07жду ми\u1d18\u1d00ми Финн | Игрок [Rama head]Rama завершил \u00abСокровища Края\u00bb (смертей: 0)",
                "Финн \u00bb Я не лгал. Я просто не договаривал.",
                "Установлено значение игрового правила advance_time: false",
                "[Dunmere] Строительство завершено: сторожевая башня.",
                "[Реестр] Аукцион закрыт и убран в архив. [Аукцион] Сервер Minecraft: Закрыто 5/5",
                "[Server: Перемотка завершена с 112 тактами в секунду (8.91 мс за такт)]",
                "Авто-очистка сущностей: 4");
        for (String line : talk) {
            assertTrue(in("talk", line), line);
            assertFalse(in("staff", line), line);
        }
        for (String line : staff) {
            assertTrue(in("staff", line), line);
            assertFalse(in("talk", line), line);
        }
        for (String line : neither) {
            assertFalse(in("talk", line), line);
            assertFalse(in("staff", line), line);
            assertFalse(in("private", line), line);
        }
        assertTrue(in("private", talk.get(1)));
        assertTrue(in("private", talk.get(2)));
        assertFalse(in("private", staff.get(3)));
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
