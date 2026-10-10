package org.lts.qualityessentials.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lts.qualityessentials.core.QEConfig;

import java.util.List;

public final class ChatOverlay {
    private static final int TEXT = 0xB0B0B0;
    private static final int DIVIDER = 0x3A3A3A;
    private static final float SLIDE_MILLIS = 160f;

    private static float progress;
    private static long lastFrame;
    private static int shift;

    private ChatOverlay() {
    }

    public static boolean onHover(QEConfig config) {
        return config.chatTimestampStyle() != QEConfig.ChatTimestamps.OFF && config.chatTimestampsHover;
    }

    public static int zoneWidth(Font font, QEConfig.ChatTimestamps style) {
        return font.width(style == QEConfig.ChatTimestamps.SECONDS ? "00:00:00" : "00:00") + 8;
    }

    public static int shift(ChatComponent.DisplayMode displayMode) {
        return displayMode.foreground ? shift : 0;
    }

    public static void begin(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode,
            List<GuiMessage.Line> lines, int scroll, int perPage, Minecraft minecraft) {
        if (!displayMode.foreground) {
            return;
        }
        long now = Util.getMillis();
        float elapsed = lastFrame == 0L ? 0f : Math.min(SLIDE_MILLIS, now - lastFrame);
        lastFrame = now;
        Geometry geometry = geometry(graphics, minecraft, lines, scroll, perPage);
        boolean hovered = false;
        int zone = 0;
        if (geometry != null && ChatModule.active() && onHover(ChatModule.config())) {
            zone = zoneWidth(font, ChatModule.config().chatTimestampStyle());
            float localX = mouseX / geometry.scale - 4f;
            float localY = mouseY / geometry.scale;
            hovered = localX >= -4f && localX <= zone && localY >= geometry.top() && localY <= geometry.bottom;
        }
        float step = elapsed / SLIDE_MILLIS;
        progress = hovered ? Math.min(1f, progress + step) : Math.max(0f, progress - step);
        float eased = 1f - (1f - progress) * (1f - progress) * (1f - progress);
        shift = Math.round(eased * zone);
    }

    public static void end() {
        lastFrame = 0L;
        progress = 0f;
        shift = 0;
    }

    public static void extract(GuiGraphicsExtractor graphics, Font font, ChatComponent.DisplayMode displayMode, List<GuiMessage.Line> lines,
            int scroll, int perPage, Minecraft minecraft) {
        if (!displayMode.foreground || !ChatModule.active()) {
            return;
        }
        GuiMessage flash = ChatView.flashing();
        if (shift <= 0 && flash == null) {
            return;
        }
        Geometry geometry = geometry(graphics, minecraft, lines, scroll, perPage);
        if (geometry == null) {
            return;
        }
        Options options = minecraft.options;
        graphics.pose().pushMatrix();
        graphics.pose().scale(geometry.scale, geometry.scale);
        graphics.pose().translate(4f, 0f);
        if (flash != null) {
            int maxWidth = Mth.ceil(ChatComponent.getWidth(options.chatWidth().get()) / geometry.scale);
            for (int i = 0; i < geometry.count; i++) {
                if (lines.get(i + scroll).parent() == flash) {
                    int entryBottom = geometry.bottom - i * geometry.entryHeight;
                    graphics.fill(-4 + shift, entryBottom - geometry.entryHeight, maxWidth + 8 + shift, entryBottom, ChatView.flashColor());
                }
            }
        }
        if (shift > 0) {
            QEConfig.ChatTimestamps style = ChatModule.config().chatTimestampStyle();
            int zone = zoneWidth(font, style);
            int background = ARGB.black(options.textBackgroundOpacity().get().floatValue());
            int alpha = Math.round(255f * shift / Math.max(1, zone));
            graphics.fill(-4, geometry.top(), shift - 4, geometry.bottom, background);
            graphics.fill(shift - 5, geometry.top(), shift - 4, geometry.bottom, ARGB.color(alpha, DIVIDER));
            for (int i = 0; i < geometry.count; i++) {
                int index = i + scroll;
                GuiMessage.Line line = lines.get(index);
                boolean firstLine = index + 1 >= lines.size() || lines.get(index + 1).parent() != line.parent();
                if (!firstLine && i != geometry.count - 1) {
                    continue;
                }
                Long time = ChatDecor.knownTime(line.parent());
                if (time == null) {
                    continue;
                }
                int entryBottom = geometry.bottom - i * geometry.entryHeight;
                graphics.text(font, ChatDecor.format(style, time), shift - zone, entryBottom - geometry.textOffset, ARGB.color(alpha, TEXT), false);
            }
        }
        graphics.pose().popMatrix();
    }

    private static Geometry geometry(GuiGraphicsExtractor graphics, Minecraft minecraft, List<GuiMessage.Line> lines, int scroll, int perPage) {
        Options options = minecraft.options;
        float scale = options.chatScale().get().floatValue();
        int count = Math.min(lines.size() - scroll, perPage);
        if (scale <= 0f || count <= 0) {
            return null;
        }
        double spacing = options.chatLineSpacing().get();
        int entryHeight = (int) (9.0 * (spacing + 1.0));
        int textOffset = (int) Math.round(8.0 * (spacing + 1.0) - 4.0 * spacing);
        int bottom = Mth.floor((graphics.guiHeight() - 40) / scale);
        return new Geometry(scale, count, entryHeight, textOffset, bottom);
    }

    private record Geometry(float scale, int count, int entryHeight, int textOffset, int bottom) {
        int top() {
            return bottom - count * entryHeight;
        }
    }
}
