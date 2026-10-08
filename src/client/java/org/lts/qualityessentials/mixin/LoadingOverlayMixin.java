package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int qualityessentials$darkBackground(IntSupplier original) {
        if (QE.active(Module.DARK_SCREEN)) {
            return 0xFF000000 | QE.config().darkPreset().rgb();
        }
        return original.getAsInt();
    }
}
