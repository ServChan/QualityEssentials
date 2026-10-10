package org.lts.qualityessentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.lts.qualityessentials.chat.ChatDecor;
import org.lts.qualityessentials.chat.ChatModule;
import org.lts.qualityessentials.chat.ChatTimes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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
        return message;
    }

    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> qualityessentials$decorate(GuiMessage message, Font font, int width,
            Operation<List<FormattedCharSequence>> original) {
        return original.call(ChatDecor.decorate(message), font, width);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("TAIL"))
    private void qualityessentials$hoverTimes(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
            ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        if (displayMode.foreground) {
            ChatTimes.extract(graphics, font, mouseX, mouseY, trimmedMessages, chatScrollbarPos, getLinesPerPage(), minecraft);
        }
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
