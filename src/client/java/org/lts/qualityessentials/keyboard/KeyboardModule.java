package org.lts.qualityessentials.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.gui.FlatButton;
import org.lts.qualityessentials.keyboard.gui.KeyboardScreen;

public final class KeyboardModule {
    public static KeyMapping openKey;

    private KeyboardModule() {
    }

    public static void init(KeyMapping.Category category) {
        ModifierRegistry.register(LitematicaPlusModifiers.tryCreate());
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.qualityessentials.keyboard", InputConstants.Type.KEYBOARD, InputConstants.KEY_F10, category));
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (screen instanceof KeyBindsScreen && QE.active(Module.KEYBOARD) && QE.config().keyboardControlsButton) {
                Component label = Component.translatable("qualityessentials.keyboard.button.open_short");
                int buttonWidth = Math.max(104, minecraft.font.width(label) + 16);
                Screens.getWidgets(screen).add(new FlatButton(width - buttonWidth - 6, 6, buttonWidth, 18, label,
                        FlatButton.Style.SECONDARY, () -> minecraft.gui.setScreen(new KeyboardScreen(screen))));
            }
        });
    }

    public static void tick(Minecraft minecraft) {
        while (openKey.consumeClick()) {
            if (QE.active(Module.KEYBOARD) && minecraft.gui.screen() == null) {
                minecraft.gui.setScreen(new KeyboardScreen(null));
            }
        }
    }

    public static Screen createScreen(Screen parent) {
        return new KeyboardScreen(parent);
    }
}
