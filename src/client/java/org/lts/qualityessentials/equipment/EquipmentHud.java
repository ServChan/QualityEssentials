package org.lts.qualityessentials.equipment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentHud {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int ROW = 17;

    private EquipmentHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        if (!QE.active(Module.EQUIPMENT)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.gui.screen() != null) {
            return;
        }
        QEConfig config = QE.config();
        List<Entry> entries = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) {
            add(entries, player, player.getItemBySlot(slot), config, false);
        }
        add(entries, player, player.getMainHandItem(), config, true);
        add(entries, player, player.getOffhandItem(), config, true);
        if (config.equipmentShowAmmo && (usesArrows(player.getMainHandItem()) || usesArrows(player.getOffhandItem()))) {
            int arrows = countMatching(player.getInventory(), EquipmentHud::isArrow);
            entries.add(new Entry(new ItemStack(Items.ARROW), DurabilityFormat.count(arrows), arrows == 0 ? 0xFFFF5050 : 0xFFFFFFFF));
        }
        if (entries.isEmpty()) {
            return;
        }
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int x;
        int y;
        switch (config.anchor()) {
            case HOTBAR_RIGHT -> {
                x = width / 2 + 91 + 8;
                y = height - entries.size() * ROW - 2;
            }
            case TOP_LEFT -> {
                x = 4;
                y = 4;
            }
            default -> {
                x = 4;
                y = (height - entries.size() * ROW) / 2;
            }
        }
        for (Entry entry : entries) {
            graphics.item(entry.stack, x, y);
            graphics.text(font, entry.text, x + 19, y + 4, entry.color, true);
            y += ROW;
        }
    }

    private static void add(List<Entry> entries, Player player, ItemStack stack, QEConfig config, boolean hand) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.isDamageableItem()) {
            float fraction = DurabilityFormat.fraction(stack.getMaxDamage(), stack.getDamageValue());
            entries.add(new Entry(stack, DurabilityFormat.text(stack.getMaxDamage(), stack.getDamageValue(), config.equipmentShowPercent),
                    DurabilityFormat.color(fraction)));
            return;
        }
        if (hand && config.equipmentShowStackTotals && stack.getMaxStackSize() > 1) {
            int total = countMatching(player.getInventory(), other -> ItemStack.isSameItemSameComponents(other, stack));
            entries.add(new Entry(stack, DurabilityFormat.count(total), 0xFFFFFFFF));
        }
    }

    private static boolean usesArrows(ItemStack stack) {
        return stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
    }

    private static boolean isArrow(ItemStack stack) {
        return stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW);
    }

    private static int countMatching(Inventory inventory, java.util.function.Predicate<ItemStack> predicate) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && predicate.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private record Entry(ItemStack stack, String text, int color) {
    }
}
