package org.lts.qualityessentials.mixin;

import net.minecraft.client.Minecraft;
import org.lts.qualityessentials.signs.SignModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftPickMixin {
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"))
    private void qualityessentials$rememberSign(CallbackInfo ci) {
        SignModule.onPick((Minecraft) (Object) this);
    }
}
