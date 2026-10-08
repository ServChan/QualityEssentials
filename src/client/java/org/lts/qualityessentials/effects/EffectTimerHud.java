package org.lts.qualityessentials.effects;

import com.google.common.collect.Ordering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.effect.MobEffectInstance;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;

import java.util.Collection;

public final class EffectTimerHud {
    private EffectTimerHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        if (!QE.active(Module.EFFECTS)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
        Screen screen = minecraft.gui.screen();
        if (effects.isEmpty() || (screen != null && screen.showsActiveEffects())) {
            return;
        }
        QEConfig config = QE.config();
        Font font = minecraft.font;
        int beneficial = 0;
        int harmful = 0;
        for (MobEffectInstance effect : Ordering.natural().reverse().sortedCopy(effects)) {
            if (!effect.showIcon()) {
                continue;
            }
            int x = graphics.guiWidth();
            int y = minecraft.isDemo() ? 16 : 1;
            if (effect.getEffect().value().isBeneficial()) {
                beneficial++;
                x -= 25 * beneficial;
            } else {
                harmful++;
                x -= 25 * harmful;
                y += 26;
            }
            if (config.effectsShowTimer) {
                String time = EffectFormat.duration(effect.getDuration(), effect.isInfiniteDuration());
                int color = EffectFormat.durationColor(effect.getDuration(), effect.isInfiniteDuration());
                graphics.text(font, time, x + 12 - font.width(time) / 2, y + 15, color, true);
            }
            if (config.effectsShowLevel) {
                String level = EffectFormat.level(effect.getAmplifier());
                if (!level.isEmpty()) {
                    graphics.text(font, level, x + 22 - font.width(level), y + 2, 0xFFFFFFFF, true);
                }
            }
        }
    }
}
