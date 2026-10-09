package org.lts.qualityessentials.newitems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.gui.Ui;

public final class NewItemTracker {
    private static final int SLOTS = 36;
    private static final int SETTLE_TICKS = 40;
    private static final int GOLD = 0xFFFFD54F;
    private static final int CORE = 0xFFFFFFFF;
    private static final int SHADOW = 0x90000000;
    private static final int[][] STAR = {
            {2, 0}, {1, 1}, {2, 1}, {3, 1}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {1, 3}, {2, 3}, {3, 3}, {2, 4}
    };
    private static final int[][] STAR_CORE = {{2, 1}, {1, 2}, {2, 2}, {3, 2}, {2, 3}};

    private static final FreshSlots<Item> SLOTS_STATE = new FreshSlots<>(SLOTS);
    private static final Item[] KEYS = new Item[SLOTS];
    private static final int[] COUNTS = new int[SLOTS];
    private static Player lastPlayer;
    private static int settle;

    private NewItemTracker() {
    }

    public static void tick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            SLOTS_STATE.clear();
            lastPlayer = null;
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            KEYS[i] = stack.isEmpty() ? null : stack.getItem();
            COUNTS[i] = stack.getCount();
        }
        if (player != lastPlayer) {
            lastPlayer = player;
            settle = SETTLE_TICKS;
        }
        if (settle > 0) {
            settle--;
            SLOTS_STATE.snapshot(KEYS, COUNTS);
            return;
        }
        boolean canMark = QE.active(Module.NEW_ITEMS) && !(minecraft.gui.screen() instanceof AbstractContainerScreen<?>);
        SLOTS_STATE.update(KEYS, COUNTS, canMark);
    }

    public static void drawSlot(GuiGraphicsExtractor graphics, Slot slot, boolean hovered) {
        if (!(slot.container instanceof Inventory)) {
            return;
        }
        int index = slot.getContainerSlot();
        if (!SLOTS_STATE.isFresh(index)) {
            return;
        }
        if (hovered || !QE.active(Module.NEW_ITEMS)) {
            SLOTS_STATE.seen(index);
            return;
        }
        float alpha = QE.config().newItemsAnimate ? 0.7f + 0.3f * Ui.pulse(Util.getMillis(), 1400f) : 1f;
        star(graphics, slot.x, slot.y, alpha);
    }

    public static void star(GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        for (int[] p : STAR) {
            graphics.fill(x + p[0] + 1, y + p[1] + 1, x + p[0] + 2, y + p[1] + 2, SHADOW);
        }
        int gold = Ui.withAlpha(GOLD, alpha);
        for (int[] p : STAR) {
            graphics.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, gold);
        }
        int core = Ui.withAlpha(CORE, alpha);
        for (int[] p : STAR_CORE) {
            graphics.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, core);
        }
    }
}
