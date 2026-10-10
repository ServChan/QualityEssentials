package org.lts.qualityessentials.hidden;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;

import java.util.ArrayList;
import java.util.List;

public final class ItemDetails {
    private static final int WRAP = 46;
    private static final int VALUE_WIDTH = 42;
    private static final int MAX_DATA_LINES = 14;
    private static final String KEY = "qualityessentials.item_details.";
    private static boolean revealed;

    private ItemDetails() {
    }

    private static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.isSameThread() && QE.active(Module.ITEM_DETAILS);
    }

    private static boolean hides(TooltipDisplay display) {
        return !display.hideTooltip() && !display.hiddenComponents().isEmpty();
    }

    public static TooltipDisplay display(TooltipDisplay display) {
        revealed = false;
        if (active() && hides(display) && Minecraft.getInstance().hasShiftDown()) {
            revealed = true;
            return TooltipDisplay.DEFAULT;
        }
        return display;
    }

    public static void finish(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag, List<Component> lines) {
        boolean wasRevealed = revealed;
        revealed = false;
        if (lines.isEmpty() || !active()) {
            return;
        }
        QEConfig config = QE.config();
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (Minecraft.getInstance().hasShiftDown()) {
            if (config.itemDetailsBlock) {
                lines.addAll(block(stack, display, config.itemDetailsData, flag.isAdvanced()));
            } else if (wasRevealed) {
                lines.add(dim(Component.translatable(KEY + "shown")));
            }
            return;
        }
        if (config.itemDetailsHint && hides(display)) {
            int extra = count(stack, context, player, flag, TooltipDisplay.DEFAULT) - count(stack, context, player, flag, display);
            if (extra > 0) {
                lines.add(dim(Component.translatable(KEY + "hint", extra)));
            }
        }
    }

    private static int count(ItemStack stack, Item.TooltipContext context, Player player, TooltipFlag flag, TooltipDisplay display) {
        int[] lines = {0};
        stack.addDetailsToTooltip(context, display, player, flag, line -> lines[0]++);
        return lines[0];
    }

    static List<Component> block(ItemStack stack, TooltipDisplay display, boolean serverData, boolean advanced) {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(Component.translatable(KEY + "header").withStyle(ChatFormatting.DARK_AQUA));
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!advanced) {
            out.add(row("id", Component.literal(id.toString())));
        }
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model != null && !model.equals(id)) {
            out.add(row("model", Component.literal(model.toString())));
        }
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            out.add(row("durability", Component.literal(left + " / " + max + " (" + DetailsText.percent(left, max) + "%)")));
        }
        Integer repair = stack.get(DataComponents.REPAIR_COST);
        if (repair != null && repair > 0) {
            out.add(row("repair", Component.literal(String.valueOf(repair))));
        }
        Rarity rarity = stack.getRarity();
        if (rarity != Rarity.COMMON) {
            out.add(row("rarity", Component.translatable(KEY + "rarity." + rarity.getSerializedName()).withStyle(rarity.color())));
        }
        int stackSize = stack.getMaxStackSize();
        if (stackSize != 64) {
            out.add(row("stack", Component.literal(String.valueOf(stackSize))));
        }
        if (display.hideTooltip() || !display.hiddenComponents().isEmpty()) {
            List<String> hidden = new ArrayList<>();
            if (display.hideTooltip()) {
                hidden.add("*");
            }
            for (DataComponentType<?> type : display.hiddenComponents()) {
                hidden.add(name(type));
            }
            listRows(out, "hidden", hidden);
        }
        if (serverData) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                out.add(label("data"));
                List<String> entries = new ArrayList<>();
                flatten("", data.copyTag(), entries, 0);
                for (int i = 0; i < Math.min(entries.size(), MAX_DATA_LINES); i++) {
                    out.add(Component.literal("  " + entries.get(i)).withStyle(ChatFormatting.GRAY));
                }
                if (entries.size() > MAX_DATA_LINES) {
                    out.add(dim(Component.translatable(KEY + "more", entries.size() - MAX_DATA_LINES)));
                }
            }
        }
        DataComponentPatch.SplitResult patch = stack.getComponentsPatch().split();
        List<String> added = new ArrayList<>();
        for (DataComponentType<?> type : patch.added().keySet()) {
            added.add(name(type));
        }
        if (!added.isEmpty()) {
            listRows(out, "added", added);
        }
        List<String> removed = new ArrayList<>();
        for (DataComponentType<?> type : patch.removed()) {
            removed.add(name(type));
        }
        if (!removed.isEmpty()) {
            listRows(out, "removed", removed);
        }
        return out;
    }

    private static void flatten(String prefix, CompoundTag tag, List<String> out, int depth) {
        for (String key : tag.keySet().stream().sorted().toList()) {
            Tag value = tag.get(key);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof CompoundTag compound && depth < 2 && !compound.isEmpty()) {
                flatten(path, compound, out, depth + 1);
            } else if (value != null) {
                String text = value.asString().orElseGet(value::toString);
                out.add(DetailsText.shorten(path + " = " + text, VALUE_WIDTH + 10));
            }
        }
    }

    private static void listRows(List<Component> out, String key, List<String> words) {
        List<String> lines = DetailsText.wrap(words, WRAP);
        out.add(label(key).append(Component.literal(" (" + words.size() + ")").withStyle(ChatFormatting.DARK_GRAY)));
        for (String line : lines) {
            out.add(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String name(DataComponentType<?> type) {
        Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return key == null ? "?" : DetailsText.stripNamespace(key.toString());
    }

    private static MutableComponent label(String key) {
        return Component.translatable(KEY + key).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component row(String key, Component value) {
        return label(key).append(Component.literal(" ")).append(value.copy().withStyle(style -> style.getColor() == null
                ? style.withColor(ChatFormatting.GRAY) : style));
    }

    private static Component dim(MutableComponent component) {
        return component.withStyle(ChatFormatting.DARK_GRAY);
    }

    public static List<Component> preview(boolean shift, QEConfig config) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("qualityessentials.settings.preview.details.name").withStyle(ChatFormatting.AQUA));
        if (shift) {
            lines.add(Component.translatable("enchantment.minecraft.efficiency").append(" V").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("enchantment.minecraft.mending").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("qualityessentials.settings.preview.details.lore").withStyle(ChatFormatting.GRAY));
        if (!shift) {
            if (config.itemDetailsHint) {
                lines.add(dim(Component.translatable(KEY + "hint", 2)));
            }
            return lines;
        }
        if (!config.itemDetailsBlock) {
            lines.add(dim(Component.translatable(KEY + "shown")));
            return lines;
        }
        lines.add(Component.translatable(KEY + "header").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(row("id", Component.literal("minecraft:netherite_pickaxe")));
        lines.add(row("model", Component.literal("server:jade_patron")));
        lines.add(row("durability", Component.literal("1820 / 2031 (90%)")));
        lines.add(row("repair", Component.literal("7")));
        lines.add(row("rarity", Component.translatable(KEY + "rarity.epic").withStyle(ChatFormatting.LIGHT_PURPLE)));
        if (config.itemDetailsData) {
            lines.add(label("data"));
            lines.add(Component.literal("  PublicBukkitValues.server:id = jade_patron").withStyle(ChatFormatting.GRAY));
        }
        lines.add(label("added").append(Component.literal(" (5)").withStyle(ChatFormatting.DARK_GRAY)));
        lines.add(Component.literal("  custom_name, lore, item_model, enchantments,").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  tooltip_display").withStyle(ChatFormatting.GRAY));
        return lines;
    }

}
