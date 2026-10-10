package org.lts.qualityessentials.chat;

import com.google.common.collect.MapMaker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lts.qualityessentials.core.QEConfig;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class ChatDecor {
    private static final Map<GuiMessage, Meta> META = new MapMaker().weakKeys().makeMap();
    private static String pattern;
    private static DateTimeFormatter formatter;

    private ChatDecor() {
    }

    public static void register(GuiMessage message, List<GuiMessage> all, List<GuiMessage.Line> trimmed) {
        long now = System.currentTimeMillis();
        int count = 1;
        if (ChatModule.active() && ChatModule.config().chatCollapse && !all.isEmpty()) {
            GuiMessage newest = all.getFirst();
            Meta previous = META.get(newest);
            if (previous != null && previous.kind == Kind.LIVE && newest.source() == message.source() && sameText(newest, message)) {
                count = previous.count + 1;
                all.removeFirst();
                trimmed.removeIf(line -> line.parent() == newest);
            }
        }
        META.put(message, new Meta(now, count, Kind.LIVE));
        ChatSessions.markDirty();
    }

    static void markRestored(GuiMessage message, long time) {
        META.put(message, new Meta(time, 1, Kind.RESTORED));
    }

    static void markSeparator(GuiMessage message) {
        META.put(message, new Meta(System.currentTimeMillis(), 1, Kind.SEPARATOR));
    }

    static boolean isSeparator(GuiMessage message) {
        Meta meta = META.get(message);
        return meta != null && meta.kind == Kind.SEPARATOR;
    }

    static Long knownTime(GuiMessage message) {
        Meta meta = META.get(message);
        return meta == null || meta.kind == Kind.SEPARATOR ? null : meta.time;
    }

    static long time(GuiMessage message) {
        Meta meta = META.get(message);
        return meta == null ? System.currentTimeMillis() : meta.time;
    }

    public static GuiMessage decorate(GuiMessage message) {
        if (!ChatModule.active()) {
            return message;
        }
        Meta meta = META.get(message);
        if (meta == null || meta.kind == Kind.SEPARATOR) {
            return message;
        }
        QEConfig config = ChatModule.config();
        QEConfig.ChatTimestamps style = config.chatTimestampStyle();
        boolean stamp = style != QEConfig.ChatTimestamps.OFF && !config.chatTimestampsHover;
        if (!stamp && meta.count < 2) {
            return message;
        }
        MutableComponent content = Component.empty();
        if (stamp) {
            content.append(Component.literal(format(style, meta.time) + " ").withStyle(ChatFormatting.DARK_GRAY));
        }
        content.append(message.content());
        if (meta.count > 1) {
            content.append(Component.literal(" (×" + meta.count + ")").withStyle(ChatFormatting.GRAY));
        }
        return new GuiMessage(message.addedTime(), content, message.signature(), message.source(), message.tag());
    }

    public static String format(QEConfig.ChatTimestamps style, long time) {
        if (!style.pattern().equals(pattern)) {
            pattern = style.pattern();
            formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        }
        return formatter.format(Instant.ofEpochMilli(time));
    }

    private static boolean sameText(GuiMessage a, GuiMessage b) {
        String text = b.content().getString();
        return !text.isBlank() && text.equals(a.content().getString());
    }

    private enum Kind {
        LIVE,
        RESTORED,
        SEPARATOR
    }

    private record Meta(long time, int count, Kind kind) {
    }
}
