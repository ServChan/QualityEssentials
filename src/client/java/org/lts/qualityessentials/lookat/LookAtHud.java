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
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.Ui;
import org.lts.qualityessentials.mixin.BossHealthOverlayAccessor;
import org.lts.qualityessentials.mixin.MultiPlayerGameModeAccessor;

import java.util.ArrayList;
import java.util.List;

public final class LookAtHud {
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("hud/armor_full");
    private static final long HOLD_MILLIS = 180L;
    private static final int LINE = 10;
    private static final int HEALTH_BAR = 64;

    private static Info lastInfo;
    private static long lastSeen;
    private static float animatedWidth;

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
            lastInfo = null;
            return;
        }
        HitResult hit = minecraft.hitResult;
        Info info = null;
        if (hit instanceof EntityHitResult entityHit) {
            info = entity(entityHit.getEntity());
        } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            info = block(minecraft, player, blockHit.getBlockPos());
        }
        long now = Util.getMillis();
        if (info != null) {
            lastInfo = info;
            lastSeen = now;
        } else if (lastInfo != null && now - lastSeen < HOLD_MILLIS) {
            info = lastInfo;
        } else {
            lastInfo = null;
            animatedWidth = 0;
            return;
        }
        int targetWidth = measure(minecraft.font, info);
        animatedWidth = animatedWidth <= 0 ? targetWidth : animatedWidth + (targetWidth - animatedWidth) * 0.35f;
        if (Math.abs(animatedWidth - targetWidth) < 1f) {
            animatedWidth = targetWidth;
        }
        int width = Math.round(animatedWidth);
        Info shown = info;
        org.lts.qualityessentials.core.HudLayout layout = QE.config().layout(org.lts.qualityessentials.core.HudElement.LOOK_AT);
        org.lts.qualityessentials.core.HudPlacement.draw(graphics, layout, width, height(shown), bossOffset(minecraft),
                g -> draw(g, minecraft.font, shown, 0, 0, width, layout.opacity));
    }

    private static int bossOffset(Minecraft minecraft) {
        if (minecraft.gui.hud.getBossOverlay() instanceof BossHealthOverlayAccessor accessor) {
            int bosses = accessor.qualityessentials$events().size();
            if (bosses > 0) {
                return 10 + 19 * Math.min(bosses, 6);
            }
        }
        return 0;
    }

    private static Info block(Minecraft minecraft, Player player, BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        QEConfig config = QE.config();
        Info info = new Info(state.getBlock().getName(), new ItemStack(state.getBlock().asItem()), Palette.CYAN);
        if (config.lookAtShowStates) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty integer && LookAtFormat.shows(property.getName())) {
                    List<Integer> values = integer.getPossibleValues();
                    int min = values.getFirst();
                    int max = values.getLast();
                    int value = state.getValue(integer);
                    if ("age".equals(property.getName())) {
                        if (value >= max) {
                            info.lines.add(Component.translatable("qualityessentials.lookat.grown").withStyle(ChatFormatting.GREEN));
                        } else {
                            info.lines.add(Component.translatable("qualityessentials.lookat.growth", LookAtFormat.percent(value, min, max))
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    } else {
                        info.lines.add(Component.translatable("qualityessentials.lookat.state." + property.getName(), value, max)
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
        if (config.lookAtShowHarvest && state.requiresCorrectToolForDrops()) {
            info.tool = Component.translatable("qualityessentials.lookat.tool", Component.translatable(toolKey(state)));
            info.toolOk = player.hasCorrectToolForDrops(state);
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
        int accent = entity instanceof Player ? Palette.PLAYER : entity instanceof Enemy ? Palette.HOSTILE
                : entity instanceof LivingEntity ? Palette.PASSIVE : Palette.CYAN;
        Info info = new Info(entity.getDisplayName(), icon, accent);
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

    public static Info preview(QEConfig config) {
        Info info = new Info(Component.translatable("block.minecraft.wheat"), new ItemStack(Items.WHEAT), Palette.CYAN);
        if (config.lookAtShowStates) {
            info.lines.add(Component.translatable("qualityessentials.lookat.growth", 57).withStyle(ChatFormatting.GRAY));
        }
        if (config.lookAtShowHarvest) {
            info.tool = Component.translatable("qualityessentials.lookat.tool", Component.translatable("qualityessentials.lookat.tool.pickaxe"));
            info.toolOk = true;
        }
        if (config.lookAtShowHealth) {
            info.health = 14f;
            info.maxHealth = 20f;
            info.armor = 4;
        }
        if (config.lookAtShowProgress) {
            info.progress = 0.6f;
        }
        info.modId = "minecraft";
        return info;
    }

    private static boolean showMod(Info info) {
        return QE.config().lookAtShowMod && info.modId != null;
    }

    public static int measure(Font font, Info info) {
        int iconSpace = info.icon.isEmpty() ? 0 : 20;
        int width = font.width(info.title);
        for (Component line : info.lines) {
            width = Math.max(width, font.width(line));
        }
        if (info.tool != null) {
            width = Math.max(width, font.width(info.tool) + 4 + font.width(toolState(info)) + 8);
        }
        if (info.maxHealth > 0) {
            width = Math.max(width, healthWidth(font, info));
        }
        if (showMod(info)) {
            width = Math.max(width, font.width(QE.modName(info.modId)));
        }
        return 8 + iconSpace + width + 8;
    }

    public static int height(Info info) {
        int lines = 1 + info.lines.size() + (info.tool != null ? 1 : 0) + (info.maxHealth > 0 ? 1 : 0) + (showMod(info) ? 1 : 0);
        int content = 6 + lines * LINE + 2;
        return Math.max(info.icon.isEmpty() ? 0 : 26, content) + (info.progress >= 0 ? 3 : 0);
    }

    private static Component toolState(Info info) {
        return Component.translatable(info.toolOk ? "qualityessentials.lookat.tool.ok" : "qualityessentials.lookat.tool.missing");
    }

    private static int healthWidth(Font font, Info info) {
        int width = 11 + HEALTH_BAR + 4 + font.width(LookAtFormat.hearts(info.health, info.maxHealth));
        if (info.armor > 0) {
            width += 8 + 11 + font.width(Integer.toString(info.armor));
        }
        return width;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, Info info, int x, int y, int width, float opacity) {
        int height = height(info);
        if (opacity > 0.01f) {
            graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, Ui.withAlpha(0x000000, 0.31f * opacity));
            Ui.panel(graphics, x, y, width, height, Ui.withAlpha(Palette.HUD_PANEL, 0.88f * opacity), Ui.withAlpha(Palette.HUD_BORDER, opacity));
        }
        graphics.fill(x, y, x + 2, y + height, info.accent);
        graphics.enableScissor(x + 2, y + 1, x + width - 1, y + height - 1);
        boolean hasIcon = !info.icon.isEmpty();
        if (hasIcon) {
            graphics.item(info.icon, x + 6, y + 5);
        }
        int textX = x + 6 + (hasIcon ? 20 : 2);
        int lineY = y + 5;
        graphics.text(font, info.title, textX, lineY, Palette.TEXT_BRIGHT, true);
        lineY += LINE + 1;
        for (Component line : info.lines) {
            graphics.text(font, line, textX, lineY, Palette.TEXT, true);
            lineY += LINE;
        }
        if (info.tool != null) {
            graphics.text(font, info.tool, textX, lineY, Palette.TEXT, true);
            Component state = toolState(info);
            int pillX = textX + font.width(info.tool) + 4;
            Ui.pill(graphics, font, state, pillX, lineY - 2, info.toolOk ? 0xFF1E5631 : 0xFF5A1E24,
                    info.toolOk ? Palette.SUCCESS : 0xFFFF8A80);
            lineY += LINE;
        }
        if (info.maxHealth > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, textX, lineY - 1, 9, 9);
            Ui.bar(graphics, textX + 11, lineY + 2, HEALTH_BAR, 4, info.health / Math.max(1f, info.maxHealth), Palette.HEALTH, Palette.HUD_TRACK);
            String hearts = LookAtFormat.hearts(info.health, info.maxHealth);
            int hx = textX + 11 + HEALTH_BAR + 4;
            graphics.text(font, hearts, hx, lineY, 0xFFFF9C9C, true);
            if (info.armor > 0) {
                int ax = hx + font.width(hearts) + 8;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR, ax, lineY - 1, 9, 9);
                graphics.text(font, Integer.toString(info.armor), ax + 11, lineY, 0xFFCFD8E6, true);
            }
            lineY += LINE;
        }
        if (showMod(info)) {
            graphics.text(font, Component.literal(QE.modName(info.modId)).withStyle(style -> style.withColor(Palette.MOD_NAME).withItalic(true)),
                    textX, lineY, Palette.MOD_NAME, true);
        }
        graphics.disableScissor();
        if (info.progress >= 0) {
            Ui.bar(graphics, x + 2, y + height - 3, width - 3, 2, Math.min(1f, info.progress), Palette.CYAN, Palette.HUD_TRACK);
        }
    }

    public static final class Info {
        private final Component title;
        private final ItemStack icon;
        private final int accent;
        private final List<Component> lines = new ArrayList<>();
        private Component tool;
        private boolean toolOk;
        private String modId;
        private float health;
        private float maxHealth;
        private int armor;
        private float progress = -1f;

        private Info(Component title, ItemStack icon, int accent) {
            this.title = title;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.accent = accent;
        }
    }
}
