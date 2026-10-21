package org.lts.qualityessentials.shulker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lts.qualityessentials.QualityEssentialsClient;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.WorldScope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlacedShulkers {
    public static final int SLOTS = 27;
    private static final String DEFAULT_TITLE = "container.shulkerBox";
    private static final int OPEN_WINDOW_TICKS = 100;
    private static final int PLACE_WINDOW_TICKS = 20;
    private static final int VALIDATE_EVERY = 40;
    private static final int SAVE_EVERY = 600;
    private static final Gson GSON = new GsonBuilder().create();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "QualityEssentials-Shulkers");
        thread.setDaemon(true);
        return thread;
    });

    private static ShulkerMemory<Contents> memory = new ShulkerMemory<>();
    private static ClientPacketListener session;
    private static String scope;
    private static boolean dirty;
    private static int ticks;
    private static Use lastUse;
    private static Placement pending;
    private static int boundMenu = -1;
    private static BlockPos boundPos;

    private PlacedShulkers() {
    }

    public static boolean enabled() {
        return QE.active(Module.SHULKER) && QE.config().shulkerPlaced;
    }

    public static ShulkerMemory.Entry<Contents> lookup(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }
        ShulkerMemory.Entry<Contents> entry = memory.get(dimension(level), pos.asLong());
        if (entry != null && !entry.block().equals(blockId(state.getBlock()))) {
            memory.remove(entry.dimension(), entry.pos());
            dirty = true;
            return null;
        }
        return entry;
    }

    public static void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled() || minecraft.level == null) {
            return;
        }
        BlockPos clicked = hit.getBlockPos();
        if (minecraft.level.getBlockState(clicked).getBlock() instanceof ShulkerBoxBlock) {
            lastUse = new Use(clicked.immutable(), ticks);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock block) {
            BlockPos beside = clicked.relative(hit.getDirection());
            pending = new Placement(block, contentsOf(stack), ticks,
                    List.of(clicked.immutable(), beside.immutable()),
                    List.of(minecraft.level.getBlockState(clicked), minecraft.level.getBlockState(beside)));
        }
    }

    public static void tick(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.level == null || minecraft.player == null) {
            if (connection == null && session != null) {
                onDisconnected();
            }
            return;
        }
        if (connection != session) {
            begin(minecraft, connection);
        }
        ticks++;
        if (!enabled()) {
            return;
        }
        ClientLevel level = minecraft.level;
        checkPlacement(level);
        captureOpenBox(minecraft, level);
        if (ticks % VALIDATE_EVERY == 0) {
            validate(level);
        }
        if (dirty && ticks % SAVE_EVERY == 0) {
            save(minecraft, false);
        }
    }

    public static void onDisconnected() {
        Minecraft minecraft = Minecraft.getInstance();
        if (dirty && scope != null) {
            save(minecraft, !minecraft.isRunning());
        }
        session = null;
        scope = null;
        memory = new ShulkerMemory<>();
        lastUse = null;
        pending = null;
        boundMenu = -1;
        boundPos = null;
    }

    private static void begin(Minecraft minecraft, ClientPacketListener connection) {
        if (session != null && dirty && scope != null) {
            save(minecraft, false);
        }
        session = connection;
        scope = WorldScope.of(minecraft);
        memory = new ShulkerMemory<>();
        dirty = false;
        if (scope == null) {
            return;
        }
        ClientPacketListener expected = connection;
        Path file = WorldScope.file("shulkers", scope);
        CompletableFuture.supplyAsync(() -> read(file), IO).thenAcceptAsync(saved -> {
            if (saved != null && session == expected && minecraft.level != null) {
                restore(minecraft, saved);
            }
        }, minecraft);
    }

    private static void checkPlacement(ClientLevel level) {
        Placement placement = pending;
        if (placement == null) {
            return;
        }
        if (ticks - placement.tick() > PLACE_WINDOW_TICKS) {
            pending = null;
            return;
        }
        for (int i = 0; i < placement.candidates().size(); i++) {
            BlockPos pos = placement.candidates().get(i);
            BlockState now = level.getBlockState(pos);
            if (now.is(placement.block()) && !placement.before().get(i).is(placement.block())) {
                remember(level, pos, placement.block(), placement.contents());
                pending = null;
                return;
            }
        }
    }

    private static void captureOpenBox(Minecraft minecraft, ClientLevel level) {
        if (!(minecraft.gui.screen() instanceof ShulkerBoxScreen screen)) {
            boundMenu = -1;
            boundPos = null;
            return;
        }
        ShulkerBoxMenu menu = screen.getMenu();
        if (menu.containerId != boundMenu) {
            boundMenu = menu.containerId;
            Use use = lastUse;
            boundPos = use != null && ticks - use.tick() <= OPEN_WINDOW_TICKS
                    && level.getBlockState(use.pos()).getBlock() instanceof ShulkerBoxBlock ? use.pos() : null;
            lastUse = null;
        }
        if (boundPos == null) {
            return;
        }
        BlockState state = level.getBlockState(boundPos);
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            boundPos = null;
            return;
        }
        List<ItemStack> items = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            items.add(menu.getSlot(i).getItem().copy());
        }
        Contents contents = new Contents(items, customTitle(screen.getTitle()));
        ShulkerMemory.Entry<Contents> known = memory.get(dimension(level), boundPos.asLong());
        if (known == null || !known.contents().sameAs(contents) || !known.block().equals(blockId(state.getBlock()))) {
            remember(level, boundPos, state.getBlock(), contents);
        }
    }

    private static void validate(ClientLevel level) {
        String dimension = dimension(level);
        for (ShulkerMemory.Entry<Contents> entry : memory.entries()) {
            if (!entry.dimension().equals(dimension)) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.pos());
            if (level.isLoaded(pos) && !blockId(level.getBlockState(pos).getBlock()).equals(entry.block())) {
                memory.remove(entry.dimension(), entry.pos());
                dirty = true;
            }
        }
    }

    private static void remember(ClientLevel level, BlockPos pos, Block block, Contents contents) {
        memory.put(new ShulkerMemory.Entry<>(dimension(level), pos.asLong(), blockId(block), contents, System.currentTimeMillis()));
        dirty = true;
    }

    private static Contents contentsOf(ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(SLOTS);
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            container.itemCopies().forEach(items::add);
        }
        while (items.size() < SLOTS) {
            items.add(ItemStack.EMPTY);
        }
        return new Contents(new ArrayList<>(items.subList(0, SLOTS)), stack.get(DataComponents.CUSTOM_NAME));
    }

    private static Component customTitle(Component title) {
        if (title.getContents() instanceof TranslatableContents translatable && DEFAULT_TITLE.equals(translatable.getKey())) {
            return null;
        }
        return title;
    }

    private static String dimension(ClientLevel level) {
        return level.dimension().identifier().toString();
    }

    private static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static void save(Minecraft minecraft, boolean now) {
        if (scope == null || minecraft.level == null) {
            return;
        }
        dirty = false;
        JsonObject snapshot = snapshot(minecraft);
        Path file = WorldScope.file("shulkers", scope);
        if (now) {
            write(file, snapshot);
        } else {
            IO.execute(() -> write(file, snapshot));
        }
    }

    private static JsonObject snapshot(Minecraft minecraft) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        JsonArray boxes = new JsonArray();
        for (ShulkerMemory.Entry<Contents> entry : memory.entries()) {
            JsonObject box = new JsonObject();
            box.addProperty("dimension", entry.dimension());
            box.addProperty("pos", entry.pos());
            box.addProperty("block", entry.block());
            box.addProperty("seen", entry.seenAt());
            Component name = entry.contents().name();
            if (name != null) {
                ComponentSerialization.CODEC.encodeStart(ops, name).result().ifPresent(json -> box.add("name", json));
            }
            JsonArray items = new JsonArray();
            List<ItemStack> stacks = entry.contents().items();
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                int slot = i;
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(json -> {
                    JsonObject item = new JsonObject();
                    item.addProperty("slot", slot);
                    item.add("item", json);
                    items.add(item);
                });
            }
            box.add("items", items);
            boxes.add(box);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("boxes", boxes);
        return root;
    }

    private static void restore(Minecraft minecraft, JsonArray boxes) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, minecraft.level.registryAccess());
        for (JsonElement element : boxes) {
            try {
                JsonObject box = element.getAsJsonObject();
                List<ItemStack> items = new ArrayList<>(SLOTS);
                for (int i = 0; i < SLOTS; i++) {
                    items.add(ItemStack.EMPTY);
                }
                for (JsonElement itemElement : box.getAsJsonArray("items")) {
                    JsonObject item = itemElement.getAsJsonObject();
                    int slot = item.get("slot").getAsInt();
                    if (slot >= 0 && slot < SLOTS) {
                        ItemStack.CODEC.parse(ops, item.get("item")).result().ifPresent(stack -> items.set(slot, stack));
                    }
                }
                Component name = box.has("name") ? ComponentSerialization.CODEC.parse(ops, box.get("name")).result().orElse(null) : null;
                memory.put(new ShulkerMemory.Entry<>(box.get("dimension").getAsString(), box.get("pos").getAsLong(),
                        box.get("block").getAsString(), new Contents(items, name), box.get("seen").getAsLong()));
            } catch (RuntimeException exception) {
                QualityEssentialsClient.LOGGER.warn("Skipped a remembered shulker box that could not be read: {}", exception.getMessage());
            }
        }
    }

    private static JsonArray read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root instanceof JsonObject object && object.get("boxes") instanceof JsonArray boxes) {
                return boxes;
            }
        } catch (IOException | RuntimeException exception) {
            QualityEssentialsClient.LOGGER.warn("Could not read remembered shulker boxes {}", file, exception);
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
            QualityEssentialsClient.LOGGER.warn("Could not save remembered shulker boxes {}", file, exception);
        }
    }

    public record Contents(List<ItemStack> items, Component name) {
        boolean sameAs(Contents other) {
            if (items.size() != other.items.size() || !Objects.equals(name, other.name)) {
                return false;
            }
            for (int i = 0; i < items.size(); i++) {
                if (!ItemStack.matches(items.get(i), other.items.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private record Use(BlockPos pos, int tick) {
    }

    private record Placement(Block block, Contents contents, int tick, List<BlockPos> candidates, List<BlockState> before) {
    }
}
