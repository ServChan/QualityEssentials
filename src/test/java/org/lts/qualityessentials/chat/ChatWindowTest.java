package org.lts.qualityessentials.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QEConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatWindowTest {
    @Test
    void normalSizeKeepsVanilla() {
        assertEquals(320, ChatWindow.width(320, 1.0, QEConfig.ChatSize.NORMAL, 960));
        assertEquals(180, ChatWindow.height(180, 1.0, QEConfig.ChatSize.NORMAL, 540));
    }

    @Test
    void factorsScaleAndStayOnScreen() {
        assertEquals(480, ChatWindow.width(320, 1.0, QEConfig.ChatSize.WIDE, 960));
        assertEquals(640, ChatWindow.width(320, 1.0, QEConfig.ChatSize.WIDER, 960));
        assertEquals(427 - ChatWindow.SIDE_MARGIN, ChatWindow.width(320, 1.0, QEConfig.ChatSize.WIDER, 427));
        assertEquals(240 - ChatWindow.BOTTOM_MARGIN - ChatWindow.TOP_MARGIN, ChatWindow.height(180, 1.0, QEConfig.ChatSize.WIDER, 240));
    }

    @Test
    void fullSizeFollowsTheVanillaSlider() {
        assertEquals(960 - ChatWindow.SIDE_MARGIN, ChatWindow.width(320, 1.0, QEConfig.ChatSize.FULL, 960));
        assertEquals(ChatWindow.MIN_WIDTH, ChatWindow.width(40, 0.0, QEConfig.ChatSize.FULL, 960));
        int half = ChatWindow.width(180, 0.5, QEConfig.ChatSize.FULL, 960);
        assertTrue(half > 180 && half < 960 - ChatWindow.SIDE_MARGIN, String.valueOf(half));
    }

    @Test
    void tinyWindowFallsBackToVanilla() {
        assertEquals(320, ChatWindow.width(320, 1.0, QEConfig.ChatSize.FULL, 300));
        assertEquals(180, ChatWindow.height(180, 1.0, QEConfig.ChatSize.FULL, 200));
    }

    @Test
    void chatConfigSanitizes(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("qualityessentials.json");
        Files.writeString(file, "{\"chatHistorySize\":5000,\"chatWidth\":\"huge\",\"chatHeight\":\"full\",\"chatTimestamps\":\"seconds\","
                + "\"chatLayoutIgnore\":[\" ghbdtn \",\"\",null,\"ghbdtn\"]}");
        QEConfig config = QEConfig.read(file);
        assertEquals(4096, config.chatHistorySize);
        assertEquals(QEConfig.ChatSize.NORMAL, config.chatWidthSize());
        assertEquals(QEConfig.ChatSize.FULL, config.chatHeightSize());
        assertEquals(QEConfig.ChatTimestamps.SECONDS, config.chatTimestampStyle());
        assertEquals(List.of("ghbdtn"), config.chatLayoutIgnore);
        config.chatFixWords = false;
        config.resetModule(Module.CHAT);
        assertTrue(config.chatFixWords);
        assertEquals(QEConfig.ChatSize.NORMAL, config.chatHeightSize());
    }
}
