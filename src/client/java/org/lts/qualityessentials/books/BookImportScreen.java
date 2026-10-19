package org.lts.qualityessentials.books;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.gui.FlatButton;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.Ui;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class BookImportScreen extends Screen {
    private static final int ROW = 22;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final Screen parent;
    private final BookEditor editor;
    private List<BookFiles.Entry> entries;
    private int selected = -1;
    private int scroll;
    private String preview;
    private int previewPages;
    private boolean previewFailed;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int previewX;
    private int previewWidth;
    private FlatButton insertButton;
    private FlatButton replaceButton;

    public BookImportScreen(Screen parent, BookEditor editor) {
        super(Component.translatable("qualityessentials.books.import.title"));
        this.parent = parent;
        this.editor = editor;
    }

    @Override
    protected void init() {
        int margin = 12;
        int top = 34;
        int bottom = this.height - 34;
        int total = Math.min(this.width - margin * 2, 560);
        listX = (this.width - total) / 2;
        listWidth = Math.max(140, total * 5 / 11);
        previewX = listX + listWidth + 8;
        previewWidth = listX + total - previewX;
        listY = top;
        listHeight = bottom - top;
        int buttonY = this.height - 26;
        int buttonWidth = Math.min(130, (total - 18) / 4);
        int x = (this.width - buttonWidth * 4 - 18) / 2;
        insertButton = addRenderableWidget(new FlatButton(x, buttonY, buttonWidth, 18, Component.translatable("qualityessentials.books.import.insert"),
                FlatButton.Style.PRIMARY, () -> apply(false)).withHint(Component.translatable("qualityessentials.books.import.insert.hint")));
        replaceButton = addRenderableWidget(new FlatButton(x + buttonWidth + 6, buttonY, buttonWidth, 18, Component.translatable("qualityessentials.books.import.replace"),
                FlatButton.Style.DANGER, () -> apply(true)).withHint(Component.translatable("qualityessentials.books.import.replace.hint")));
        addRenderableWidget(new FlatButton(x + (buttonWidth + 6) * 2, buttonY, buttonWidth, 18, Component.translatable("qualityessentials.books.import.folder"),
                FlatButton.Style.SECONDARY, BookFiles::openFolder));
        addRenderableWidget(new FlatButton(x + (buttonWidth + 6) * 3, buttonY, buttonWidth, 18, Component.translatable("qualityessentials.books.import.back"),
                FlatButton.Style.SECONDARY, this::onClose));
        refreshButtons();
        if (entries == null) {
            BookFiles.list(list -> {
                entries = list;
                if (!list.isEmpty() && selected < 0) {
                    select(0);
                }
                refreshButtons();
            });
        }
    }

    private void refreshButtons() {
        boolean ready = preview != null && !previewFailed;
        insertButton.active = ready;
        replaceButton.active = ready;
    }

    private void select(int index) {
        selected = index;
        preview = null;
        previewFailed = false;
        refreshButtons();
        BookFiles.Entry entry = entries.get(index);
        BookFiles.read(entry.path(), text -> {
            if (selected == index) {
                preview = text;
                previewPages = BookText.paginate(BookText.clean(text, QE.config().booksAmpersand), RichPages.fit(this.font)).size();
                refreshButtons();
            }
        }, error -> {
            if (selected == index) {
                previewFailed = true;
                preview = "";
                refreshButtons();
            }
        });
    }

    private void apply(boolean replace) {
        if (preview == null || previewFailed) {
            return;
        }
        String text = preview;
        this.minecraft.gui.setScreen(parent);
        editor.importText(text, replace);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, Palette.BACKGROUND);
        graphics.fill(0, 0, this.width, 26, Palette.HEADER);
        graphics.fill(0, 25, this.width, 26, Palette.BORDER);
        Ui.scaledText(graphics, this.font, this.title, 10, 7, 1.5f, Palette.TEXT_BRIGHT, true);
        Component path = Component.literal(BookFiles.dir().toString());
        int titleEnd = 10 + Math.round(this.font.width(this.title) * 1.5f) + 10;
        graphics.text(this.font, this.font.plainSubstrByWidth(path.getString(), Math.max(0, this.width - titleEnd - 8)), titleEnd, 10, Palette.TEXT_DIM, false);
        Ui.panel(graphics, listX, listY, listWidth, listHeight, Palette.CASE, Palette.BORDER);
        Ui.panel(graphics, previewX, listY, previewWidth, listHeight, Palette.CASE, Palette.BORDER);
        if (entries == null) {
            graphics.centeredText(this.font, Component.translatable("qualityessentials.books.import.loading"), listX + listWidth / 2, listY + listHeight / 2 - 4, Palette.TEXT_DIM);
        } else if (entries.isEmpty()) {
            drawEmpty(graphics);
        } else {
            drawList(graphics, mouseX, mouseY);
            drawPreview(graphics);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawEmpty(GuiGraphicsExtractor graphics) {
        int centerX = listX + (listWidth + 8 + previewWidth) / 2;
        int y = listY + listHeight / 2 - 24;
        Ui.scaledText(graphics, this.font, Component.translatable("qualityessentials.books.import.empty"),
                centerX - Math.round(this.font.width(Component.translatable("qualityessentials.books.import.empty")) * 1.25f) / 2, y, 1.25f, Palette.TEXT_BRIGHT, false);
        List<FormattedCharSequence> lines = this.font.split(Component.translatable("qualityessentials.books.import.empty.hint"), Math.min(320, listWidth + previewWidth - 20));
        for (int i = 0; i < lines.size(); i++) {
            graphics.centeredText(this.font, lines.get(i), centerX, y + 18 + i * 10, Palette.TEXT_DIM);
        }
    }

    private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int visible = Math.max(1, (listHeight - 4) / ROW);
        scroll = Math.max(0, Math.min(scroll, entries.size() - visible));
        graphics.enableScissor(listX + 1, listY + 1, listX + listWidth - 1, listY + listHeight - 1);
        for (int i = 0; i < visible && scroll + i < entries.size(); i++) {
            int index = scroll + i;
            BookFiles.Entry entry = entries.get(index);
            int y = listY + 2 + i * ROW;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ROW;
            if (index == selected) {
                graphics.fill(listX + 2, y, listX + listWidth - 2, y + ROW - 1, Palette.SURFACE_SELECTED);
                graphics.fill(listX + 2, y, listX + 4, y + ROW - 1, Palette.ACCENT);
            } else if (hovered) {
                graphics.fill(listX + 2, y, listX + listWidth - 2, y + ROW - 1, Palette.SURFACE_HOVER);
            }
            String name = this.font.plainSubstrByWidth(entry.name(), listWidth - 16);
            graphics.text(this.font, name, listX + 8, y + 2, index == selected ? Palette.TEXT_BRIGHT : Palette.TEXT, false);
            String meta = DATE.format(Instant.ofEpochMilli(entry.modified())) + "  ·  " + size(entry.size());
            graphics.text(this.font, meta, listX + 8, y + 12, Palette.TEXT_DIM, false);
        }
        graphics.disableScissor();
        if (entries.size() > visible) {
            int trackHeight = listHeight - 4;
            int thumb = Math.max(10, trackHeight * visible / entries.size());
            int thumbY = listY + 2 + (trackHeight - thumb) * scroll / Math.max(1, entries.size() - visible);
            graphics.fill(listX + listWidth - 4, thumbY, listX + listWidth - 2, thumbY + thumb, Palette.BORDER_STRONG);
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        int x = previewX + 6;
        int y = listY + 6;
        if (selected < 0) {
            return;
        }
        if (preview == null) {
            graphics.text(this.font, Component.translatable("qualityessentials.books.import.loading"), x, y, Palette.TEXT_DIM, false);
            return;
        }
        if (previewFailed) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("qualityessentials.books.import.failed", BookFiles.MAX_BYTES / 1024), previewWidth - 12);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(this.font, lines.get(i), x, y + i * 10, Palette.DANGER_TEXT, false);
            }
            return;
        }
        String clean = BookText.clean(preview, false);
        graphics.text(this.font, Component.translatable("qualityessentials.books.import.summary", clean.length(), previewPages), x, y,
                previewPages > BookText.MAX_PAGES ? Palette.WARNING : Palette.TEXT_DIM, false);
        y += 14;
        graphics.enableScissor(previewX + 1, listY + 1, previewX + previewWidth - 1, listY + listHeight - 1);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : FormatCodes.strip(clean, FormatCode.SECTION).split("\n", -1)) {
            if (lines.size() > (listHeight - 30) / 10) {
                break;
            }
            lines.addAll(this.font.split(Component.literal(line), previewWidth - 12));
        }
        for (int i = 0; i < lines.size() && y + i * 10 < listY + listHeight - 10; i++) {
            graphics.text(this.font, lines.get(i), x, y + i * 10, Palette.TEXT_BRIGHT, false);
        }
        graphics.disableScissor();
    }

    private static String size(long bytes) {
        return bytes < 1024 ? bytes + " B" : (bytes + 512) / 1024 + " KB";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (getChildAt(event.x(), event.y()).isPresent()) {
            return super.mouseClicked(event, doubleClick);
        }
        if (entries != null && !entries.isEmpty() && event.x() >= listX && event.x() < listX + listWidth && event.y() >= listY + 2 && event.y() < listY + listHeight) {
            int index = scroll + (int) ((event.y() - listY - 2) / ROW);
            if (index >= 0 && index < entries.size()) {
                if (index != selected) {
                    select(index);
                } else if (doubleClick) {
                    apply(false);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (entries != null && mouseX >= listX && mouseX < listX + listWidth) {
            scroll -= (int) Math.signum(vertical);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (entries != null && !entries.isEmpty() && (event.isUp() || event.isDown())) {
            int next = Math.max(0, Math.min(entries.size() - 1, selected + (event.isDown() ? 1 : -1)));
            if (next != selected) {
                select(next);
                int visible = Math.max(1, (listHeight - 4) / ROW);
                if (next < scroll) {
                    scroll = next;
                } else if (next >= scroll + visible) {
                    scroll = next - visible + 1;
                }
            }
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN && preview != null && !previewFailed) {
            apply(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
