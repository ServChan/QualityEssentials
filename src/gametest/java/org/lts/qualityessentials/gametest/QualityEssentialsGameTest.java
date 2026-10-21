package org.lts.qualityessentials.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.Vec3;
import org.lts.qualityessentials.books.BookFiles;
import org.lts.qualityessentials.books.BookImportScreen;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.gui.CodeButton;
import org.lts.qualityessentials.gui.HudEditorScreen;
import org.lts.qualityessentials.gui.SettingsScreen;
import org.lts.qualityessentials.keyboard.KeyboardModule;
import org.lts.qualityessentials.mixin.AbstractSignEditScreenAccessor;
import org.lts.qualityessentials.mixin.BookEditScreenAccessor;
import org.lts.qualityessentials.shulker.PlacedShulkers;
import org.lts.qualityessentials.text.FormatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class QualityEssentialsGameTest implements FabricClientGameTest {
    private static final char S = FormatCode.SECTION;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.languageCode = "ru_ru";
            client.getLanguageManager().setSelected("ru_ru");
            client.reloadResourcePacks();
        });
        context.waitTicks(5);
        context.waitFor(client -> client.gui.overlay() == null, 2400);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            if (Boolean.getBoolean("qualityessentials.uiOnly")) {
                interfaceShots(context, world);
                return;
            }
            if (Boolean.getBoolean("qualityessentials.shulkersOnly")) {
                view(context, 1280, 720, 2);
                placedShulkers(context, world);
                return;
            }
            view(context, 1280, 720, 2);
            books(context);
            reader(context);
            signs(context, world);
            settings(context);
            view(context, 854, 480, 2);
            context.setScreen(QualityEssentialsGameTest::book);
            context.waitTicks(5);
            shot(context, "20_book_narrow");
            context.setScreen(() -> sign(Minecraft.getInstance(), signPos(Minecraft.getInstance(), 6)));
            context.waitTicks(5);
            shot(context, "21_sign_narrow");
            context.setScreen(() -> null);
        }
    }

    private static Screen book() {
        Minecraft client = Minecraft.getInstance();
        return new BookEditScreen(client.player, new ItemStack(Items.WRITABLE_BOOK), InteractionHand.MAIN_HAND, WritableBookContent.EMPTY);
    }

    private void books(ClientGameTestContext context) {
        context.setScreen(QualityEssentialsGameTest::book);
        context.waitTicks(5);
        shot(context, "01_book_empty");
        context.getInput().typeChars("QualityEssentials book editor. ");
        String sample = sample();
        context.runOnClient(client -> client.keyboardHandler.setClipboard(sample));
        ctrl(context, InputConstants.KEY_V);
        int pages = context.computeOnClient(client -> bookPages(client).size());
        check(pages > 3, "long paste should spread over pages, got " + pages);
        shot(context, "02_book_pasted");
        ctrl(context, InputConstants.KEY_PAGEUP);
        String firstPage = context.computeOnClient(client -> bookPages(client).getFirst());
        check(firstPage.startsWith("QualityEssentials book editor."), "first page keeps typed text: " + firstPage);
        shot(context, "03_book_first_page");
        ctrl(context, InputConstants.KEY_A);
        click(context, widget -> widget instanceof CodeButton && widget.getMessage().getString().equals(I18n.get(FormatCode.DARK_RED.translationKey())));
        String colored = context.computeOnClient(client -> bookPages(client).getFirst());
        check(colored.startsWith(S + "4"), "selection should turn dark red: " + colored);
        shot(context, "04_book_colored");
        ctrl(context, InputConstants.KEY_Z);
        String undone = context.computeOnClient(client -> bookPages(client).getFirst());
        check(undone.equals(firstPage), "undo should restore the page");
        ctrl(context, InputConstants.KEY_RETURN);
        int afterBreak = context.computeOnClient(client -> bookPages(client).size());
        check(afterBreak == pages + 1, "Ctrl+Enter should add a page");
        ctrl(context, InputConstants.KEY_Z);
        long before = countFiles();
        click(context, label("qualityessentials.books.save"));
        context.waitFor(client -> countFiles() > before, 200);
        shot(context, "05_book_saved");
        click(context, label("qualityessentials.books.load"));
        context.waitForScreen(BookImportScreen.class);
        context.waitTicks(10);
        shot(context, "06_import");
        click(context, label("qualityessentials.books.import.replace"));
        context.waitForScreen(BookEditScreen.class);
        context.waitTicks(3);
        int loaded = context.computeOnClient(client -> bookPages(client).size());
        check(loaded == pages, "replace from file should restore " + pages + " pages, got " + loaded);
        shot(context, "07_book_loaded");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
    }

    private void reader(ClientGameTestContext context) {
        List<Component> pages = List.of(Component.literal(S + "4" + S + "lГлава" + S + "r\nОбычный текст и " + S + "2зелёный."),
                Component.literal("Вторая страница"), Component.literal("Третья"));
        context.setScreen(() -> new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        context.waitTicks(5);
        shot(context, "08_reader");
        ctrl(context, InputConstants.KEY_C);
        String copied = context.computeOnClient(client -> client.keyboardHandler.getClipboard());
        check(copied.startsWith(S + "4" + S + "l"), "reader copy keeps codes: " + copied);
        context.getInput().scroll(-1);
        context.waitTicks(3);
        check(context.computeOnClient(client -> ((org.lts.qualityessentials.mixin.BookViewScreenAccessor) client.gui.screen()).qualityessentials$currentPage()) == 1,
                "wheel should turn the page");
        context.getInput().pressKey(InputConstants.KEY_END);
        context.waitTicks(3);
        shot(context, "09_reader_last");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
    }

    private static BlockPos signPos(Minecraft client, int offset) {
        return client.player.blockPosition().offset(offset, 0, 2);
    }

    private static Screen sign(Minecraft client, BlockPos pos) {
        return new SignEditScreen((SignBlockEntity) client.level.getBlockEntity(pos), SignTextSlot.FRONT, false);
    }

    private void signs(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockPos first = context.computeOnClient(client -> signPos(client, 0));
        BlockPos second = context.computeOnClient(client -> signPos(client, 3));
        BlockPos third = context.computeOnClient(client -> signPos(client, 6));
        for (BlockPos pos : List.of(first, second, third)) {
            world.getServer().runCommand("setblock " + pos.getX() + " " + (pos.getY() - 1) + " " + pos.getZ() + " minecraft:stone");
            world.getServer().runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " minecraft:oak_sign");
        }
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> client.level.getBlockEntity(third) instanceof SignBlockEntity, 100);
        context.setScreen(() -> sign(Minecraft.getInstance(), first));
        context.waitTicks(5);
        context.getInput().typeChars("&a&lМагазин");
        context.getInput().pressKey(InputConstants.KEY_TAB);
        context.getInput().typeChars("&7Алмазы");
        context.waitTicks(2);
        shot(context, "10_sign");
        context.runOnClient(client -> client.keyboardHandler.setClipboard("Первая\nВторая\nТретья"));
        context.getInput().holdControl();
        context.getInput().holdShift();
        context.getInput().pressKey(InputConstants.KEY_V);
        context.getInput().releaseShift();
        context.getInput().releaseControl();
        context.waitTicks(3);
        String[] lines = context.computeOnClient(client -> signLines(client));
        check(List.of(lines).equals(List.of("Первая", "Вторая", "Третья", "")), "whole-sign paste: " + List.of(lines));
        shot(context, "11_sign_pasted");
        ctrl(context, InputConstants.KEY_Z);
        lines = context.computeOnClient(client -> signLines(client));
        check(lines[0].equals("&a&lМагазин"), "sign undo: " + List.of(lines));
        click(context, label("qualityessentials.signs.copy"));
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
        world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:oak_sign");
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> client.player.getMainHandItem().is(Items.OAK_SIGN), 100);
        context.setScreen(() -> sign(Minecraft.getInstance(), second));
        context.waitTicks(5);
        lines = context.computeOnClient(client -> signLines(client));
        check(lines[0].equals("&a&lМагазин") && lines[1].equals("&7Алмазы"), "new sign should be filled: " + List.of(lines));
        shot(context, "12_sign_autofilled");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
    }

    private void placedShulkers(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
        int x = feet.getX();
        int y = feet.getY();
        int z = feet.getZ();
        world.getServer().runCommand("fill " + (x - 3) + " " + y + " " + (z - 3) + " " + (x + 3) + " " + (y + 3) + " " + (z + 3) + " minecraft:air");
        world.getServer().runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z - 3) + " " + (x + 3) + " " + (y - 1) + " " + (z + 3) + " minecraft:stone");
        world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:light_blue_shulker_box[container=["
                + "{slot:0,item:{id:\"minecraft:diamond\",count:24}},{slot:1,item:{id:\"minecraft:iron_ingot\",count:64}},"
                + "{slot:13,item:{id:\"minecraft:ender_pearl\",count:16}}],custom_name=\"Склад\"]");
        BlockPos placed = new BlockPos(x, y, z + 2);
        BlockPos unknown = new BlockPos(x + 2, y, z + 2);
        world.getServer().runCommand("setblock " + unknown.getX() + " " + unknown.getY() + " " + unknown.getZ()
                + " minecraft:red_shulker_box{Items:[{Slot:4b,id:\"minecraft:emerald\",count:5}]}");
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> id(client.player.getMainHandItem().getItem()).equals("light_blue_shulker_box"), 100);
        context.waitFor(client -> id(client.level.getBlockState(unknown).getBlock()).equals("red_shulker_box"), 100);
        look(context, new Vec3(x + 0.5, y, z + 2.5));
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitFor(client -> id(client.level.getBlockState(placed).getBlock()).equals("light_blue_shulker_box"), 100);
        context.waitTicks(3);
        check(context.computeOnClient(client -> PlacedShulkers.lookup(client.level, placed) != null), "placed box should be remembered");
        look(context, Vec3.atCenterOf(placed));
        shot(context, "30_placed_shulker_hint");
        context.getInput().holdShift();
        context.waitTicks(3);
        shot(context, "31_placed_shulker_shift");
        look(context, Vec3.atCenterOf(unknown));
        context.waitTicks(3);
        check(context.computeOnClient(client -> PlacedShulkers.lookup(client.level, unknown) == null), "box placed by a command should be unknown");
        shot(context, "32_unknown_shulker_shift");
        context.getInput().releaseShift();
        context.waitTicks(2);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitFor(client -> client.gui.screen() instanceof ShulkerBoxScreen, 100);
        context.waitTicks(5);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
        boolean emerald = context.computeOnClient(client -> {
            var entry = PlacedShulkers.lookup(client.level, unknown);
            return entry != null && entry.contents().items().get(4).is(Items.EMERALD) && entry.contents().items().get(4).getCount() == 5;
        });
        check(emerald, "opened box should remember its emeralds");
        context.getInput().holdShift();
        context.waitTicks(3);
        shot(context, "33_opened_shulker_shift");
        context.getInput().releaseShift();
        world.getServer().runCommand("setblock " + placed.getX() + " " + placed.getY() + " " + placed.getZ() + " minecraft:air");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(45);
        world.getServer().runCommand("setblock " + placed.getX() + " " + placed.getY() + " " + placed.getZ() + " minecraft:light_blue_shulker_box");
        world.getConnection().waitForClientboundPackets();
        context.waitFor(client -> id(client.level.getBlockState(placed).getBlock()).equals("light_blue_shulker_box"), 100);
        check(context.computeOnClient(client -> PlacedShulkers.lookup(client.level, placed) == null), "broken box should be forgotten");
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static void look(ClientGameTestContext context, Vec3 target) {
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, target));
        context.waitTicks(3);
    }

    private void settings(ClientGameTestContext context) {
        context.setScreen(() -> new SettingsScreen(null));
        context.waitTicks(5);
        for (int i = 0; i < 11; i++) {
            context.getInput().pressKey(InputConstants.KEY_DOWN);
        }
        context.waitTicks(3);
        shot(context, "13_settings_books");
        context.getInput().pressKey(InputConstants.KEY_DOWN);
        context.waitTicks(3);
        shot(context, "14_settings_signs");
        context.setScreen(() -> null);
    }

    private void interfaceShots(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> {
            QEConfig config = QE.config().copy();
            for (Module module : Module.values()) {
                config.setEnabled(module, module != Module.SCREENSHOTS && module != Module.DARK_SCREEN);
            }
            QE.update(config);
            client.player.setXRot(55f);
        });
        world.getServer().runCommand("item replace entity @p armor.chest with minecraft:diamond_chestplate");
        world.getServer().runCommand("item replace entity @p armor.head with minecraft:iron_helmet");
        world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_pickaxe");
        view(context, 1280, 720, 2);
        context.setScreen(() -> null);
        context.waitTicks(20);
        shot(context, "ui_00_hud");
        Module[] modules = {Module.FOOD, Module.LOOK_AT, Module.STATS, Module.CHAT, Module.ITEM_DETAILS, Module.NEW_ITEMS, Module.KEYBOARD};
        for (Module module : modules) {
            context.setScreen(() -> new SettingsScreen(null, module));
            context.waitTicks(5);
            shot(context, "ui_11_after_" + module.id());
        }
        context.setScreen(() -> new HudEditorScreen(null, HudElement.STATS));
        context.waitTicks(5);
        shot(context, "ui_21_after_hud_editor");
        context.setScreen(() -> KeyboardModule.createScreen(null));
        context.waitTicks(5);
        shot(context, "ui_30_keyboard");
        view(context, 854, 480, 2);
        context.setScreen(() -> new SettingsScreen(null, Module.STATS));
        context.waitTicks(5);
        shot(context, "ui_41_after_stats_narrow");
        context.setScreen(() -> null);
    }

    private static String[] signLines(Minecraft client) {
        return ((AbstractSignEditScreenAccessor) client.gui.screen()).qualityessentials$messages().clone();
    }

    private static List<String> bookPages(Minecraft client) {
        return List.copyOf(((BookEditScreenAccessor) client.gui.screen()).qualityessentials$pages());
    }

    private static long countFiles() {
        Path dir = BookFiles.dir();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        } catch (Exception exception) {
            return 0;
        }
    }

    private static String sample() {
        StringBuilder text = new StringBuilder();
        String[] colors = {"4", "2", "9", "6", "5", "0"};
        for (int i = 0; i < 18; i++) {
            text.append(S).append(colors[i % colors.length]);
            if (i % 4 == 0) {
                text.append(S).append('l');
            }
            text.append("Абзац ").append(i + 1).append(". ").append(S).append('r');
            text.append("Длинный текст сам переходит на следующие страницы, слова не разрываются, а цвет продолжается. ");
            if (i == 9) {
                text.append("\n---\n");
            }
        }
        return text.toString();
    }

    private static Predicate<AbstractWidget> label(String key) {
        return widget -> widget.getMessage().getString().equals(I18n.get(key));
    }

    private static void click(ClientGameTestContext context, Predicate<AbstractWidget> match) {
        double[] at = context.computeOnClient(client -> {
            for (GuiEventListener child : client.gui.screen().children()) {
                if (child instanceof AbstractWidget widget && widget.visible && match.test(widget)) {
                    int scale = client.getWindow().getGuiScale();
                    return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
                }
            }
            throw new AssertionError("Widget not found on " + client.gui.screen().getClass().getSimpleName());
        });
        context.getInput().setCursorPos(at[0], at[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
    }

    private static void ctrl(ClientGameTestContext context, int key) {
        context.getInput().holdControl();
        context.getInput().pressKey(key);
        context.getInput().releaseControl();
        context.waitTicks(3);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void view(ClientGameTestContext context, int width, int height, int guiScale) {
        context.getInput().resizeWindow(width, height);
        context.runOnClient(client -> {
            client.options.guiScale().set(guiScale);
            client.resizeGui();
        });
        context.waitTicks(5);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> client.gui.toastManager().clear());
        context.waitTicks(2);
        context.takeScreenshot(name);
    }

}
