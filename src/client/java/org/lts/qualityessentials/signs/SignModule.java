package org.lts.qualityessentials.signs;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;

import java.util.List;

public final class SignModule {
    private static String[] remembered;
    private static int rememberedConnection;

    private SignModule() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (QE.active(Module.SIGNS) && screen instanceof AbstractSignEditScreen sign) {
                SignEditor.attach(sign, minecraft, width);
            }
        });
    }

    public static void onPick(Minecraft minecraft) {
        if (!QE.active(Module.SIGNS) || !QE.config().signsRemember || minecraft.gui.screen() != null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof SignBlockEntity sign)) {
            return;
        }
        SignText text = sign.getText(sign.getSlotPlayerIsFacing(minecraft.player));
        List<Component> messages = text.getMessages(minecraft.isTextFilteringEnabled());
        String[] lines = new String[SignLines.LINES];
        for (int i = 0; i < SignLines.LINES; i++) {
            lines[i] = i < messages.size() ? messages.get(i).getString() : "";
        }
        if (SignLines.blank(lines)) {
            return;
        }
        remember(lines);
        minecraft.gui.hud.setOverlayMessage(Component.translatable("qualityessentials.signs.remembered", preview(lines)), false);
    }

    static String preview(String[] lines) {
        for (String line : lines) {
            if (!line.isBlank()) {
                return line.strip();
            }
        }
        return "";
    }

    static String[] remembered() {
        if (remembered != null && rememberedConnection != connection()) {
            remembered = null;
        }
        return remembered == null ? null : remembered.clone();
    }

    static void remember(String[] lines) {
        remembered = lines.clone();
        rememberedConnection = connection();
    }

    private static int connection() {
        return System.identityHashCode(Minecraft.getInstance().getConnection());
    }

    static void forget() {
        remembered = null;
    }
}
