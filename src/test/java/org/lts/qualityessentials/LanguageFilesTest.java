package org.lts.qualityessentials;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.chat.tabs.ChatRules;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.keyboard.layout.KeyCap;
import org.lts.qualityessentials.keyboard.layout.KeyboardLayout;
import org.lts.qualityessentials.lookat.LookAtFormat;
import org.lts.qualityessentials.text.FormatCode;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageFilesTest {
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final Path LANG = Path.of("src/client/resources/assets/qualityessentials/lang");
    private static final Pattern KEY_LITERAL = Pattern.compile("\"((?:key\\.)?qualityessentials\\.[a-z0-9_.]*[a-z0-9_])\"");
    private static final String[] OTHERS = {"ru_ru.json", "es_es.json", "es_mx.json"};

    private static Map<String, String> read(String file) throws IOException {
        try (Reader reader = Files.newBufferedReader(LANG.resolve(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, MAP_TYPE);
        }
    }

    @Test
    void allLocalesHaveTheSameKeys() throws IOException {
        Map<String, String> english = read("en_us.json");
        for (String locale : OTHERS) {
            assertEquals(new TreeSet<>(english.keySet()), new TreeSet<>(read(locale).keySet()), locale);
        }
    }

    @Test
    void everyKeyUsedInCodeIsTranslated() throws IOException {
        Map<String, String> english = read("en_us.json");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/client/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY_LITERAL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (key.endsWith(".json") || !english.containsKey(key) && isPrefix(english, key)) {
                        continue;
                    }
                    if (!english.containsKey(key)) {
                        missing.add(file.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing translations: " + missing);
    }

    private static boolean isPrefix(Map<String, String> english, String key) {
        String prefix = key + ".";
        return english.keySet().stream().anyMatch(existing -> existing.startsWith(prefix));
    }

    @Test
    void dynamicKeysAreTranslated() throws IOException {
        Map<String, String> english = read("en_us.json");
        List<String> expected = new ArrayList<>();
        for (Module module : Module.values()) {
            expected.add(module.translationKey());
            expected.add(module.translationKey() + ".description");
        }
        for (QEConfig.EquipmentAnchor anchor : QEConfig.EquipmentAnchor.values()) {
            expected.add("qualityessentials.anchor." + anchor.name().toLowerCase(Locale.ROOT));
        }
        for (QEConfig.DarkPreset preset : QEConfig.DarkPreset.values()) {
            expected.add("qualityessentials.dark." + preset.name().toLowerCase(Locale.ROOT));
        }
        for (int size : QEConfig.HISTORY_SIZES) {
            expected.add("qualityessentials.chat.history." + size);
        }
        for (QEConfig.ChatSize size : QEConfig.ChatSize.values()) {
            expected.add("qualityessentials.chat.size." + size.name().toLowerCase(Locale.ROOT));
        }
        for (ChatRules.TabData tab : ChatRules.defaults().tabs) {
            expected.add("qualityessentials.chat.tab." + tab.id);
        }
        for (QEConfig.ChatTimestamps style : QEConfig.ChatTimestamps.values()) {
            expected.add("qualityessentials.chat.time." + style.name().toLowerCase(Locale.ROOT));
        }
        for (String property : LookAtFormat.STATE_PROPERTIES) {
            if (!property.equals("age")) {
                expected.add("qualityessentials.lookat.state." + property);
            }
        }
        for (FormatCode code : FormatCode.values()) {
            expected.add(code.translationKey());
        }
        for (KeyCap cap : KeyboardLayout.mouse()) {
            expected.add(cap.label());
        }
        List<String> missing = expected.stream().filter(key -> !english.containsKey(key)).toList();
        assertTrue(missing.isEmpty(), "Missing translations: " + missing);
    }

    @Test
    void placeholderCountsMatchAcrossLocales() throws IOException {
        Map<String, String> english = read("en_us.json");
        Pattern placeholder = Pattern.compile("%(\\d+\\$)?s");
        for (String locale : OTHERS) {
            Map<String, String> translated = read(locale);
            for (Map.Entry<String, String> entry : english.entrySet()) {
                assertEquals(placeholder.matcher(entry.getValue()).results().count(),
                        placeholder.matcher(translated.get(entry.getKey())).results().count(), locale + " " + entry.getKey());
            }
        }
    }
}
