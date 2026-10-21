package org.lts.qualityessentials.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class WorldScope {
    private WorldScope() {
    }

    public static String of(Minecraft minecraft) {
        ServerData server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return "server:" + server.ip.trim().toLowerCase(Locale.ROOT);
        }
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            return "world:" + integrated.getWorldData().getLevelName();
        }
        return null;
    }

    public static Path file(String folder, String scope) {
        String readable = scope.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (readable.length() > 64) {
            readable = readable.substring(0, 64);
        }
        return FabricLoader.getInstance().getConfigDir().resolve("qualityessentials").resolve(folder)
                .resolve(readable + "_" + hash(scope) + ".json");
    }

    private static String hash(String scope) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(scope.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
