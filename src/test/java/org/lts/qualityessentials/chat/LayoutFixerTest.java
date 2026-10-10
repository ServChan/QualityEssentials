package org.lts.qualityessentials.chat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.chat.layout.LanguageModel;
import org.lts.qualityessentials.chat.layout.LayoutFixer;
import org.lts.qualityessentials.chat.layout.LayoutMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutFixerTest {
    private static final Path LAYOUT = Path.of("src/client/resources/assets/qualityessentials/layout");
    private static final Set<String> COMMANDS = Set.of("spawn", "home", "msg", "tell", "r", "tpa", "warp", "me", "help", "gamemode");
    private static final Set<String> PLAYERS = Set.of("steve", "notch", "kirill");
    private static final Predicate<String> PLAYER = name -> PLAYERS.contains(name.toLowerCase(Locale.ROOT));
    private static final Predicate<String> NONE = name -> false;
    private static LayoutFixer fixer;

    @BeforeAll
    static void load() throws IOException {
        fixer = new LayoutFixer(model("ru", 33), model("en", 26));
    }

    private static LanguageModel model(String name, int alphabet) throws IOException {
        try (InputStream grams = Files.newInputStream(LAYOUT.resolve(name + ".grams"));
                InputStream words = Files.newInputStream(LAYOUT.resolve(name + ".words"))) {
            return LanguageModel.load(grams, words, alphabet);
        }
    }

    private static String fix(String input) {
        return fixer.fixInput(input, COMMANDS::contains, PLAYER, NONE, true, true);
    }

    @Test
    void mapsBothWaysAndKeepsCase() {
        assertEquals("Привет, как дела?", LayoutMap.toCyrillic("Ghbdtn? rfr ltkf&"));
        assertEquals("Ghbdtn? rfr ltkf&", LayoutMap.toLatin("Привет, как дела?"));
        assertEquals("/spawn", LayoutMap.toLatin(".ызфцт"));
    }

    @Test
    void fixesRussianTypedOnTheLatinLayout() {
        assertEquals("привет", fix("ghbdtn"));
        assertEquals("привет, как дела?", fix("ghbdtn? rfr ltkf&"));
        assertEquals("нет", fix("ytn"));
        assertEquals("да", fix("lf"));
        assertEquals("спасибо большое", fix("cgfcb,j ,jkmijt"));
        assertEquals("Привет всем", fix("Ghbdtn dctv"));
        assertEquals("ПРИВЕТ", fix("GHBDTN"));
        assertEquals("иду к тебе на базу", fix("ble r nt,t yf ,fpe"));
        assertEquals("что-то сломалось", fix("xnj-nj ckjvfkjcm"));
    }

    @Test
    void fixesEnglishTypedOnTheCyrillicLayout() {
        assertEquals("hello", fix("руддщ"));
        assertEquals("thanks everyone", fix("ерфтлы умукнщту"));
    }

    @Test
    void leavesRealTextAlone() {
        for (String text : List.of("hello", "gg wp", "afk brb", "lol", "ok", "привет", "как дела", "go go go", "xD", "tp me pls",
                "I need diamonds", "nice base bro", "wtf", "idk", "Steve", "ты где", "щас", "иду", "лут", "тут", "ну да",
                "https://lts-server.com/mods", "play.example.net", "&aGreen", "x2", "steve_2009", "iPhone", "LTS_Server")) {
            assertEquals(text, fix(text), text);
        }
    }

    @Test
    void keepsPlayerNamesAndIgnoredWords() {
        assertEquals("привет Steve", fix("ghbdtn Steve"));
        assertEquals("Notch привет", fix("Notch ghbdtn"));
        Predicate<String> ignored = word -> word.equalsIgnoreCase("ghbdtn");
        assertEquals("ghbdtn", fixer.fixInput("ghbdtn", COMMANDS::contains, PLAYER, ignored, true, true));
    }

    @Test
    void fixesCommandsTypedOnTheCyrillicLayout() {
        assertEquals("/spawn", fix("/ызфцт"));
        assertEquals("/spawn", fix(".ызфцт"));
        assertEquals("/home base", fix("/рщьу base"));
        assertEquals("/msg Steve привет", fix("/msg Steve ghbdtn"));
        assertEquals("/msg Steve привет", fix("/ьып Ыеуму ghbdtn"));
        assertEquals("/r где ты?", fix("/r ult ns&"));
        assertEquals("/unknown", fix("/unknown"));
        assertEquals("/фыва", fix("/фыва"));
        assertEquals(".привет", fix(".привет"));
        assertEquals("/gamemode creative", fix("/gamemode creative"));
    }

    @Test
    void switchesOffIndependently() {
        assertEquals("ghbdtn", fixer.fixInput("ghbdtn", COMMANDS::contains, PLAYER, NONE, false, true));
        assertEquals("/ызфцт", fixer.fixInput("/ызфцт", COMMANDS::contains, PLAYER, NONE, true, false));
    }

    @Test
    void manualFlipFollowsTheDominantLayout() {
        assertEquals("привет мир", fixer.flip("ghbdtn vbh", PLAYER));
        assertEquals("hello world", fixer.flip("руддщ цщкдв", PLAYER));
        assertEquals("/msg Steve привет", fixer.flip("/msg Steve ghbdtn", PLAYER));
        assertEquals("/spawn", fixer.flip("/ызфцт", PLAYER));
        assertEquals("привет", LayoutFixer.flipSelection("ghbdtn"));
    }

    @Test
    void chatVocabularyIsRecognised() throws IOException {
        List<String> missedRussian = new ArrayList<>();
        for (String word : words("ru_eval.txt")) {
            String typed = LayoutMap.toLatin(word);
            if (!fix(typed).equals(word)) {
                missedRussian.add(word);
            }
        }
        List<String> brokenEnglish = new ArrayList<>();
        for (String word : words("en_eval.txt")) {
            if (!fix(word).equals(word)) {
                brokenEnglish.add(word + "->" + fix(word));
            }
        }
        List<String> brokenRussian = new ArrayList<>();
        for (String word : words("ru_eval.txt")) {
            if (!fix(word).equals(word)) {
                brokenRussian.add(word + "->" + fix(word));
            }
        }
        assertTrue(brokenEnglish.isEmpty(), "English changed: " + brokenEnglish);
        assertTrue(brokenRussian.isEmpty(), "Russian changed: " + brokenRussian);
        assertTrue(missedRussian.size() * 20 <= words("ru_eval.txt").size(), "Missed: " + missedRussian);
    }

    private static List<String> words(String file) throws IOException {
        return List.of(Files.readString(Path.of("src/test/resources/layout").resolve(file)).trim().split("\\s+"));
    }
}
