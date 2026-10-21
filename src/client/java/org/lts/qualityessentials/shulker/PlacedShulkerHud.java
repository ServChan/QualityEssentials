package org.lts.qualityessentials.shulker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.gui.Palette;

import java.util.ArrayList;
import java.util.List;

public final class PlacedShulkerHud {
    private static final Identifier TOOLTIP_BACKGROUND = Identifier.withDefaultNamespace("tooltip/background");
    private static final Identifier TOOLTIP_FRAME = Identifier.withDefaultNamespace("tooltip/frame");
    private static final int LINE = 10;
    private static final int PAD = 4;
    private static final int GAP = 16;

    private PlacedShulkerHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PlacedShulkers.enabled() || minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getDebugOverlay().showDebugScreen() || !minecraft.hasShiftDown()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Block block = minecraft.level.getBlockState(pos).getBlock();
        if (!(block instanceof ShulkerBoxBlock)) {
            return;
        }
        ShulkerMemory.Entry<PlacedShulkers.Contents> entry = PlacedShulkers.lookup(minecraft.level, pos);
        Font font = minecraft.font;
        Component title = entry != null && entry.contents().name() != null ? entry.contents().name() : block.getName();
        ShulkerTooltip grid = null;
        List<Component> lines = new ArrayList<>();
        if (entry == null) {
            lines.add(Component.translatable("qualityessentials.shulker.placed.unknown"));
            lines.add(Component.translatable("qualityessentials.shulker.placed.unknown_hint"));
        } else {
            List<ItemStack> items = entry.contents().items();
            if (items.stream().allMatch(ItemStack::isEmpty)) {
                lines.add(Component.translatable("qualityessentials.shulker.placed.empty"));
            } else {
                grid = ShulkerPreview.build(items, PlacedShulkers.SLOTS, true, ShulkerPreview.accent(block), QE.config().shulkerCompact);
            }
            lines.add(age(entry.seenAt()));
        }
        int width = font.width(title);
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int gridHeight = 0;
        if (grid != null) {
            width = Math.max(width, ClientShulkerTooltip.width(font, grid));
            gridHeight = ClientShulkerTooltip.height(grid) + 2;
        }
        width += PAD * 2;
        int height = PAD + LINE + 2 + gridHeight + lines.size() * LINE + PAD - 2;
        int center = graphics.guiWidth() / 2;
        int x = center + GAP;
        if (x + width + 9 > graphics.guiWidth() - 4) {
            x = Math.max(13, center - GAP - width);
        }
        int y = Math.max(13, Math.min(graphics.guiHeight() / 2 - height / 2, graphics.guiHeight() - height - 13));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_BACKGROUND, x - 9, y - 9, width + 18, height + 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_FRAME, x - 9, y - 9, width + 18, height + 18);
        int lineY = y + PAD;
        graphics.text(font, title, x + PAD, lineY, Palette.TEXT_BRIGHT, true);
        lineY += LINE + 2;
        if (grid != null) {
            ClientShulkerTooltip.draw(graphics, font, grid, x + PAD, lineY);
            lineY += gridHeight;
        }
        for (Component line : lines) {
            graphics.text(font, line, x + PAD, lineY, Palette.TEXT_DIM, true);
            lineY += LINE;
        }
    }

    private static Component age(long seenAt) {
        ShulkerMemory.Age age = ShulkerMemory.age(seenAt, System.currentTimeMillis());
        return switch (age.unit()) {
            case NOW -> Component.translatable("qualityessentials.shulker.placed.age.now");
            case MINUTES -> Component.translatable("qualityessentials.shulker.placed.age.minutes", age.value());
            case HOURS -> Component.translatable("qualityessentials.shulker.placed.age.hours", age.value());
            case DAYS -> Component.translatable("qualityessentials.shulker.placed.age.days", age.value());
        };
    }
}
