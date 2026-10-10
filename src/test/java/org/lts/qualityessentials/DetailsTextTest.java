package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.hidden.DetailsText;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailsTextTest {
    @Test
    void wrapsComponentListsOnCommas() {
        List<String> lines = DetailsText.wrap(List.of("custom_name", "lore", "item_model", "enchantments", "tooltip_display", "attribute_modifiers"), 30);
        assertEquals(List.of("custom_name, lore, item_model,", "enchantments, tooltip_display,", "attribute_modifiers"), lines);
        for (String line : lines) {
            assertTrue(line.length() <= 31, line);
        }
        assertEquals(List.of(), DetailsText.wrap(List.of(), 30));
    }

    @Test
    void shortensAndFormats() {
        assertEquals("abc", DetailsText.shorten("abc", 5));
        assertEquals("abcd…", DetailsText.shorten("abcdefgh", 5));
        assertEquals("a b", DetailsText.shorten("a\nb", 5));
        assertEquals("enchantments", DetailsText.stripNamespace("minecraft:enchantments"));
        assertEquals("server:jade", DetailsText.stripNamespace("server:jade"));
        assertEquals(90, DetailsText.percent(1820, 2031));
        assertEquals(0, DetailsText.percent(5, 0));
    }
}
