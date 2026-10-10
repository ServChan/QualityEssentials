package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lts.qualityessentials.chat.ChatModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true)
    private String qualityessentials$fixLayout(String message) {
        return ChatModule.fixInput(message);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void qualityessentials$flipLayout(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input != null && ChatModule.flip(input, event)) {
            cir.setReturnValue(true);
        }
    }
}
