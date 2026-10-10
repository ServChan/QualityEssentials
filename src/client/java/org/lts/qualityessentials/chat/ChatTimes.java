package org.lts.qualityessentials.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Mth;
import org.lts.qualityessentials.core.QEConfig;

import java.util.List;

public final class ChatTimes {
    private static final int BACKGROUND = 0xE0141414;
    private static final int TEXT = 0xFFB0B0B0;
    private static final int DIVIDER = 0xFF3A3A3A;

    private ChatTimes() {
    }

    public static boolean onHover(QEConfig config) {
        return config.chatTimestampStyle() != QEConfig.ChatTimestamps.OFF && config.chatTimestampsHover;
    }

    public static int zoneWidth(Font font, QEConfig.ChatTimestamps style) {
        return font.width(style == QEConfig.ChatTimestamps.SECONDS ? "00:00:00" : "00:00") + 8;
    }

    public static void extract(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, List<GuiMessage.Line> lines, int scroll,
            int perPage, Minecraft minecraft) {
        if (!ChatModule.active() || !onHover(ChatModule.config()) || lines.isEmpty()) {
            return;
        }
        QEConfig.ChatTimestamps style = ChatModule.config().chatTimestampStyle();
        Options options = minecraft.options;
        float scale = options.chatScale().get().floatValue();
        if (scale <= 0f) {
            return;
        }
        int count = Math.min(lines.size() - scroll, perPage);
        if (count <= 0) {
            return;
        }
        double spacing = options.chatLineSpacing().get();
        int entryHeight = (int) (9.0 * (spacing + 1.0));
        int textOffset = (int) Math.round(8.0 * (spacing + 1.0) - 4.0 * spacing);
        int chatBottom = Mth.floor((graphics.guiHeight() - 40) / scale);
        int chatTop = chatBottom - count * entryHeight;
        int zone = zoneWidth(font, style);
        float localX = mouseX / scale - 4f;
        float localY = mouseY / scale;
        if (localX < -4f || localX > zone || localY < chatTop || localY > chatBottom) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(4f, 0f);
        for (int i = 0; i < count; i++) {
            int index = i + scroll;
            GuiMessage.Line line = lines.get(index);
            boolean firstLine = index + 1 >= lines.size() || lines.get(index + 1).parent() != line.parent();
            if (!firstLine && i != count - 1) {
                continue;
            }
            Long time = ChatDecor.knownTime(line.parent());
            if (time == null) {
                continue;
            }
            int entryBottom = chatBottom - i * entryHeight;
            String text = ChatDecor.format(style, time);
            int width = font.width(text) + 4;
            graphics.fill(0, entryBottom - entryHeight, width, entryBottom, BACKGROUND);
            graphics.fill(width, entryBottom - entryHeight, width + 1, entryBottom, DIVIDER);
            graphics.text(font, text, 2, entryBottom - textOffset, TEXT, false);
        }
        graphics.pose().popMatrix();
    }
}
