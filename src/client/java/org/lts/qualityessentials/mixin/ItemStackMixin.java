package org.lts.qualityessentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lts.qualityessentials.hidden.ItemDetails;
import org.lts.qualityessentials.shulker.ShulkerPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyVariable(method = "getTooltipLines", at = @At("STORE"))
    private TooltipDisplay qualityessentials$revealHidden(TooltipDisplay display) {
        return ItemDetails.display(display);
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void qualityessentials$hiddenHint(Item.TooltipContext context, Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        ItemDetails.finish((ItemStack) (Object) this, context, player, flag, cir.getReturnValue());
    }

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void qualityessentials$containerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (cir.getReturnValue().isEmpty()) {
            Optional<TooltipComponent> preview = ShulkerPreview.create((ItemStack) (Object) this);
            if (preview.isPresent()) {
                cir.setReturnValue(preview);
            }
        }
    }
}
