package org.lts.qualityessentials.shulker;

import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.Palette;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShulkerPreview {
    private static final int UNDYED = 0xFF9A6E9A;

    private ShulkerPreview() {
    }

    public static void register() {
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerTooltip tooltip ? new ClientShulkerTooltip(tooltip) : null);
    }

    public static boolean hidesVanillaList(ItemContainerContents contents) {
        return QE.active(Module.SHULKER) && QE.config().shulkerHideText && contents.nonEmptyItemCopyStream().findAny().isPresent();
    }

    public static Component shiftHint(ItemContainerContents contents) {
        if (!QE.active(Module.SHULKER) || Minecraft.getInstance().hasShiftDown()) {
            return null;
        }
        int total = contents.nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum();
        if (total <= 0) {
            return null;
        }
        return Component.translatable("qualityessentials.shulker.hold_shift", total).withStyle(ChatFormatting.GRAY);
    }

    public static Optional<TooltipComponent> create(ItemStack stack) {
        if (!QE.active(Module.SHULKER) || !Minecraft.getInstance().hasShiftDown()) {
            return Optional.empty();
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || contents.nonEmptyItemCopyStream().findAny().isEmpty()) {
            return Optional.empty();
        }
        Block block = Block.byItem(stack.getItem());
        boolean shulkerBox = block instanceof ShulkerBoxBlock;
        int accent = Palette.BORDER_STRONG;
        if (block instanceof ShulkerBoxBlock box) {
            DyeColor color = box.getColor();
            accent = color == null ? UNDYED : 0xFF000000 | color.getTextureDiffuseColor();
        }
        return Optional.of(build(contents.itemCopies().toList(), contents.size(), shulkerBox, accent, QE.config().shulkerCompact));
    }

    public static ShulkerTooltip sample(QEConfig config) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(Items.DIAMOND, 24));
        items.add(new ItemStack(Items.IRON_INGOT, 64));
        items.add(new ItemStack(Items.IRON_INGOT, 64));
        items.add(new ItemStack(Items.OAK_LOG, 48));
        items.add(ItemStack.EMPTY);
        items.add(new ItemStack(Items.GOLDEN_CARROT, 32));
        items.add(new ItemStack(Items.ENDER_PEARL, 16));
        items.add(ItemStack.EMPTY);
        items.add(new ItemStack(Items.IRON_INGOT, 37));
        items.add(new ItemStack(Items.REDSTONE, 64));
        items.add(new ItemStack(Items.TORCH, 64));
        items.add(new ItemStack(Items.DIAMOND_PICKAXE));
        return build(items, 27, true, 0xFF000000 | DyeColor.LIGHT_BLUE.getTextureDiffuseColor(), config.shulkerCompact);
    }

    static ShulkerTooltip build(List<ItemStack> raw, int declaredSize, boolean shulkerBox, int accent, boolean summary) {
        int total = 0;
        for (ItemStack stack : raw) {
            total += stack.getCount();
        }
        if (summary) {
            List<ItemStack> filled = raw.stream().filter(stack -> !stack.isEmpty()).toList();
            List<ShulkerContents.Group<ItemStack>> groups = ShulkerContents.merge(filled, ItemStack::getCount, ItemStack::isSameItemSameComponents);
            List<ItemStack> slots = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (ShulkerContents.Group<ItemStack> group : groups) {
                slots.add(group.item.copyWithCount(1));
                counts.add(group.count);
            }
            return new ShulkerTooltip(slots, counts, Math.min(ShulkerContents.COLUMNS, Math.max(1, slots.size())), accent, true, total);
        }
        int lastFilled = -1;
        for (int i = 0; i < raw.size(); i++) {
            if (!raw.get(i).isEmpty()) {
                lastFilled = i;
            }
        }
        int size = ShulkerContents.gridSize(declaredSize, lastFilled, shulkerBox);
        List<ItemStack> slots = new ArrayList<>(size);
        List<Integer> counts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = i < raw.size() ? raw.get(i) : ItemStack.EMPTY;
            slots.add(stack);
            counts.add(stack.getCount());
        }
        return new ShulkerTooltip(slots, counts, ShulkerContents.COLUMNS, accent, false, total);
    }
}
