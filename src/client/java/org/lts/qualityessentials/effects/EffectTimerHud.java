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
    private static final int BACKING = 0xB0000000;

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
            overlay(graphics, minecraft.font, x, y, effect.getDuration(), effect.isInfiniteDuration(), effect.getAmplifier(), QE.config());
        }
    }

    public static void overlay(GuiGraphicsExtractor graphics, Font font, int x, int y, int duration, boolean infinite, int amplifier, QEConfig config) {
        if (config.effectsShowTimer) {
            String time = EffectFormat.duration(duration, infinite);
            int width = font.width(time);
            int tx = x + 12 - width / 2;
            graphics.fill(tx - 1, y + 15, tx + width, y + 23, BACKING);
            graphics.text(font, time, tx, y + 15, EffectFormat.durationColor(duration, infinite), false);
        }
        if (config.effectsShowLevel) {
            String level = EffectFormat.level(amplifier);
            if (!level.isEmpty()) {
                int width = font.width(level);
                int lx = x + 23 - width;
                graphics.fill(lx - 1, y + 1, lx + width, y + 9, BACKING);
                graphics.text(font, level, lx, y + 1, 0xFFFFE066, false);
            }
        }
    }
}
