package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudInvoker {
    @Invoker("getCameraPlayer")
    Player qualityessentials$cameraPlayer();

    @Invoker("getPlayerVehicleWithHealth")
    LivingEntity qualityessentials$vehicleWithHealth();

    @Invoker("getVehicleMaxHearts")
    int qualityessentials$vehicleMaxHearts(LivingEntity vehicle);

    @Invoker("extractFood")
    void qualityessentials$extractFood(GuiGraphicsExtractor graphics, Player player, int top, int right);
}
