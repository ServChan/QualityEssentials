package org.lts.qualityessentials.chat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lts.qualityessentials.ModLog;
import org.lts.qualityessentials.QualityEssentialsClient;
import org.lts.qualityessentials.chat.layout.LanguageModel;
import org.lts.qualityessentials.chat.layout.LayoutFixer;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class ChatModule {
    private static final String LAYOUT = "/assets/qualityessentials/layout/";

    public static KeyMapping flipKey;
    private static volatile LayoutFixer fixer;
    private static final long SEND_DELAY_MILLIS = 1500L;
    private static final Deque<String> OUTGOING = new ArrayDeque<>();
    private static Object lastLayout;
    private static String whole;
    private static long nextSendAt;
    private static final Set<String> REPORTED_FIX_FAILURES = new HashSet<>();

    private ChatModule() {
    }

    public static void init(KeyMapping.Category category) {
        flipKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.qualityessentials.chat_layout", InputConstants.Type.KEYBOARD, InputConstants.KEY_PAUSE, category));
        CompletableFuture.runAsync(() -> {
            try {
                fixer = new LayoutFixer(model("ru", 33), model("en", 26));
            } catch (IOException | RuntimeException exception) {
                QualityEssentialsClient.LOGGER.warn("Could not load the keyboard layout model; the layout fix is off", exception);
            }
        });
    }

    private static LanguageModel model(String name, int alphabet) throws IOException {
        try (InputStream grams = Objects.requireNonNull(ChatModule.class.getResourceAsStream(LAYOUT + name + ".grams"));
                InputStream words = Objects.requireNonNull(ChatModule.class.getResourceAsStream(LAYOUT + name + ".words"))) {
            return LanguageModel.load(grams, words, alphabet);
        }
    }

    public static boolean active() {
        return QE.active(Module.CHAT);
    }

    public static QEConfig config() {
        return QE.config();
    }

    public static int historyLimit(int vanilla) {
        return active() ? Math.max(vanilla, config().chatHistorySize) : vanilla;
    }

    public static int width(int vanilla, double pct) {
        Window window = window();
        if (window == null || !active()) {
            return vanilla;
        }
        return ChatWindow.width(vanilla, pct, config().chatWidthSize(), window.getGuiScaledWidth());
    }

    public static int height(int vanilla, double pct) {
        Window window = window();
        if (window == null || !active()) {
            return vanilla;
        }
        return ChatWindow.height(vanilla, pct, config().chatHeightSize(), window.getGuiScaledHeight());
    }

    private static Window window() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getWindow();
    }

    public static void tick(Minecraft minecraft) {
        ChatSessions.tick(minecraft);
        ChatView.tick(minecraft);
        sendQueued(minecraft);
        Window window = minecraft.getWindow();
        QEConfig config = config();
        boolean active = active();
        boolean resized = config.chatWidthSize() != QEConfig.ChatSize.NORMAL || config.chatHeightSize() != QEConfig.ChatSize.NORMAL;
        Object layout = List.of(active, config.chatWidth, config.chatHeight, config.chatTimestamps, config.chatTimestampsHover, config.chatCollapse, config.chatTabs,
                resized ? window.getGuiScaledWidth() : 0, resized ? window.getGuiScaledHeight() : 0);
        if (!layout.equals(lastLayout)) {
            boolean first = lastLayout == null;
            lastLayout = layout;
            if (!first && minecraft.level != null) {
                minecraft.gui.hud.getChat().rescaleChat();
            }
        }
    }

    public static String prepareSend(String message) {
        whole = null;
        if (message == null || !active() || !config().chatLongMessages) {
            return message;
        }
        String text = message.strip();
        if (!OUTGOING.isEmpty() && !text.isEmpty()) {
            OUTGOING.addAll(MessageSplitter.split(text, MessageSplitter.LIMIT));
            Minecraft.getInstance().gui.hud.getChat().addRecentChat(text);
            return "";
        }
        if (text.length() <= MessageSplitter.LIMIT) {
            return message;
        }
        List<String> parts = MessageSplitter.split(text, MessageSplitter.LIMIT);
        if (parts.size() < 2) {
            return message;
        }
        whole = text;
        OUTGOING.addAll(parts.subList(1, parts.size()));
        nextSendAt = System.currentTimeMillis() + SEND_DELAY_MILLIS;
        return parts.getFirst();
    }

    public static String wholeMessage(String message) {
        String full = whole;
        whole = null;
        return full != null ? full : message;
    }

    public static int queued() {
        return OUTGOING.size();
    }

    private static void sendQueued(Minecraft minecraft) {
        if (OUTGOING.isEmpty()) {
            return;
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            OUTGOING.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextSendAt) {
            return;
        }
        String part = StringUtils.normalizeSpace(OUTGOING.poll().strip());
        nextSendAt = now + SEND_DELAY_MILLIS;
        if (part.isEmpty()) {
            return;
        }
        if (part.startsWith("/")) {
            connection.sendCommand(part.substring(1));
        } else {
            connection.sendChat(part);
        }
    }

    public static String fixInput(String message) {
        LayoutFixer current = fixer;
        if (current == null || message == null || !active()) {
            return message;
        }
        QEConfig config = config();
        if (!config.chatFixWords && !config.chatFixCommands) {
            return message;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        Predicate<String> knownCommand = name -> connection != null && connection.getCommands().getRoot().getChild(name) != null;
        Set<String> ignored = lower(config.chatLayoutIgnore);
        try {
            return current.fixInput(message, knownCommand, playerNames(connection), word -> ignored.contains(word.toLowerCase(Locale.ROOT)),
                    config.chatFixWords, config.chatFixCommands);
        } catch (RuntimeException exception) {
            if (REPORTED_FIX_FAILURES.add(ModLog.describe(exception))) {
                QualityEssentialsClient.LOGGER.warn("Could not fix the keyboard layout of a chat message; sending it unchanged", exception);
            }
            return message;
        }
    }

    public static boolean flip(EditBox input, KeyEvent event) {
        LayoutFixer current = fixer;
        if (current == null || flipKey == null || !active() || !flipKey.matches(event)) {
            return false;
        }
        String highlighted = input.getHighlighted();
        if (!highlighted.isEmpty()) {
            input.insertText(LayoutFixer.flipSelection(highlighted));
            return true;
        }
        int cursor = input.getCursorPosition();
        String value = input.getValue();
        String flipped = current.flip(value, playerNames(Minecraft.getInstance().getConnection()));
        if (!flipped.equals(value)) {
            input.setValue(flipped);
            input.setCursorPosition(Math.min(cursor, flipped.length()));
        }
        return true;
    }

    private static Predicate<String> playerNames(ClientPacketListener connection) {
        Set<String> names = new HashSet<>();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                names.add(info.getProfile().name().toLowerCase(Locale.ROOT));
            }
        }
        return name -> names.contains(name.toLowerCase(Locale.ROOT));
    }

    private static Set<String> lower(Iterable<String> words) {
        Set<String> out = new HashSet<>();
        for (String word : words) {
            out.add(word.toLowerCase(Locale.ROOT));
        }
        return out;
    }
}
