package org.lts.qualityessentials.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.lts.qualityessentials.chat.ChatSessions;
import org.lts.qualityessentials.effects.EffectTimerHud;
import org.lts.qualityessentials.food.FoodHud;
import org.lts.qualityessentials.mount.MountHud;
import org.lts.qualityessentials.shulker.PlacedShulkers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Unique
    private boolean qualityessentials$vehicleShifted;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void qualityessentials$saveChat(CallbackInfo ci) {
        ChatSessions.onDisconnected(Minecraft.getInstance());
        PlacedShulkers.onDisconnected();
    }

    @Inject(method = "extractEffects", at = @At("TAIL"))
    private void qualityessentials$effectTimers(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        EffectTimerHud.extract(graphics);
    }

    @Inject(method = "extractFood", at = @At("TAIL"))
    private void qualityessentials$foodOverlay(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        FoodHud.extract(graphics, player, top, right);
    }

    @Inject(method = "extractVehicleHealth", at = @At("HEAD"))
    private void qualityessentials$playerFoodWhileRiding(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        qualityessentials$vehicleShifted = false;
        if (!qualityessentials$ridingWithFood()) {
            return;
        }
        HudInvoker hud = (HudInvoker) this;
        Player player = hud.qualityessentials$cameraPlayer();
        if (player == null) {
            return;
        }
        hud.qualityessentials$extractFood(graphics, player, graphics.guiHeight() - 39, graphics.guiWidth() / 2 + 91);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, -10f);
        qualityessentials$vehicleShifted = true;
    }

    @Inject(method = "extractVehicleHealth", at = @At("RETURN"))
    private void qualityessentials$restoreVehicleRow(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (qualityessentials$vehicleShifted) {
            graphics.pose().popMatrix();
            qualityessentials$vehicleShifted = false;
        }
    }

    @ModifyArg(method = "extractPlayerHealth", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractAirBubbles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;III)V"),
            index = 3)
    private int qualityessentials$airAboveShiftedVehicle(int top) {
        return qualityessentials$ridingWithFood() ? top - 10 : top;
    }

    @Unique
    private boolean qualityessentials$ridingWithFood() {
        if (!MountHud.showPlayerFood()) {
            return false;
        }
        HudInvoker hud = (HudInvoker) this;
        LivingEntity vehicle = hud.qualityessentials$vehicleWithHealth();
        return vehicle != null && hud.qualityessentials$vehicleMaxHearts(vehicle) > 0;
    }
}
