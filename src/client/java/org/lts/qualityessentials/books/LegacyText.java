package org.lts.qualityessentials.books;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lts.qualityessentials.text.FormatCode;

import java.util.Optional;

public final class LegacyText {
    private LegacyText() {
    }

    public static String of(Component component) {
        StringBuilder out = new StringBuilder();
        String[] previous = {""};
        component.visit((style, text) -> {
            if (text.isEmpty()) {
                return Optional.empty();
            }
            String codes = codes(style);
            if (!codes.equals(previous[0])) {
                boolean colorFirst = !codes.isEmpty() && FormatCode.byCode(codes.charAt(1)).isColor();
                if (!colorFirst && !previous[0].isEmpty()) {
                    out.append(FormatCode.RESET.with(FormatCode.SECTION));
                }
                out.append(codes);
                previous[0] = codes;
            }
            out.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static String codes(Style style) {
        StringBuilder codes = new StringBuilder();
        if (style.getColor() != null) {
            codes.append(FormatCode.nearestColor(style.getColor().getValue()).with(FormatCode.SECTION));
        }
        if (style.isObfuscated()) {
            codes.append(FormatCode.OBFUSCATED.with(FormatCode.SECTION));
        }
        if (style.isBold()) {
            codes.append(FormatCode.BOLD.with(FormatCode.SECTION));
        }
        if (style.isStrikethrough()) {
            codes.append(FormatCode.STRIKETHROUGH.with(FormatCode.SECTION));
        }
        if (style.isUnderlined()) {
            codes.append(FormatCode.UNDERLINE.with(FormatCode.SECTION));
        }
        if (style.isItalic()) {
            codes.append(FormatCode.ITALIC.with(FormatCode.SECTION));
        }
        return codes.toString();
    }
}
