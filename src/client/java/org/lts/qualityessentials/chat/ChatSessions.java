package org.lts.qualityessentials.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import org.lts.qualityessentials.QualityEssentialsClient;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.mixin.ChatComponentAccessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatSessions {
    static final int MAX_SAVED = 1000;
    private static final int FADED_TICKS = 1000;
    private static final Gson GSON = new GsonBuilder().create();
    private static final GuiMessageTag RESTORED_TAG = new GuiMessageTag(0x5A5A5A, null,
            Component.translatable("qualityessentials.chat.restored.tag"), "Restored");
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "QualityEssentials-Chat");
        thread.setDaemon(true);
        return thread;
    });

    private static ClientPacketListener session;
    private static String scope;
    private static boolean dirty;

    private ChatSessions() {
    }

    static ExecutorService io() {
        return IO;
    }

    static void markDirty() {
        dirty = true;
    }

    public static void tick(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.level == null || minecraft.player == null) {
            if (connection == null) {
                session = null;
            }
            return;
        }
        if (connection == session) {
            return;
        }
        session = connection;
        scope = scope(minecraft);
        dirty = false;
        if (scope == null || !ChatModule.active() || !ChatModule.config().chatRestore) {
            return;
        }
        ClientPacketListener expected = connection;
        Path file = file(scope);
        CompletableFuture.supplyAsync(() -> read(file), IO).thenAcceptAsync(saved -> {
            if (saved != null && session == expected && minecraft.level != null) {
                restore(minecraft, saved);
            }
        }, minecraft);
    }

    public static void onDisconnected(Minecraft minecraft) {
        String current = scope;
        session = null;
        scope = null;
        if (current == null || !dirty || !ChatModule.active() || !ChatModule.config().chatRestore) {
            return;
        }
        dirty = false;
        JsonObject snapshot = snapshot(minecraft);
        Path file = file(current);
        if (minecraft.isRunning()) {
            IO.execute(() -> write(file, snapshot));
        } else {
            write(file, snapshot);
        }
    }

    private static JsonObject snapshot(Minecraft minecraft) {
        ChatComponent chat = minecraft.gui.hud.getChat();
        List<GuiMessage> all = ((ChatComponentAccessor) chat).qualityessentials$allMessages();
        DynamicOps<JsonElement> ops = minecraft.level == null ? JsonOps.INSTANCE
                : RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        List<JsonObject> newestFirst = new ArrayList<>();
        for (GuiMessage message : all) {
            if (newestFirst.size() >= MAX_SAVED) {
                break;
            }
            if (ChatDecor.isSeparator(message)) {
                continue;
            }
            JsonElement text = ComponentSerialization.CODEC.encodeStart(ops, message.content()).result().orElse(null);
            if (text == null) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("time", ChatDecor.time(message));
            entry.addProperty("source", message.source().name());
            entry.add("text", text);
            newestFirst.add(entry);
        }
        JsonArray messages = new JsonArray();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            messages.add(newestFirst.get(i));
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("messages", messages);
        return root;
    }

    private static void restore(Minecraft minecraft, JsonArray saved) {
        if (saved.isEmpty() || !ChatModule.active() || !ChatModule.config().chatRestore) {
            return;
        }
        ChatComponent chat = minecraft.gui.hud.getChat();
        ChatComponentAccessor access = (ChatComponentAccessor) chat;
        List<GuiMessage> all = access.qualityessentials$allMessages();
        int room = ChatModule.config().chatHistorySize - all.size() - 1;
        if (room <= 0) {
            return;
        }
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        int ticks = minecraft.gui.hud.getGuiTicks() - FADED_TICKS;
        List<GuiMessage> restored = new ArrayList<>();
        long newest = 0L;
        for (int i = saved.size() - 1; i >= 0 && restored.size() < room; i--) {
            if (!(saved.get(i) instanceof JsonObject entry) || !entry.has("text")) {
                continue;
            }
            Component text = ComponentSerialization.CODEC.parse(ops, entry.get("text")).result().orElse(null);
            if (text == null) {
                continue;
            }
            long time = entry.has("time") ? entry.get("time").getAsLong() : 0L;
            newest = Math.max(newest, time);
            GuiMessage message = new GuiMessage(ticks, text, null, source(entry), RESTORED_TAG);
            ChatDecor.markRestored(message, time);
            restored.add(message);
        }
        if (restored.isEmpty()) {
            return;
        }
        String when = ChatDecor.format(QEConfig.ChatTimestamps.MINUTES, newest);
        GuiMessage separator = new GuiMessage(ticks, Component.translatable("qualityessentials.chat.restored.separator", when)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), null, GuiMessageSource.SYSTEM_CLIENT, RESTORED_TAG);
        ChatDecor.markSeparator(separator);
        all.add(separator);
        all.addAll(restored);
        access.qualityessentials$refreshTrimmedMessages();
    }

    private static GuiMessageSource source(JsonObject entry) {
        try {
            return GuiMessageSource.valueOf(entry.get("source").getAsString());
        } catch (RuntimeException exception) {
            return GuiMessageSource.SYSTEM_SERVER;
        }
    }

    private static JsonArray read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root instanceof JsonObject object && object.get("messages") instanceof JsonArray messages) {
                return messages;
            }
        } catch (IOException | RuntimeException exception) {
            QualityEssentialsClient.LOGGER.warn("Failed to read saved chat {}", file, exception);
        }
        return null;
    }

    private static void write(Path file, JsonObject snapshot) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(snapshot), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            QualityEssentialsClient.LOGGER.warn("Failed to save chat {}", file, exception);
        }
    }

    private static String scope(Minecraft minecraft) {
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

    static Path file(String scope) {
        String readable = scope.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (readable.length() > 64) {
            readable = readable.substring(0, 64);
        }
        return FabricLoader.getInstance().getConfigDir().resolve("qualityessentials").resolve("chat")
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
