package org.lts.qualityessentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.lts.qualityessentials.chat.ChatDecor;
import org.lts.qualityessentials.chat.ChatModule;
import org.lts.qualityessentials.chat.ChatOverlay;
import org.lts.qualityessentials.chat.ChatView;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public abstract int getLinesPerPage();

    @ModifyExpressionValue(method = {"addMessageToDisplayQueue", "addMessageToQueue", "addRecentChat"},
            at = @At(value = "CONSTANT", args = "intValue=100"))
    private int qualityessentials$historyLimit(int original) {
        return ChatModule.historyLimit(original);
    }

    @ModifyVariable(method = "addMessage", at = @At("STORE"))
    private GuiMessage qualityessentials$register(GuiMessage message) {
        ChatDecor.register(message, allMessages, trimmedMessages);
        ChatView.onAdded(message, chatScrollbarPos, minecraft.gui.screen() instanceof ChatScreen);
        return message;
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void qualityessentials$filter(GuiMessage message, CallbackInfo ci) {
        if (!ChatView.visible(message)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> qualityessentials$decorate(GuiMessage message, Font font, int width,
            Operation<List<FormattedCharSequence>> original) {
        return original.call(ChatDecor.decorate(message), font, width);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
    private void qualityessentials$beginFrame(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
            ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        ChatOverlay.begin(graphics, font, mouseX, mouseY, displayMode, trimmedMessages, chatScrollbarPos, getLinesPerPage(), minecraft);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("TAIL"))
    private void qualityessentials$times(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
            ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        ChatOverlay.extract(graphics, font, displayMode, trimmedMessages, chatScrollbarPos, getLinesPerPage(), minecraft);
    }

    @WrapOperation(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;updatePose(Ljava/util/function/Consumer;)V"))
    private void qualityessentials$slide(ChatComponent.ChatGraphicsAccess access, Consumer<Matrix3x2f> updater,
            Operation<Void> original, @Local(argsOnly = true) ChatComponent.DisplayMode displayMode) {
        int shift = ChatOverlay.shift(displayMode);
        original.call(access, shift == 0 ? updater : updater.andThen(pose -> pose.translate(shift, 0f)));
    }

    @Inject(method = "getWidth(D)I", at = @At("RETURN"), cancellable = true)
    private static void qualityessentials$width(double pct, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ChatModule.width(cir.getReturnValue(), pct));
    }

    @Inject(method = "getHeight(D)I", at = @At("RETURN"), cancellable = true)
    private static void qualityessentials$height(double pct, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ChatModule.height(cir.getReturnValue(), pct));
    }
}
