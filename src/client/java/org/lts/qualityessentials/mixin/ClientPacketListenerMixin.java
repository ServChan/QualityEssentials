package org.lts.qualityessentials.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.lts.qualityessentials.stats.StatsCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void qualityessentials$sampleServerTicks(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            StatsCollector.onTimePacket(packet.gameTime());
        }
    }
}
