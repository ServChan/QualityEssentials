package org.lts.qualityessentials.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lts.qualityessentials.chat.ChatModule;
import org.lts.qualityessentials.chat.ChatOverlay;
import org.lts.qualityessentials.chat.ChatView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void qualityessentials$open(CallbackInfo ci) {
        ChatView.onOpen(input);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void qualityessentials$close(CallbackInfo ci) {
        ChatOverlay.end();
        if (input != null) {
            ChatView.onClose(input);
        }
    }

    @ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true)
    private String qualityessentials$prepare(String message) {
        return ChatModule.prepareSend(ChatModule.fixInput(message));
    }

    @WrapOperation(method = "handleChatInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V"))
    private void qualityessentials$rememberWhole(ChatComponent chat, String message, Operation<Void> original) {
        original.call(chat, ChatModule.wholeMessage(message));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void qualityessentials$keys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input != null && (ChatView.keyPressed(input, event) || !ChatView.searching() && ChatModule.flip(input, event))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void qualityessentials$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Screen screen = (Screen) (Object) this;
        if (input != null && ChatView.mouseClicked(event, screen.width, screen.height, input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/CommandSuggestions;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void qualityessentials$row(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ChatView.extractRow(graphics, screen.width, screen.height, mouseX, mouseY, input);
    }
}
