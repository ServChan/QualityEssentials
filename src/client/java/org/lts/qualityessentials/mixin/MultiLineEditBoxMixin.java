package org.lts.qualityessentials.mixin;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lts.qualityessentials.books.RichPage;
import org.lts.qualityessentials.books.RichPages;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin extends AbstractTextAreaWidget implements RichPage {
    @Unique
    private static final int RESET_MARK = 0xFF8A7A62;

    @Shadow
    @Final
    private Font font;
    @Shadow
    @Final
    private MultilineTextField textField;
    @Shadow
    @Final
    private int textColor;
    @Shadow
    @Final
    private boolean textShadow;
    @Shadow
    @Final
    private int cursorColor;
    @Shadow
    private long focusedTime;
    @Shadow
    private IMEPreeditOverlay preeditOverlay;

    @Unique
    private boolean qualityessentials$rich;
    @Unique
    private boolean qualityessentials$marks;

    private MultiLineEditBoxMixin(int x, int y, int width, int height, Component narration, AbstractScrollArea.ScrollbarSettings settings) {
        super(x, y, width, height, narration, settings);
    }

    @Shadow
    public abstract boolean capturesInput();

    @Override
    public void qualityessentials$setRich(boolean rich, boolean marks) {
        this.qualityessentials$rich = rich;
        this.qualityessentials$marks = marks;
    }

    @Override
    public boolean qualityessentials$rich() {
        return qualityessentials$rich;
    }

    @Override
    public MultilineTextField qualityessentials$field() {
        return textField;
    }

    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void qualityessentials$extractRich(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!qualityessentials$rich) {
            return;
        }
        ci.cancel();
        MultilineTextFieldAccessor field = (MultilineTextFieldAccessor) textField;
        String value = textField.value();
        int left = getInnerLeft();
        int top = getInnerTop();
        int cursor = Math.min(textField.cursor(), value.length());
        int select = Math.min(field.qualityessentials$selectCursor(), value.length());
        int selectFrom = Math.min(cursor, select);
        int selectTo = Math.max(cursor, select);
        boolean showCursor = isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - focusedTime);
        List<int[]> lines = RichPages.lines(font, value, field.qualityessentials$width());
        int cursorLine = RichPages.lineOf(lines, cursor);
        int cursorX = left;
        int cursorY = top;
        for (int i = 0; i < lines.size(); i++) {
            int begin = lines.get(i)[0];
            int end = lines.get(i)[1];
            int y = top + i * 9;
            String prefix = FormatCodes.active(value, begin, FormatCode.SECTION);
            if (withinContentAreaTopBottom(y, y + 9)) {
                graphics.text(font, prefix + value.substring(begin, end), left, y, textColor, textShadow);
                if (qualityessentials$marks) {
                    qualityessentials$drawMarks(graphics, value, prefix, begin, end, left, y);
                }
                if (selectFrom != selectTo && selectFrom <= end && selectTo >= begin) {
                    int x1 = left + font.width(prefix + value.substring(begin, Math.max(selectFrom, begin)));
                    int x2 = selectTo > end ? getX() + this.width - innerPadding() : left + font.width(prefix + value.substring(begin, selectTo));
                    if (x2 > x1) {
                        graphics.textHighlight(x1, y, x2, y + 9, true);
                    }
                }
            }
            if (i == cursorLine) {
                cursorX = left + font.width(prefix + value.substring(begin, Math.max(begin, Math.min(cursor, end))));
                cursorY = y;
            }
        }
        if (showCursor && withinContentAreaTopBottom(cursorY, cursorY + 9)) {
            if (cursor < value.length()) {
                TextCursorUtils.extractInsertCursor(graphics, cursorX, cursorY, cursorColor, 10);
            } else {
                TextCursorUtils.extractAppendCursor(graphics, font, cursorX, cursorY, cursorColor, textShadow);
            }
        }
        if (isHovered()) {
            graphics.requestCursor(CursorTypes.IBEAM);
        }
        if (preeditOverlay == null) {
            if (capturesInput()) {
                Minecraft.getInstance().textInputManager().setTextInputArea(cursorX, cursorY, cursorX + 1, cursorY + 10);
            }
        } else {
            preeditOverlay.updateInputPosition(cursorX, cursorY);
            graphics.setPreeditOverlay(preeditOverlay);
        }
    }

    @Unique
    private void qualityessentials$drawMarks(GuiGraphicsExtractor graphics, String value, String prefix, int begin, int end, int left, int y) {
        for (int j = begin; j < end; j++) {
            if (!FormatCodes.isCode(value, j, FormatCode.SECTION)) {
                continue;
            }
            FormatCode code = FormatCode.byCode(value.charAt(j + 1));
            int x = left + font.width(prefix + value.substring(begin, j));
            int color = code.isColor() ? 0xFF000000 | code.rgb() : RESET_MARK;
            graphics.fill(x - 1, y - 1, x, y + 2, color);
            j++;
        }
    }
}
