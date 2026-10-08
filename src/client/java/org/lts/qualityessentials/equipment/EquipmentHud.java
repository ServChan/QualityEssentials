package org.lts.qualityessentials.equipment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.Ui;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentHud {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int ROW = 18;
    private static final int BAR = 24;
    private static final int BACKING = 0xA0000000;

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
        List<Entry> entries = collect(player, config);
        if (entries.isEmpty()) {
            return;
        }
        org.lts.qualityessentials.core.HudLayout layout = config.layout(org.lts.qualityessentials.core.HudElement.EQUIPMENT);
        boolean horizontal = config.equipmentHorizontal;
        int[] size = size(minecraft.font, entries, horizontal);
        int shift = 0;
        if (horizontal && !player.getOffhandItem().isEmpty() && player.getMainArm() == HumanoidArm.LEFT
                && layout.anchor() == org.lts.qualityessentials.core.HudLayout.Anchor.BOTTOM_CENTER && layout.alignX < 0) {
            shift = 29;
        }
        org.lts.qualityessentials.core.HudLayout placed = layout;
        if (shift > 0) {
            placed = layout.copy();
            placed.offsetX += shift;
        }
        org.lts.qualityessentials.core.HudPlacement.draw(graphics, placed, size[0], size[1], 0,
                g -> draw(g, minecraft.font, entries, horizontal, layout.opacity));
    }

    public static int[] size(Font font, List<Entry> entries, boolean horizontal) {
        if (horizontal) {
            int width = 0;
            for (Entry entry : entries) {
                width += Math.max(18, font.width(entry.text) + 4) + 2;
            }
            return new int[]{width + 2, 33};
        }
        int text = BAR;
        for (Entry entry : entries) {
            text = Math.max(text, font.width(entry.text));
        }
        return new int[]{4 + 19 + text + 4, entries.size() * ROW + 2};
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, List<Entry> entries, boolean horizontal, float opacity) {
        int[] size = size(font, entries, horizontal);
        if (opacity > 0.01f) {
            graphics.fill(0, 0, size[0], size[1], Ui.withAlpha(0x0B1119, 0.82f * opacity));
        }
        if (horizontal) {
            drawRow(graphics, font, entries, 2, 13);
        } else {
            drawColumn(graphics, font, entries, 3, 1);
        }
    }

    public static List<Entry> collect(Player player, QEConfig config) {
        List<Entry> entries = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) {
            add(entries, player, player.getItemBySlot(slot), config, false);
        }
        add(entries, player, player.getMainHandItem(), config, true);
        add(entries, player, player.getOffhandItem(), config, true);
        if (config.equipmentShowAmmo && (usesArrows(player.getMainHandItem()) || usesArrows(player.getOffhandItem()))) {
            int arrows = countMatching(player.getInventory(), EquipmentHud::isArrow);
            entries.add(new Entry(new ItemStack(Items.ARROW), DurabilityFormat.count(arrows), arrows == 0 ? 0xFFFF5050 : Palette.TEXT_BRIGHT, -1f));
        }
        return entries;
    }

    public static List<Entry> preview(QEConfig config) {
        List<Entry> entries = new ArrayList<>();
        entries.add(sample(new ItemStack(Items.DIAMOND_HELMET), 363, 12, config));
        entries.add(sample(new ItemStack(Items.NETHERITE_CHESTPLATE), 592, 380, config));
        entries.add(sample(new ItemStack(Items.ELYTRA), 432, 400, config));
        entries.add(sample(new ItemStack(Items.DIAMOND_PICKAXE), 1561, 61, config));
        if (config.equipmentShowStackTotals) {
            entries.add(new Entry(new ItemStack(Items.STONE_BRICKS), DurabilityFormat.count(448), Palette.TEXT_BRIGHT, -1f));
        }
        if (config.equipmentShowAmmo) {
            entries.add(new Entry(new ItemStack(Items.ARROW), "64", Palette.TEXT_BRIGHT, -1f));
        }
        return entries;
    }

    private static Entry sample(ItemStack stack, int max, int damage, QEConfig config) {
        float fraction = DurabilityFormat.fraction(max, damage);
        return new Entry(stack, DurabilityFormat.text(max, damage, config.equipmentShowPercent), DurabilityFormat.color(fraction), fraction);
    }

    public static void drawColumn(GuiGraphicsExtractor graphics, Font font, List<Entry> entries, int x, int y) {
        for (Entry entry : entries) {
            warn(graphics, entry, x, y);
            graphics.item(entry.stack, x, y);
            int textY = entry.fraction >= 0 ? y + 2 : y + 5;
            graphics.text(font, entry.text, x + 19, textY, entry.color, true);
            if (entry.fraction >= 0) {
                Ui.bar(graphics, x + 19, y + 12, BAR, 2, entry.fraction, entry.color, Palette.HUD_TRACK);
            }
            y += ROW;
        }
    }

    public static void drawRow(GuiGraphicsExtractor graphics, Font font, List<Entry> entries, int x, int iconY) {
        for (Entry entry : entries) {
            int cell = Math.max(18, font.width(entry.text) + 4);
            int iconX = x + (cell - 16) / 2;
            warn(graphics, entry, iconX, iconY);
            graphics.item(entry.stack, iconX, iconY);
            int textWidth = font.width(entry.text);
            int textX = x + (cell - textWidth) / 2;
            graphics.fill(textX - 2, iconY - 11, textX + textWidth + 1, iconY - 2, BACKING);
            graphics.text(font, entry.text, textX, iconY - 10, entry.color, false);
            if (entry.fraction >= 0) {
                Ui.bar(graphics, iconX, iconY + 17, 16, 2, entry.fraction, entry.color, Palette.HUD_TRACK);
            }
            x += cell + 2;
        }
    }

    private static void warn(GuiGraphicsExtractor graphics, Entry entry, int x, int y) {
        if (entry.fraction >= 0 && entry.fraction <= 0.1f) {
            float alpha = 0.25f + 0.45f * Ui.pulse(Util.getMillis(), 900f);
            graphics.fill(x - 1, y - 1, x + 17, y + 17, Ui.withAlpha(0xFF3030, alpha));
        }
    }

    private static void add(List<Entry> entries, Player player, ItemStack stack, QEConfig config, boolean hand) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.isDamageableItem()) {
            float fraction = DurabilityFormat.fraction(stack.getMaxDamage(), stack.getDamageValue());
            entries.add(new Entry(stack, DurabilityFormat.text(stack.getMaxDamage(), stack.getDamageValue(), config.equipmentShowPercent),
                    DurabilityFormat.color(fraction), fraction));
            return;
        }
        if (hand && config.equipmentShowStackTotals && stack.getMaxStackSize() > 1) {
            int total = countMatching(player.getInventory(), other -> ItemStack.isSameItemSameComponents(other, stack));
            entries.add(new Entry(stack, DurabilityFormat.count(total), Palette.TEXT_BRIGHT, -1f));
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

    public record Entry(ItemStack stack, String text, int color, float fraction) {
    }
}
