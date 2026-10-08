package org.lts.qualityessentials;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.equipment.EquipmentHud;
import org.lts.qualityessentials.food.FoodHud;
import org.lts.qualityessentials.gui.HudEditorScreen;
import org.lts.qualityessentials.gui.SettingsScreen;
import org.lts.qualityessentials.stats.StatsHud;
import org.lts.qualityessentials.keyboard.KeyboardModule;
import org.lts.qualityessentials.lookat.LookAtHud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QualityEssentialsClient implements ClientModInitializer {
    public static final String MOD_ID = "qualityessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping settingsKey;
    private static KeyMapping editorKey;

    @Override
    public void onInitializeClient() {
        QE.load();
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.qualityessentials.settings", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));
        editorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.qualityessentials.hud_editor", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));
        KeyboardModule.init(CATEGORY);
        FoodHud.registerTooltip();
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.fromNamespaceAndPath(MOD_ID, "equipment"),
                (graphics, tickCounter) -> EquipmentHud.extract(graphics));
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath(MOD_ID, "look_at"),
                (graphics, tickCounter) -> LookAtHud.extract(graphics));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "stats"), (graphics, tickCounter) -> StatsHud.extract(graphics));
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            KeyboardModule.tick(minecraft);
            while (editorKey.consumeClick()) {
                if (minecraft.gui.screen() == null) {
                    minecraft.gui.setScreen(new HudEditorScreen(null, null));
                }
            }
            while (settingsKey.consumeClick()) {
                if (minecraft.gui.screen() == null) {
                    minecraft.gui.setScreen(new SettingsScreen(null));
                }
            }
        });
        StringBuilder blocked = new StringBuilder();
        for (Module module : Module.values()) {
            String blocker = QE.blocker(module);
            if (blocker != null) {
                blocked.append(' ').append(module.id()).append("<-").append(blocker);
            }
        }
        LOGGER.info("QualityEssentials initialized{}", blocked.isEmpty() ? "" : "; yielding to:" + blocked);
    }

    public static Screen createKeyboardScreen(Screen parent) {
        return KeyboardModule.createScreen(parent);
    }

    public static boolean keyboardActive() {
        return QE.active(Module.KEYBOARD);
    }
}
