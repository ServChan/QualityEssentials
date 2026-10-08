package org.lts.qualityessentials.lookat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.mixin.MultiPlayerGameModeAccessor;

import java.util.ArrayList;
import java.util.List;

public final class LookAtHud {
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("hud/armor_full");
    private static final int BACKGROUND = 0xD00D141F;
    private static final int BORDER = 0xFF2E435E;
    private static final int ACCENT = 0xFF00E5FF;
    private static final int MOD_COLOR = 0xFF6F8CFF;

    private LookAtHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        if (!QE.active(Module.LOOK_AT)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null
                || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return;
        }
        Info info = hit instanceof EntityHitResult entityHit ? entity(entityHit.getEntity())
                : hit instanceof BlockHitResult blockHit ? block(minecraft, player, blockHit.getBlockPos()) : null;
        if (info == null) {
            return;
        }
        draw(graphics, minecraft.font, info);
    }

    private static Info block(Minecraft minecraft, Player player, BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        QEConfig config = QE.config();
        Info info = new Info(state.getBlock().getName(), new ItemStack(state.getBlock().asItem()));
        if (config.lookAtShowStates) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty integer && LookAtFormat.shows(property.getName())) {
                    List<Integer> values = integer.getPossibleValues();
                    int min = values.getFirst();
                    int max = values.getLast();
                    int value = state.getValue(integer);
                    if ("age".equals(property.getName())) {
                        info.lines.add(value >= max
                                ? Component.translatable("qualityessentials.lookat.grown").withStyle(ChatFormatting.GREEN)
                                : Component.translatable("qualityessentials.lookat.growth", LookAtFormat.percent(value, min, max))
                                        .withStyle(ChatFormatting.GRAY));
                    } else {
                        info.lines.add(Component.translatable("qualityessentials.lookat.state." + property.getName(), value, max)
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
        if (config.lookAtShowHarvest && state.requiresCorrectToolForDrops()) {
            boolean ok = player.hasCorrectToolForDrops(state);
            info.lines.add(Component.translatable("qualityessentials.lookat.tool", Component.translatable(toolKey(state)))
                    .append(" ")
                    .append(Component.translatable(ok ? "qualityessentials.lookat.tool.ok" : "qualityessentials.lookat.tool.missing"))
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (config.lookAtShowProgress && minecraft.gameMode instanceof MultiPlayerGameModeAccessor accessor
                && minecraft.gameMode.isDestroying() && pos.equals(accessor.qualityessentials$destroyBlockPos())) {
            info.progress = accessor.qualityessentials$destroyProgress();
        }
        info.modId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        return info;
    }

    private static String toolKey(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return "qualityessentials.lookat.tool.pickaxe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return "qualityessentials.lookat.tool.axe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return "qualityessentials.lookat.tool.shovel";
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return "qualityessentials.lookat.tool.hoe";
        }
        return "qualityessentials.lookat.tool.other";
    }

    private static Info entity(Entity entity) {
        QEConfig config = QE.config();
        ItemStack icon = ItemStack.EMPTY;
        if (entity instanceof ItemFrame frame) {
            icon = frame.getItem();
        } else if (entity instanceof ItemEntity item) {
            icon = item.getItem();
        }
        Info info = new Info(entity.getDisplayName(), icon);
        if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            info.lines.add(frame.getItem().getHoverName().copy().withStyle(ChatFormatting.GRAY));
        }
        if (entity instanceof ItemEntity item && item.getItem().getCount() > 1) {
            info.lines.add(Component.literal("x" + item.getItem().getCount()).withStyle(ChatFormatting.GRAY));
        }
        if (config.lookAtShowHealth && entity instanceof LivingEntity living) {
            info.health = living.getHealth();
            info.maxHealth = living.getMaxHealth();
            info.armor = living.getArmorValue();
        }
        if (entity instanceof Villager villager) {
            VillagerData data = villager.getVillagerData();
            data.profession().unwrapKey().ifPresent(key -> info.lines.add(
                    Component.translatable("entity.minecraft.villager." + key.identifier().getPath())
                            .append(" - ")
                            .append(Component.translatable("merchant.level." + data.level()))
                            .withStyle(ChatFormatting.GRAY)));
        }
        if (entity instanceof AbstractHorse horse) {
            double speed = LookAtFormat.horseSpeed(horse.getAttributeValue(Attributes.MOVEMENT_SPEED));
            double jump = LookAtFormat.horseJumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH));
            info.lines.add(Component.translatable("qualityessentials.lookat.horse",
                    LookAtFormat.oneDecimal(speed), LookAtFormat.oneDecimal(jump)).withStyle(ChatFormatting.GRAY));
        }
        info.modId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
        return info;
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, Info info) {
        QEConfig config = QE.config();
        boolean hasIcon = !info.icon.isEmpty();
        int textX = hasIcon ? 24 : 6;
        List<Component> lines = new ArrayList<>();
        lines.add(info.title.copy().withStyle(ChatFormatting.WHITE));
        lines.addAll(info.lines);
        String healthText = info.maxHealth > 0 ? LookAtFormat.hearts(info.health, info.maxHealth) : null;
        if (config.lookAtShowMod && info.modId != null) {
            lines.add(Component.literal(QE.modName(info.modId)).withStyle(style -> style.withColor(MOD_COLOR).withItalic(true)));
        }
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int healthWidth = 0;
        if (healthText != null) {
            healthWidth = 11 + font.width(healthText) + (info.armor > 0 ? 16 + font.width(Integer.toString(info.armor)) : 0);
            width = Math.max(width, healthWidth);
        }
        int lineCount = lines.size() + (healthText != null ? 1 : 0);
        int panelWidth = textX + width + 6;
        int panelHeight = Math.max(hasIcon ? 24 : 0, 6 + lineCount * 10) + (info.progress >= 0 ? 3 : 0);
        int x = (graphics.guiWidth() - panelWidth) / 2;
        int y = 4;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND);
        graphics.outline(x, y, panelWidth, panelHeight, BORDER);
        if (hasIcon) {
            graphics.item(info.icon, x + 4, y + 4);
        }
        int lineY = y + 4;
        int index = 0;
        for (Component line : lines) {
            boolean modLine = config.lookAtShowMod && info.modId != null && index == lines.size() - 1;
            if (modLine && healthText != null) {
                drawHealth(graphics, font, x + textX, lineY, healthText, info.armor);
                lineY += 10;
            }
            graphics.text(font, line, x + textX, lineY, 0xFFFFFFFF, true);
            lineY += 10;
            index++;
        }
        if (healthText != null && !(config.lookAtShowMod && info.modId != null)) {
            drawHealth(graphics, font, x + textX, lineY, healthText, info.armor);
        }
        if (info.progress >= 0) {
            int barWidth = Math.round((panelWidth - 2) * Math.min(1f, info.progress));
            graphics.fill(x + 1, y + panelHeight - 3, x + panelWidth - 1, y + panelHeight - 1, 0xFF1B2636);
            graphics.fill(x + 1, y + panelHeight - 3, x + 1 + barWidth, y + panelHeight - 1, ACCENT);
        }
    }

    private static void drawHealth(GuiGraphicsExtractor graphics, Font font, int x, int y, String text, int armor) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, x, y - 1, 9, 9);
        graphics.text(font, text, x + 11, y, 0xFFFF8080, true);
        if (armor > 0) {
            int ax = x + 11 + font.width(text) + 6;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR, ax, y - 1, 9, 9);
            graphics.text(font, Integer.toString(armor), ax + 11, y, 0xFFCFD8E6, true);
        }
    }

    private static final class Info {
        private final Component title;
        private final ItemStack icon;
        private final List<Component> lines = new ArrayList<>();
        private String modId;
        private float health;
        private float maxHealth;
        private int armor;
        private float progress = -1f;

        private Info(Component title, ItemStack icon) {
            this.title = title;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
        }
    }
}
