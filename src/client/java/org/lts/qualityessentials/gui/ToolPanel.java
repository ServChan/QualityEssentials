package org.lts.qualityessentials.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ToolPanel {
    public static final int ROW = 14;
    public static final int GAP = 2;
    public static final int PADDING = 4;

    private final int x;
    private final int y;
    private final int width;
    private final Consumer<AbstractWidget> sink;
    private final List<Label> labels = new ArrayList<>();
    private int cursorY;
    private int height;

    public ToolPanel(int x, int y, int width, Consumer<AbstractWidget> sink) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.sink = sink;
        this.cursorY = y + PADDING;
    }

    public void section(Component title) {
        labels.add(new Label(title, x + PADDING, cursorY + 1));
        cursorY += 11;
    }

    public FlatButton[] row(Tool... tools) {
        int inner = width - PADDING * 2;
        int each = (inner - GAP * (tools.length - 1)) / tools.length;
        if (tools.length > 1) {
            Font font = Minecraft.getInstance().font;
            boolean cramped = false;
            for (Tool tool : tools) {
                cramped |= font.width(tool.label()) > each - 6;
            }
            if (cramped) {
                FlatButton[] buttons = new FlatButton[tools.length];
                for (int i = 0; i < tools.length; i++) {
                    buttons[i] = row(tools[i])[0];
                }
                return buttons;
            }
        }
        FlatButton[] buttons = new FlatButton[tools.length];
        int bx = x + PADDING;
        for (int i = 0; i < tools.length; i++) {
            int w = i == tools.length - 1 ? x + width - PADDING - bx : each;
            Tool tool = tools[i];
            FlatButton button = new FlatButton(bx, cursorY, w, ROW, tool.label(), tool.style(), tool.action());
            if (tool.hint() != null) {
                button.withHint(tool.hint());
            }
            sink.accept(button);
            buttons[i] = button;
            bx += w + GAP;
        }
        cursorY += ROW + GAP;
        return buttons;
    }

    public <T extends AbstractWidget> T place(T widget) {
        sink.accept(widget);
        return widget;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int innerX() {
        return x + PADDING;
    }

    public int innerWidth() {
        return width - PADDING * 2;
    }

    public int cursorY() {
        return cursorY;
    }

    public void skip(int amount) {
        cursorY += amount;
    }

    public void finish() {
        height = cursorY - y + PADDING - GAP;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void drawBackground(GuiGraphicsExtractor graphics) {
        Ui.panel(graphics, x, y, width, height, Palette.BACKGROUND, Palette.BORDER);
    }

    public void drawLabels(GuiGraphicsExtractor graphics, Font font) {
        for (Label label : labels) {
            graphics.text(font, label.text(), label.x(), label.y(), Palette.TEXT_DIM, false);
        }
    }

    public record Tool(Component label, Component hint, FlatButton.Style style, Runnable action) {
        public static Tool of(String key, Runnable action) {
            return new Tool(Component.translatable(key), hintOf(key), FlatButton.Style.SECONDARY, action);
        }

        public static Tool danger(String key, Runnable action) {
            return new Tool(Component.translatable(key), hintOf(key), FlatButton.Style.DANGER, action);
        }

        private static Component hintOf(String key) {
            return Component.translatable(key + ".hint");
        }
    }

    private record Label(Component text, int x, int y) {
    }
}
