package org.lts.qualityessentials.stats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.HudPlacement;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.Palette;
import org.lts.qualityessentials.gui.Ui;

import java.util.ArrayList;
import java.util.List;

public final class StatsHud {
    private static final int LINE = 10;
    private static final int PAD = 4;
    private static final int PANEL = 0x000000;

    private StatsHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        StatsCollector.onFrame();
        if (!QE.active(Module.STATS)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getDebugOverlay().showDebugScreen() || minecraft.gui.screen() != null) {
            return;
        }
        QEConfig config = QE.config();
        List<Stat> stats = collect(minecraft, config);
        if (stats.isEmpty()) {
            return;
        }
        float opacity = config.layout(HudElement.STATS).opacity;
        int[] size = size(minecraft.font, stats, config.statsCompact);
        HudPlacement.draw(graphics, config.layout(HudElement.STATS), size[0], size[1], 0,
                g -> draw(g, minecraft.font, stats, config.statsCompact, opacity));
    }

    public static List<Stat> collect(Minecraft minecraft, QEConfig config) {
        List<Stat> stats = new ArrayList<>();
        int fps = minecraft.getFps();
        if (config.statsFps) {
            String value = fps + (config.statsLows ? " / " + StatsCollector.onePercentLow() : "");
            stats.add(new Stat(Component.translatable(config.statsLows ? "qualityessentials.stats.fps_lows" : "qualityessentials.stats.fps"),
                    value, StatFormat.fpsColor(fps)));
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        Player player = minecraft.player;
        if (server != null) {
            double target = server.tickRateManager().tickrate();
            double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
            double budget = 1000.0 / Math.max(1.0, target);
            if (config.statsTps) {
                double tps = mspt <= 0 ? target : Math.min(target, 1000.0 / mspt);
                stats.add(new Stat(Component.translatable("qualityessentials.stats.tps"), StatFormat.oneDecimal(tps), StatFormat.tpsColor(tps, target)));
            }
            if (config.statsMspt) {
                stats.add(new Stat(Component.translatable("qualityessentials.stats.mspt"), StatFormat.oneDecimal(mspt), StatFormat.msptColor(mspt, budget)));
            }
        } else {
            if (config.statsPing && player != null && minecraft.getConnection() != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) {
                    int ping = info.getLatency();
                    stats.add(new Stat(Component.translatable("qualityessentials.stats.ping"),
                            Component.translatable("qualityessentials.stats.ms", ping).getString(), StatFormat.pingColor(ping)));
                }
            }
            if (config.statsTps) {
                double tps = StatsCollector.estimatedServerTps();
                stats.add(new Stat(Component.translatable("qualityessentials.stats.tps_server"),
                        tps < 0 ? "-" : StatFormat.oneDecimal(tps), tps < 0 ? StatFormat.NEUTRAL : StatFormat.tpsColor(tps, 20)));
            }
        }
        if (config.statsMemory) {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long max = runtime.maxMemory();
            stats.add(new Stat(Component.translatable("qualityessentials.stats.memory"),
                    StatFormat.gigabytes(used) + "/" + StatFormat.gigabytes(max), StatFormat.memoryColor(used / (double) max)));
        }
        if (config.statsEntities && minecraft.level != null) {
            stats.add(new Stat(Component.translatable("qualityessentials.stats.entities"), Integer.toString(minecraft.level.getEntityCount()), StatFormat.NEUTRAL));
        }
        if (config.statsCoordinates && player != null && !player.isReducedDebugInfo()) {
            stats.add(new Stat(Component.translatable("qualityessentials.stats.coords"),
                    player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ(), StatFormat.NEUTRAL));
        }
        ClientLevel level = minecraft.level;
        if (level != null && player != null && !level.dimensionType().hasFixedTime()) {
            if (config.statsTime) {
                long ticks = level.getDefaultClockTime();
                stats.add(time(ticks, level.isThundering()));
            }
            if (config.statsWeather) {
                BlockPos pos = player.blockPosition();
                Biome.Precipitation precipitation = level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel());
                String kind = !level.isRaining() || precipitation == Biome.Precipitation.NONE ? "clear"
                        : level.isThundering() ? "thunder" : precipitation == Biome.Precipitation.SNOW ? "snow" : "rain";
                stats.add(weather(kind, config.statsCompact));
            }
        }
        return stats;
    }

    public static List<Stat> sample(QEConfig config, boolean singleplayer) {
        List<Stat> stats = new ArrayList<>();
        if (config.statsFps) {
            stats.add(new Stat(Component.translatable(config.statsLows ? "qualityessentials.stats.fps_lows" : "qualityessentials.stats.fps"),
                    config.statsLows ? "144 / 97" : "144", StatFormat.GOOD));
        }
        if (singleplayer) {
            if (config.statsTps) {
                stats.add(new Stat(Component.translatable("qualityessentials.stats.tps"), "20.0", StatFormat.GOOD));
            }
            if (config.statsMspt) {
                stats.add(new Stat(Component.translatable("qualityessentials.stats.mspt"), "12.4", StatFormat.GOOD));
            }
        } else {
            if (config.statsPing) {
                stats.add(new Stat(Component.translatable("qualityessentials.stats.ping"),
                        Component.translatable("qualityessentials.stats.ms", 42).getString(), StatFormat.GOOD));
            }
            if (config.statsTps) {
                stats.add(new Stat(Component.translatable("qualityessentials.stats.tps_server"), "19.6", StatFormat.GOOD));
            }
        }
        if (config.statsMemory) {
            stats.add(new Stat(Component.translatable("qualityessentials.stats.memory"), "2.4/6.0", StatFormat.GOOD));
        }
        if (config.statsEntities) {
            stats.add(new Stat(Component.translatable("qualityessentials.stats.entities"), "532", StatFormat.NEUTRAL));
        }
        if (config.statsCoordinates) {
            stats.add(new Stat(Component.translatable("qualityessentials.stats.coords"), "128 64 -340", StatFormat.NEUTRAL));
        }
        if (config.statsTime) {
            stats.add(time(11 * StatFormat.DAY_TICKS + 8500, false));
        }
        if (config.statsWeather) {
            stats.add(weather("rain", config.statsCompact));
        }
        return stats;
    }

    private static Stat time(long ticks, boolean thundering) {
        boolean sleep = StatFormat.canSleep(ticks, thundering);
        return new Stat(Component.translatable("qualityessentials.stats.day", StatFormat.day(ticks)), StatFormat.clock(ticks),
                sleep ? StatFormat.NIGHT : StatFormat.NEUTRAL);
    }

    private static Stat weather(String kind, boolean compact) {
        int color = switch (kind) {
            case "thunder" -> StatFormat.THUNDER;
            case "snow" -> StatFormat.SNOW;
            case "rain" -> StatFormat.RAIN;
            default -> StatFormat.NEUTRAL;
        };
        Component label = compact ? Component.empty() : Component.translatable("qualityessentials.stats.weather");
        return new Stat(label, Component.translatable("qualityessentials.stats.weather." + kind).getString(), color);
    }

    public static int[] size(Font font, List<Stat> stats, boolean compact) {
        if (compact) {
            int width = 0;
            for (int i = 0; i < stats.size(); i++) {
                Stat stat = stats.get(i);
                int label = font.width(stat.label);
                width += font.width(stat.value) + (label > 0 ? 3 + label : 0) + (i > 0 ? 9 : 0);
            }
            return new int[]{width + PAD * 2, LINE + 3};
        }
        int labels = 0;
        int values = 0;
        for (Stat stat : stats) {
            labels = Math.max(labels, font.width(stat.label));
            values = Math.max(values, font.width(stat.value));
        }
        return new int[]{PAD * 2 + labels + 8 + values, PAD + stats.size() * LINE + 1};
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, List<Stat> stats, boolean compact, float opacity) {
        int[] size = size(font, stats, compact);
        if (opacity > 0.01f) {
            graphics.fill(0, 0, size[0], size[1], Ui.withAlpha(PANEL, 0.5f * opacity));
        }
        if (compact) {
            int x = PAD;
            for (int i = 0; i < stats.size(); i++) {
                Stat stat = stats.get(i);
                if (i > 0) {
                    graphics.fill(x + 3, 3, x + 4, LINE, Palette.BORDER_STRONG);
                    x += 9;
                }
                graphics.text(font, stat.value, x, 2, stat.color, true);
                x += font.width(stat.value);
                int label = font.width(stat.label);
                if (label > 0) {
                    graphics.text(font, stat.label, x + 3, 2, Palette.TEXT_DIM, true);
                    x += 3 + label;
                }
            }
            return;
        }
        int y = PAD - 1;
        for (Stat stat : stats) {
            graphics.text(font, stat.label, PAD, y, Palette.TEXT_DIM, true);
            graphics.text(font, stat.value, size[0] - PAD - font.width(stat.value), y, stat.color, true);
            y += LINE;
        }
    }

    public record Stat(Component label, String value, int color) {
    }
}
