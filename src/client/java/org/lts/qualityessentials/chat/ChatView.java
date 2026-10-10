package org.lts.qualityessentials.chat;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.lts.qualityessentials.QualityEssentialsClient;
import org.lts.qualityessentials.chat.tabs.ChatRules;
import org.lts.qualityessentials.chat.tabs.TabRules;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.mixin.ChatComponentAccessor;
import org.lts.qualityessentials.mixin.ChatScreenAccessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatView {
    private static final int ROW_HEIGHT = 11;
    private static final int GAP = 2;
    private static final int SEARCH_TEXT = 0xFFFFE08A;
    private static final int TEXT = 0xFFAAAAAA;
    private static final int TEXT_ACTIVE = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF808080;
    private static final int UNREAD = 0xFFFFD54F;
    private static final int WARNING = 0xFFFF6B6B;
    private static final int ACCENT = 0xFF4FC3F7;
    private static final int BACKGROUND = 0x90000000;
    private static final int BACKGROUND_HOVER = 0xC0303030;
    private static final int BACKGROUND_ACTIVE = 0xE0202428;
    private static final int FLASH = 0x50FFE08A;
    private static final long FLASH_MILLIS = 2500L;
    private static final long WARNING_MILLIS = 3000L;
    private static final int CHECK_TICKS = 40;

    private static final Map<GuiMessage, Long> MASKS = new MapMaker().weakKeys().makeMap();
    private static volatile TabRules rules = ChatRules.compile(ChatRules.defaults(), problem -> { });
    private static volatile boolean rulesChanged;
    private static volatile long rulesStamp = Long.MIN_VALUE;
    private static int checkIn;
    private static int tab = -1;
    private static int[] unread = new int[0];
    private static boolean[] present = new boolean[0];
    private static boolean presentDirty = true;
    private static boolean searching;
    private static String stash = "";
    private static String needle = "";
    private static int newBelow;
    private static GuiMessage flash;
    private static long flashUntil;
    private static Component warning;
    private static long warningUntil;

    private ChatView() {
    }

    public static Path rulesFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("qualityessentials").resolve("chat_tabs.json");
    }

    public static void openRules() {
        ChatSessions.io().execute(() -> {
            Path file = rulesFile();
            try {
                ChatRules.read(file);
                Blaze3D.openPath(file);
            } catch (Exception exception) {
                QualityEssentialsClient.LOGGER.warn("Failed to open {}", file, exception);
            }
        });
    }

    private static void reload() {
        Path file = rulesFile();
        long stamp = stamp(file);
        if (stamp == rulesStamp) {
            return;
        }
        rulesStamp = stamp;
        try {
            ChatRules.FileData data = ChatRules.read(file);
            rules = ChatRules.compile(data, problem -> QualityEssentialsClient.LOGGER.warn("chat_tabs.json: {}", problem));
            rulesStamp = stamp(file);
            rulesChanged = true;
        } catch (Exception exception) {
            QualityEssentialsClient.LOGGER.warn("Failed to read {}", file, exception);
        }
    }

    private static long stamp(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        } catch (Exception exception) {
            return -1L;
        }
    }

    static void tick(Minecraft minecraft) {
        if (--checkIn <= 0) {
            checkIn = CHECK_TICKS;
            ChatSessions.io().execute(ChatView::reload);
        }
        if (rulesChanged) {
            rulesChanged = false;
            MASKS.clear();
            tab = -1;
            unread = new int[rules.size()];
            present = new boolean[rules.size()];
            presentDirty = true;
            refresh(minecraft);
        }
        if (searching && minecraft.gui.screen() instanceof ChatScreen screen) {
            String query = input(screen).getValue().strip().toLowerCase(Locale.ROOT);
            if (!query.equals(needle)) {
                needle = query;
                refresh(minecraft);
            }
        }
    }

    private static EditBox input(ChatScreen screen) {
        return ((ChatScreenAccessor) screen).qualityessentials$input();
    }

    private static boolean tabsOn() {
        return ChatModule.active() && ChatModule.config().chatTabs && rules.size() > 1;
    }

    private static int currentTab() {
        TabRules current = rules;
        if (tab < 0 || tab >= current.size()) {
            tab = current.indexOf(ChatModule.config().chatTab);
        }
        return tab;
    }

    private static long mask(GuiMessage message) {
        TabRules current = rules;
        return MASKS.computeIfAbsent(message, key -> current.classify(key.content().getString()));
    }

    public static boolean visible(GuiMessage message) {
        if (!ChatModule.active()) {
            return true;
        }
        boolean separator = ChatDecor.isSeparator(message);
        if (searching && !needle.isEmpty()) {
            return !separator && message.content().getString().toLowerCase(Locale.ROOT).contains(needle);
        }
        return separator || !tabsOn() || rules.visible(currentTab(), mask(message));
    }

    public static void onAdded(GuiMessage message, int scroll, boolean focused) {
        if (!ChatModule.active()) {
            return;
        }
        if (tabsOn()) {
            long mask = mask(message);
            int current = currentTab();
            ensureArrays();
            for (int i = 0; i < rules.size(); i++) {
                if (rules.visible(i, mask)) {
                    present[i] = true;
                    if (i != current) {
                        unread[i]++;
                    }
                }
            }
        }
        if (focused && scroll > 0 && visible(message)) {
            newBelow++;
        }
    }

    private static void ensureArrays() {
        if (unread.length != rules.size()) {
            unread = Arrays.copyOf(unread, rules.size());
            present = Arrays.copyOf(present, rules.size());
            presentDirty = true;
        }
    }

    private static void refresh(Minecraft minecraft) {
        if (minecraft.level != null) {
            minecraft.gui.hud.getChat().rescaleChat();
        }
        newBelow = 0;
    }

    private static void select(Minecraft minecraft, int index) {
        if (index == currentTab()) {
            return;
        }
        tab = index;
        ensureArrays();
        unread[index] = 0;
        QEConfig updated = QE.config().copy();
        updated.chatTab = rules.tab(index).id();
        QE.update(updated);
        refresh(minecraft);
    }

    private static void cycle(Minecraft minecraft, int step) {
        List<Integer> shown = shownTabs(minecraft);
        if (shown.size() < 2) {
            return;
        }
        int position = Math.max(0, shown.indexOf(currentTab()));
        select(minecraft, shown.get(Math.floorMod(position + step, shown.size())));
    }

    private static List<Integer> shownTabs(Minecraft minecraft) {
        ensureArrays();
        if (presentDirty) {
            presentDirty = false;
            Arrays.fill(present, false);
            for (GuiMessage message : ((ChatComponentAccessor) minecraft.gui.hud.getChat()).qualityessentials$allMessages()) {
                long mask = mask(message);
                for (int i = 0; i < rules.size(); i++) {
                    if (!present[i] && rules.visible(i, mask)) {
                        present[i] = true;
                    }
                }
            }
        }
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.alwaysPresent(i) || present[i] || i == currentTab()) {
                shown.add(i);
            }
        }
        return shown;
    }

    public static void onOpen(EditBox input) {
        presentDirty = true;
        if (ChatModule.active() && ChatModule.config().chatLongMessages) {
            input.setMaxLength(MessageSplitter.INPUT_LIMIT);
        }
    }

    public static void onClose(EditBox input) {
        if (searching) {
            stopSearch(Minecraft.getInstance(), input, false);
        }
    }

    public static boolean searching() {
        return searching;
    }

    private static void startSearch(Minecraft minecraft, EditBox input) {
        stash = input.getValue();
        searching = true;
        needle = "";
        input.setValue("");
        input.setTextColor(SEARCH_TEXT);
    }

    private static void stopSearch(Minecraft minecraft, EditBox input, boolean jump) {
        GuiMessage target = jump && !needle.isEmpty() ? bottomMessage(minecraft) : null;
        searching = false;
        needle = "";
        input.setValue(stash);
        input.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
        stash = "";
        refresh(minecraft);
        if (target != null) {
            scrollTo(minecraft, target);
        }
    }

    private static GuiMessage bottomMessage(Minecraft minecraft) {
        ChatComponentAccessor chat = (ChatComponentAccessor) minecraft.gui.hud.getChat();
        List<GuiMessage.Line> lines = chat.qualityessentials$trimmedMessages();
        int position = chat.qualityessentials$chatScrollbarPos();
        return position < lines.size() ? lines.get(position).parent() : null;
    }

    private static void scrollTo(Minecraft minecraft, GuiMessage target) {
        ChatComponent chat = minecraft.gui.hud.getChat();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).qualityessentials$trimmedMessages();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).parent() == target) {
                chat.scrollChat(Math.max(0, i - 3));
                flash = target;
                flashUntil = System.currentTimeMillis() + FLASH_MILLIS;
                return;
            }
        }
    }

    public static GuiMessage flashing() {
        if (flash != null && System.currentTimeMillis() > flashUntil) {
            flash = null;
        }
        return flash;
    }

    public static int flashColor() {
        return FLASH;
    }

    public static boolean keyPressed(EditBox input, KeyEvent event) {
        if (!ChatModule.active()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (event.shortcutKey() == 'f' && event.hasControlDownWithQuirk() && !event.hasShiftDown() && !event.hasAltDown()) {
            if (searching) {
                stopSearch(minecraft, input, false);
            } else {
                startSearch(minecraft, input);
            }
            return true;
        }
        if (searching) {
            if (event.isEscape()) {
                stopSearch(minecraft, input, false);
                return true;
            }
            if (event.isConfirmation()) {
                stopSearch(minecraft, input, true);
                return true;
            }
            if (event.isUp() || event.isDown()) {
                minecraft.gui.hud.getChat().scrollChat(event.isUp() ? 1 : -1);
                return true;
            }
            return false;
        }
        if (event.isCycleFocus() && event.hasControlDown() && tabsOn()) {
            cycle(minecraft, event.hasShiftDown() ? -1 : 1);
            return true;
        }
        if (event.isConfirmation()) {
            String value = input.getValue();
            if (value.strip().length() > MessageSplitter.LIMIT
                    && (!ChatModule.config().chatLongMessages || MessageSplitter.split(value.strip(), MessageSplitter.LIMIT).isEmpty())) {
                warning = Component.translatable("qualityessentials.chat.long.blocked", MessageSplitter.LIMIT);
                warningUntil = System.currentTimeMillis() + WARNING_MILLIS;
                return true;
            }
        }
        return false;
    }

    public static boolean mouseClicked(MouseButtonEvent event, int width, int height, EditBox input) {
        if (!ChatModule.active() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        for (Item item : layout(minecraft, minecraft.font, width, height, input)) {
            if (item.contains(event.x(), event.y(), height)) {
                switch (item.kind) {
                    case TAB -> select(minecraft, item.index);
                    case NEW -> {
                        minecraft.gui.hud.getChat().resetChatScroll();
                        newBelow = 0;
                    }
                    case SEARCH -> {
                        if (searching) {
                            stopSearch(minecraft, input, false);
                        } else {
                            startSearch(minecraft, input);
                        }
                    }
                    default -> {
                    }
                }
                return item.kind != Kind.INFO;
            }
        }
        return false;
    }

    public static void extractRow(GuiGraphicsExtractor graphics, int width, int height, int mouseX, int mouseY, EditBox input) {
        if (!ChatModule.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int top = height - 14 - ROW_HEIGHT - GAP;
        for (Item item : layout(minecraft, font, width, height, input)) {
            boolean hover = item.kind != Kind.INFO && item.contains(mouseX, mouseY, height);
            int background = item.active ? BACKGROUND_ACTIVE : hover ? BACKGROUND_HOVER : BACKGROUND;
            graphics.fill(item.x, top, item.x + item.width, top + ROW_HEIGHT, background);
            if (item.active) {
                graphics.fill(item.x, top + ROW_HEIGHT - 1, item.x + item.width, top + ROW_HEIGHT, ACCENT);
            }
            graphics.text(font, item.label, item.x + 4, top + 2, item.color, false);
        }
    }

    private static List<Item> layout(Minecraft minecraft, Font font, int width, int height, EditBox input) {
        List<Item> items = new ArrayList<>();
        int x = 2;
        if (tabsOn()) {
            int current = currentTab();
            for (int index : shownTabs(minecraft)) {
                Component name = tabName(rules.tab(index));
                int count = index == current || index >= unread.length ? 0 : unread[index];
                Component label = count > 0
                        ? name.copy().append(Component.literal(" " + (count > 99 ? "99+" : count)).withColor(UNREAD))
                        : name;
                Item item = new Item(Kind.TAB, index, x, font.width(label) + 8, label, index == current ? TEXT_ACTIVE : TEXT, index == current);
                items.add(item);
                x += item.width + 1;
            }
            x += 4;
        }
        ChatComponentAccessor chat = (ChatComponentAccessor) minecraft.gui.hud.getChat();
        if (chat.qualityessentials$chatScrollbarPos() > 0) {
            Component label = newBelow > 0
                    ? Component.translatable("qualityessentials.chat.new_below", newBelow)
                    : Component.translatable("qualityessentials.chat.to_bottom");
            items.add(new Item(Kind.NEW, 0, x, font.width(label) + 8, label, newBelow > 0 ? UNREAD : TEXT, false));
        } else {
            newBelow = 0;
        }
        Component right = rightLabel(minecraft, input);
        if (right != null) {
            int status = rightColor(input);
            int rightWidth = font.width(right) + 8;
            items.add(new Item(status == TEXT_DIM || searching ? Kind.SEARCH : Kind.INFO, 0, width - 2 - rightWidth, rightWidth, right, status, searching));
        }
        return items;
    }

    private static Component rightLabel(Minecraft minecraft, EditBox input) {
        if (searching) {
            if (needle.isEmpty()) {
                return Component.translatable("qualityessentials.chat.search.prompt");
            }
            return Component.translatable("qualityessentials.chat.search.found", countFound(minecraft));
        }
        if (warning != null && System.currentTimeMillis() < warningUntil) {
            return warning;
        }
        int length = input.getValue().strip().length();
        if (length > MessageSplitter.LIMIT) {
            List<String> parts = ChatModule.config().chatLongMessages ? MessageSplitter.split(input.getValue().strip(), MessageSplitter.LIMIT) : List.of();
            return parts.isEmpty()
                    ? Component.translatable("qualityessentials.chat.long.too_long", length, MessageSplitter.LIMIT)
                    : Component.translatable("qualityessentials.chat.long.parts", length, parts.size());
        }
        if (length > MessageSplitter.LIMIT - 40) {
            return Component.literal(length + "/" + MessageSplitter.LIMIT);
        }
        if (ChatModule.queued() > 0) {
            return Component.translatable("qualityessentials.chat.long.sending", ChatModule.queued());
        }
        return Component.translatable("qualityessentials.chat.search.hint");
    }

    private static int rightColor(EditBox input) {
        if (searching) {
            return SEARCH_TEXT;
        }
        if (warning != null && System.currentTimeMillis() < warningUntil) {
            return WARNING;
        }
        int length = input.getValue().strip().length();
        if (length > MessageSplitter.LIMIT) {
            return ChatModule.config().chatLongMessages && !MessageSplitter.split(input.getValue().strip(), MessageSplitter.LIMIT).isEmpty()
                    ? UNREAD : WARNING;
        }
        return length > MessageSplitter.LIMIT - 40 ? TEXT : TEXT_DIM;
    }

    private static int countFound(Minecraft minecraft) {
        int count = 0;
        for (GuiMessage.Line line : ((ChatComponentAccessor) minecraft.gui.hud.getChat()).qualityessentials$trimmedMessages()) {
            if (line.endOfEntry()) {
                count++;
            }
        }
        return count;
    }

    private static Component tabName(TabRules.Tab tab) {
        if (tab.name() != null && !tab.name().isBlank()) {
            return Component.literal(tab.name());
        }
        String key = "qualityessentials.chat.tab." + tab.id();
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(tab.id());
    }

    private enum Kind {
        TAB,
        NEW,
        SEARCH,
        INFO
    }

    private record Item(Kind kind, int index, int x, int width, Component label, int color, boolean active) {
        boolean contains(double mouseX, double mouseY, int height) {
            int top = height - 14 - ROW_HEIGHT - GAP;
            return mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < top + ROW_HEIGHT;
        }
    }
}
