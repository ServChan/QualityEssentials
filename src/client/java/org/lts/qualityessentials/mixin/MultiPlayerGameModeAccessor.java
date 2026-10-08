package org.lts.qualityessentials.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float qualityessentials$destroyProgress();

    @Accessor("destroyBlockPos")
    BlockPos qualityessentials$destroyBlockPos();
}
