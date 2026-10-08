package org.lts.qualityessentials.food;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;

import java.util.Locale;

public final class FoodHud {
    private static final Identifier SATURATION_FULL = Identifier.fromNamespaceAndPath("qualityessentials", "hud/saturation_full");
    private static final Identifier SATURATION_HALF = Identifier.fromNamespaceAndPath("qualityessentials", "hud/saturation_half");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");

    private FoodHud() {
    }

    public static void registerTooltip() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!QE.active(Module.FOOD) || !QE.config().foodTooltip) {
                return;
            }
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food == null) {
                return;
            }
            Component line = Component.translatable("qualityessentials.food.tooltip", food.nutrition(),
                    String.format(Locale.ROOT, "%.1f", food.saturation())).withStyle(ChatFormatting.GOLD);
            lines.add(Math.min(1, lines.size()), line);
        });
    }

    public static void extract(GuiGraphicsExtractor graphics, Player player, int top, int right) {
        if (!QE.active(Module.FOOD) || player == null) {
            return;
        }
        QEConfig config = QE.config();
        FoodData data = player.getFoodData();
        int food = data.getFoodLevel();
        float saturation = data.getSaturationLevel();
        if (config.foodSaturationOverlay) {
            drawSaturation(graphics, saturation, top, right, 0xFFFFFFFF, -1f);
        }
        if (!config.foodHeldPreview) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        FoodProperties props = held.get(DataComponents.FOOD);
        if (props == null) {
            held = player.getOffhandItem();
            props = held.get(DataComponents.FOOD);
        }
        if (props == null || !FoodMath.canEat(food, props.canAlwaysEat(), player.isCreative())) {
            return;
        }
        FoodMath.Result after = FoodMath.afterEating(food, saturation, props.nutrition(), props.saturation());
        float pulse = 0.25f + 0.45f * (Mth.sin((net.minecraft.util.Util.getMillis() % 100000L) / 160f) * 0.5f + 0.5f);
        int color = ARGB.white(pulse);
        for (int icon = 0; icon < 10; icon++) {
            FoodMath.Fill now = FoodMath.iconFill(food, icon);
            FoodMath.Fill later = FoodMath.iconFill(after.food(), icon);
            if (later == now || later == FoodMath.Fill.EMPTY) {
                continue;
            }
            int x = right - icon * 8 - 9;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, later == FoodMath.Fill.FULL ? FOOD_FULL : FOOD_HALF, x, top, 9, 9, color);
        }
        if (config.foodSaturationOverlay) {
            drawSaturation(graphics, after.saturation(), top, right, color, saturation);
        }
    }

    public static void preview(GuiGraphicsExtractor graphics, int right, int top, QEConfig config) {
        int food = 13;
        float saturation = 7f;
        Identifier empty = Identifier.withDefaultNamespace("hud/food_empty");
        for (int icon = 0; icon < 10; icon++) {
            int x = right - icon * 8 - 9;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, x, top, 9, 9);
            FoodMath.Fill fill = FoodMath.iconFill(food, icon);
            if (fill != FoodMath.Fill.EMPTY) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill == FoodMath.Fill.FULL ? FOOD_FULL : FOOD_HALF, x, top, 9, 9);
            }
        }
        if (config.foodSaturationOverlay) {
            drawSaturation(graphics, saturation, top, right, 0xFFFFFFFF, -1f);
        }
        if (config.foodHeldPreview) {
            FoodMath.Result after = FoodMath.afterEating(food, saturation, 8, 12.8f);
            float pulse = 0.25f + 0.45f * (Mth.sin((net.minecraft.util.Util.getMillis() % 100000L) / 160f) * 0.5f + 0.5f);
            int color = ARGB.white(pulse);
            for (int icon = 0; icon < 10; icon++) {
                FoodMath.Fill now = FoodMath.iconFill(food, icon);
                FoodMath.Fill later = FoodMath.iconFill(after.food(), icon);
                if (later != now && later != FoodMath.Fill.EMPTY) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, later == FoodMath.Fill.FULL ? FOOD_FULL : FOOD_HALF,
                            right - icon * 8 - 9, top, 9, 9, color);
                }
            }
            if (config.foodSaturationOverlay) {
                drawSaturation(graphics, after.saturation(), top, right, color, saturation);
            }
        }
    }

    private static void drawSaturation(GuiGraphicsExtractor graphics, float saturation, int top, int right, int color, float skipBelow) {
        for (int icon = 0; icon < 10; icon++) {
            FoodMath.Fill fill = FoodMath.iconFill(saturation, icon);
            if (fill == FoodMath.Fill.EMPTY) {
                break;
            }
            if (skipBelow >= 0 && FoodMath.iconFill(skipBelow, icon) == fill) {
                continue;
            }
            int x = right - icon * 8 - 9;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill == FoodMath.Fill.FULL ? SATURATION_FULL : SATURATION_HALF,
                    x, top, 9, 9, color);
        }
    }
}
