package org.lts.qualityessentials.screenshot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;

import java.io.File;
import java.util.function.Consumer;

public final class ScreenshotActions {
    private static final int OPEN = 0xFF55E07A;
    private static final int FOLDER = 0xFFFFD54F;
    private static final int COPY = 0xFF6FD3FF;

    private ScreenshotActions() {
    }

    public static Consumer<Component> wrap(Consumer<Component> callback) {
        if (callback == null) {
            return null;
        }
        return message -> callback.accept(decorate(message));
    }

    public static Component decorate(Component message) {
        if (!QE.active(Module.SCREENSHOTS)) {
            return message;
        }
        File file = findFile(message);
        if (file == null) {
            return message;
        }
        return withButtons(message, file, QE.config());
    }

    public static Component withButtons(Component message, File file, QEConfig config) {
        MutableComponent result = message.copy();
        if (config.screenshotOpen) {
            result.append(" ").append(button("open", OPEN, new ClickEvent.OpenFile(file)));
        }
        File folder = file.getAbsoluteFile().getParentFile();
        if (config.screenshotFolder && folder != null) {
            result.append(" ").append(button("folder", FOLDER, new ClickEvent.OpenFile(folder)));
        }
        if (config.screenshotCopy) {
            result.append(" ").append(button("copy", COPY, new ClickEvent.CopyToClipboard(file.getAbsolutePath())));
        }
        return result;
    }

    private static Component button(String id, int color, ClickEvent click) {
        Component hint = Component.translatable("qualityessentials.screenshot." + id + ".hint");
        return Component.literal("[").append(Component.translatable("qualityessentials.screenshot." + id)).append("]")
                .withStyle(style -> style.withColor(color).withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(hint)));
    }

    static File findFile(Component component) {
        if (component.getStyle().getClickEvent() instanceof ClickEvent.OpenFile open) {
            return open.file();
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component nested) {
                    File file = findFile(nested);
                    if (file != null) {
                        return file;
                    }
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            File file = findFile(sibling);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static Component sample(QEConfig config) {
        Component name = Component.literal("2026-10-09_14.30.12.png").withStyle(ChatFormatting.UNDERLINE);
        return withButtons(Component.translatable("screenshot.success", name), new File("screenshots", "2026-10-09_14.30.12.png"), config);
    }
}
