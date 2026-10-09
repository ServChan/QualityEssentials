package org.lts.qualityessentials.mount;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.HudPlacement;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.Ui;
import org.lts.qualityessentials.lookat.LookAtFormat;

public final class MountHud {
    private static final int PAD = 4;
    private static final int TEXT_X = 24;
    private static final int PANEL = 0xD00B1119;

    private static Entity tracked;
    private static Vec3 lastPosition;
    private static double smoothedSpeed;

    private MountHud() {
    }

    public static boolean showPlayerFood() {
        return QE.active(Module.MOUNT) && QE.config().mountPlayerFood;
    }

    public static void tick(Minecraft minecraft) {
        Player player = minecraft.player;
        Entity vehicle = player == null ? null : player.getVehicle();
        if (vehicle == null) {
            tracked = null;
            lastPosition = null;
            smoothedSpeed = 0;
            return;
        }
        Vec3 position = vehicle.position();
        if (vehicle != tracked || lastPosition == null) {
            tracked = vehicle;
            lastPosition = position;
            smoothedSpeed = 0;
            return;
        }
        double dx = position.x - lastPosition.x;
        double dz = position.z - lastPosition.z;
        double perSecond = Math.sqrt(dx * dx + dz * dz) * 20.0;
        smoothedSpeed = MountMath.smooth(smoothedSpeed, perSecond);
        lastPosition = position;
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        if (!QE.active(Module.MOUNT)) {
            return;
        }
        QEConfig config = QE.config();
        if (!config.mountPanel) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        Info info = collect(player, config);
        if (info == null) {
            return;
        }
        float opacity = config.layout(HudElement.MOUNT).opacity;
        int[] size = size(minecraft.font, info);
        HudPlacement.draw(graphics, config.layout(HudElement.MOUNT), size[0], size[1], 0, g -> draw(g, minecraft.font, info, opacity));
    }

    public static Info collect(Player player, QEConfig config) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return null;
        }
        ItemStack icon = vehicle.getPickResult();
        if (icon == null || icon.isEmpty()) {
            icon = new ItemStack(Items.SADDLE);
        }
        float health = -1;
        float maxHealth = -1;
        if (vehicle instanceof LivingEntity living) {
            health = living.getHealth();
            maxHealth = living.getMaxHealth();
        }
        double speed = -1;
        double jump = -1;
        if (vehicle instanceof AbstractHorse horse) {
            speed = LookAtFormat.horseSpeed(horse.getAttributeValue(Attributes.MOVEMENT_SPEED));
            jump = LookAtFormat.horseJumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH));
        }
        double current = config.mountCurrentSpeed && vehicle == tracked ? smoothedSpeed : -1;
        return new Info(vehicle.getDisplayName(), icon, health, maxHealth, speed, jump, current);
    }

    public static Info preview(QEConfig config) {
        return new Info(Component.translatable("entity.minecraft.horse"), new ItemStack(Items.SADDLE), 22f, 30f, 13.4, 4.1,
                config.mountCurrentSpeed ? 9.8 : -1);
    }

    private static Component currentText(Info info) {
        return Component.translatable("qualityessentials.mount.speed_now", LookAtFormat.oneDecimal(info.current));
    }

    private static Component statsText(Info info) {
        if (info.speed < 0) {
            return null;
        }
        Component speed = Component.translatable("qualityessentials.mount.speed", LookAtFormat.oneDecimal(info.speed));
        if (info.jump < 0) {
            return speed;
        }
        return speed.copy().append("  ").append(Component.translatable("qualityessentials.mount.jump", LookAtFormat.oneDecimal(info.jump)));
    }

    private static String healthText(Info info) {
        return Math.round(Math.max(0f, info.health)) + "/" + Math.round(info.maxHealth);
    }

    public static int[] size(Font font, Info info) {
        int top = font.width(info.name) + (info.current >= 0 ? 10 + font.width(currentText(info)) : 0);
        int width = top;
        if (info.maxHealth > 0) {
            width = Math.max(width, 60 + 4 + font.width(healthText(info)));
        }
        Component stats = statsText(info);
        if (stats != null) {
            width = Math.max(width, font.width(stats));
        }
        int rows = 1 + (info.maxHealth > 0 ? 1 : 0) + (stats != null ? 1 : 0);
        return new int[]{TEXT_X + width + PAD, Math.max(24, PAD + rows * 10 + 2)};
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, Info info, float opacity) {
        int[] size = size(font, info);
        if (opacity > 0.01f) {
            graphics.fill(0, 0, size[0], size[1], Ui.withAlpha(PANEL, 0.82f * opacity));
            graphics.fill(0, 0, 2, size[1], Ui.withAlpha(Palette.ACCENT, Math.max(0.5f, opacity)));
        }
        graphics.item(info.icon, PAD + 1, (size[1] - 16) / 2);
        int y = PAD;
        graphics.text(font, info.name, TEXT_X, y, Palette.TEXT_BRIGHT, true);
        if (info.current >= 0) {
            Component current = currentText(info);
            graphics.text(font, current, size[0] - PAD - font.width(current), y, Palette.CYAN, true);
        }
        y += 10;
        if (info.maxHealth > 0) {
            String health = healthText(info);
            int barWidth = size[0] - PAD - TEXT_X - font.width(health) - 4;
            Ui.bar(graphics, TEXT_X, y + 2, barWidth, 4, info.health / info.maxHealth, Palette.HEALTH, Palette.HUD_TRACK);
            graphics.text(font, health, size[0] - PAD - font.width(health), y, Palette.TEXT, true);
            y += 10;
        }
        Component stats = statsText(info);
        if (stats != null) {
            graphics.text(font, stats, TEXT_X, y, Palette.TEXT_DIM, true);
        }
    }

    public record Info(Component name, ItemStack icon, float health, float maxHealth, double speed, double jump, double current) {
    }
}
