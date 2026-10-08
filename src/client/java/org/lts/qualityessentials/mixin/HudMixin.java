package org.lts.qualityessentials.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.lts.qualityessentials.effects.EffectTimerHud;
import org.lts.qualityessentials.food.FoodHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractEffects", at = @At("TAIL"))
    private void qualityessentials$effectTimers(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        EffectTimerHud.extract(graphics);
    }

    @Inject(method = "extractFood", at = @At("TAIL"))
    private void qualityessentials$foodOverlay(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        FoodHud.extract(graphics, player, top, right);
    }
}
