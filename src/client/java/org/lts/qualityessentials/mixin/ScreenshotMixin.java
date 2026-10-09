package org.lts.qualityessentials.mixin;

import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.screenshot.ScreenshotActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @ModifyVariable(method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private static Consumer<Component> qualityessentials$actions(Consumer<Component> callback) {
        return ScreenshotActions.wrap(callback);
    }
}
