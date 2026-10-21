package org.lts.qualityessentials.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QEConfig {
    public static final int CURRENT_VERSION = 3;
    public static final int[] HISTORY_SIZES = {100, 1000, 4096, 16384};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int configVersion = CURRENT_VERSION;
    public Map<String, Boolean> modules = new LinkedHashMap<>();

    public boolean foodSaturationOverlay = true;
    public boolean foodHeldPreview = true;
    public boolean foodTooltip = true;

    public boolean lookAtShowMod = true;
    public boolean lookAtShowHealth = true;
    public boolean lookAtShowStates = true;
    public boolean lookAtShowHarvest = true;
    public boolean lookAtShowProgress = true;

    public boolean effectsShowTimer = true;
    public boolean effectsShowLevel = true;

    public String equipmentAnchor = EquipmentAnchor.LEFT_CENTER.name();
    public boolean equipmentHorizontal = false;
    public boolean equipmentShowPercent = false;
    public boolean equipmentShowAmmo = true;
    public boolean equipmentShowStackTotals = true;

    public String darkScreenPreset = DarkPreset.SLATE.name();

    public boolean keyboardControlsButton = true;

    public boolean statsFps = true;
    public boolean statsLows = false;
    public boolean statsPing = true;
    public boolean statsTps = true;
    public boolean statsMspt = false;
    public boolean statsMemory = false;
    public boolean statsEntities = false;
    public boolean statsCoordinates = false;
    public boolean statsTime = true;
    public boolean statsWeather = true;
    public boolean statsCompact = true;

    public boolean mountPlayerFood = true;
    public boolean mountPanel = true;
    public boolean mountCurrentSpeed = true;

    public boolean shulkerHideText = true;
    public boolean shulkerCompact = false;

    public boolean itemDetailsHint = true;
    public boolean itemDetailsBlock = true;
    public boolean itemDetailsData = true;

    public boolean newItemsAnimate = true;

    public boolean screenshotOpen = true;
    public boolean screenshotFolder = true;
    public boolean screenshotCopy = true;

    public int chatHistorySize = 16384;
    public boolean chatRestore = true;
    public String chatWidth = ChatSize.NORMAL.name();
    public String chatHeight = ChatSize.NORMAL.name();
    public String chatTimestamps = ChatTimestamps.OFF.name();
    public boolean chatTimestampsHover = true;
    public boolean chatCollapse = true;
    public boolean chatFixWords = false;
    public boolean chatFixCommands = false;
    public boolean chatTabs = true;
    public String chatTab = "all";
    public boolean chatLongMessages = true;
    public List<String> chatLayoutIgnore = new ArrayList<>();

    public boolean booksFormatting = true;
    public boolean booksCodeMarks = true;
    public boolean booksPageTools = true;
    public boolean booksSmartPaste = true;
    public boolean booksAmpersand = false;
    public boolean booksReaderTools = true;
    public boolean booksWheel = true;
    public boolean booksLongTitle = false;

    public boolean signsTools = true;
    public boolean signsPalette = true;
    public boolean signsMouse = true;
    public boolean signsRemember = true;
    public boolean signsAutoFill = true;
    public boolean signsAutoConfirm = false;

    public Map<String, HudLayout> layouts = new LinkedHashMap<>();

    public QEConfig() {
        for (Module module : Module.values()) {
            modules.put(module.id(), true);
        }
        for (HudElement element : HudElement.values()) {
            layouts.put(element.id(), element.defaultLayout());
        }
    }

    public HudLayout layout(HudElement element) {
        HudLayout layout = layouts.get(element.id());
        return layout == null ? element.defaultLayout() : layout;
    }

    public void setLayout(HudElement element, HudLayout layout) {
        layouts.put(element.id(), layout.copy().sanitize());
    }

    public boolean enabled(Module module) {
        Boolean value = modules.get(module.id());
        return value == null || value;
    }

    public void setEnabled(Module module, boolean value) {
        modules.put(module.id(), value);
    }

    public EquipmentAnchor anchor() {
        return EquipmentAnchor.parse(equipmentAnchor);
    }

    public DarkPreset darkPreset() {
        return DarkPreset.parse(darkScreenPreset);
    }

    public ChatSize chatWidthSize() {
        return ChatSize.parse(chatWidth);
    }

    public ChatSize chatHeightSize() {
        return ChatSize.parse(chatHeight);
    }

    public ChatTimestamps chatTimestampStyle() {
        return ChatTimestamps.parse(chatTimestamps);
    }

    public void resetModule(Module module) {
        QEConfig defaults = new QEConfig();
        switch (module) {
            case FOOD -> {
                foodSaturationOverlay = defaults.foodSaturationOverlay;
                foodHeldPreview = defaults.foodHeldPreview;
                foodTooltip = defaults.foodTooltip;
            }
            case LOOK_AT -> {
                layouts.put(HudElement.LOOK_AT.id(), HudElement.LOOK_AT.defaultLayout());
                lookAtShowMod = defaults.lookAtShowMod;
                lookAtShowHealth = defaults.lookAtShowHealth;
                lookAtShowStates = defaults.lookAtShowStates;
                lookAtShowHarvest = defaults.lookAtShowHarvest;
                lookAtShowProgress = defaults.lookAtShowProgress;
            }
            case EFFECTS -> {
                effectsShowTimer = defaults.effectsShowTimer;
                effectsShowLevel = defaults.effectsShowLevel;
            }
            case EQUIPMENT -> {
                equipmentAnchor = defaults.equipmentAnchor;
                equipmentHorizontal = defaults.equipmentHorizontal;
                layouts.put(HudElement.EQUIPMENT.id(), HudElement.EQUIPMENT.defaultLayout());
                equipmentShowPercent = defaults.equipmentShowPercent;
                equipmentShowAmmo = defaults.equipmentShowAmmo;
                equipmentShowStackTotals = defaults.equipmentShowStackTotals;
            }
            case DARK_SCREEN -> darkScreenPreset = defaults.darkScreenPreset;
            case KEYBOARD -> keyboardControlsButton = defaults.keyboardControlsButton;
            case STATS -> {
                statsFps = defaults.statsFps;
                statsLows = defaults.statsLows;
                statsPing = defaults.statsPing;
                statsTps = defaults.statsTps;
                statsMspt = defaults.statsMspt;
                statsMemory = defaults.statsMemory;
                statsEntities = defaults.statsEntities;
                statsCoordinates = defaults.statsCoordinates;
                statsTime = defaults.statsTime;
                statsWeather = defaults.statsWeather;
                statsCompact = defaults.statsCompact;
                layouts.put(HudElement.STATS.id(), HudElement.STATS.defaultLayout());
            }
            case MOUNT -> {
                mountPlayerFood = defaults.mountPlayerFood;
                mountPanel = defaults.mountPanel;
                mountCurrentSpeed = defaults.mountCurrentSpeed;
                layouts.put(HudElement.MOUNT.id(), HudElement.MOUNT.defaultLayout());
            }
            case SHULKER -> {
                shulkerHideText = defaults.shulkerHideText;
                shulkerCompact = defaults.shulkerCompact;
            }
            case ITEM_DETAILS -> {
                itemDetailsHint = defaults.itemDetailsHint;
                itemDetailsBlock = defaults.itemDetailsBlock;
                itemDetailsData = defaults.itemDetailsData;
            }
            case NEW_ITEMS -> newItemsAnimate = defaults.newItemsAnimate;
            case SCREENSHOTS -> {
                screenshotOpen = defaults.screenshotOpen;
                screenshotFolder = defaults.screenshotFolder;
                screenshotCopy = defaults.screenshotCopy;
            }
            case CHAT -> {
                chatHistorySize = defaults.chatHistorySize;
                chatRestore = defaults.chatRestore;
                chatWidth = defaults.chatWidth;
                chatHeight = defaults.chatHeight;
                chatTimestamps = defaults.chatTimestamps;
                chatTimestampsHover = defaults.chatTimestampsHover;
                chatCollapse = defaults.chatCollapse;
                chatFixWords = defaults.chatFixWords;
                chatFixCommands = defaults.chatFixCommands;
                chatTabs = defaults.chatTabs;
                chatTab = defaults.chatTab;
                chatLongMessages = defaults.chatLongMessages;
            }
            case BOOKS -> {
                booksFormatting = defaults.booksFormatting;
                booksCodeMarks = defaults.booksCodeMarks;
                booksPageTools = defaults.booksPageTools;
                booksSmartPaste = defaults.booksSmartPaste;
                booksAmpersand = defaults.booksAmpersand;
                booksReaderTools = defaults.booksReaderTools;
                booksWheel = defaults.booksWheel;
                booksLongTitle = defaults.booksLongTitle;
            }
            case SIGNS -> {
                signsTools = defaults.signsTools;
                signsPalette = defaults.signsPalette;
                signsMouse = defaults.signsMouse;
                signsRemember = defaults.signsRemember;
                signsAutoFill = defaults.signsAutoFill;
                signsAutoConfirm = defaults.signsAutoConfirm;
            }
        }
        setEnabled(module, true);
    }

    public QEConfig copy() {
        return GSON.fromJson(GSON.toJson(this), QEConfig.class).sanitize();
    }

    public QEConfig sanitize() {
        if (modules == null) {
            modules = new LinkedHashMap<>();
        }
        Map<String, Boolean> cleaned = new LinkedHashMap<>();
        for (Module module : Module.values()) {
            Boolean value = modules.get(module.id());
            cleaned.put(module.id(), value == null || value);
        }
        modules = cleaned;
        equipmentAnchor = EquipmentAnchor.parse(equipmentAnchor).name();
        darkScreenPreset = DarkPreset.parse(darkScreenPreset).name();
        chatHistorySize = historySize(chatHistorySize);
        chatWidth = ChatSize.parse(chatWidth).name();
        chatHeight = ChatSize.parse(chatHeight).name();
        chatTimestamps = ChatTimestamps.parse(chatTimestamps).name();
        if (chatTab == null || chatTab.isBlank()) {
            chatTab = "all";
        }
        List<String> ignore = new ArrayList<>();
        if (chatLayoutIgnore != null) {
            for (String word : chatLayoutIgnore) {
                if (word != null && !word.isBlank() && !ignore.contains(word.trim())) {
                    ignore.add(word.trim());
                }
            }
        }
        chatLayoutIgnore = ignore;
        if (layouts == null) {
            layouts = new LinkedHashMap<>();
        }
        Map<String, HudLayout> cleanedLayouts = new LinkedHashMap<>();
        for (HudElement element : HudElement.values()) {
            HudLayout layout = layouts.get(element.id());
            cleanedLayouts.put(element.id(), layout == null ? element.defaultLayout() : layout.sanitize());
        }
        layouts = cleanedLayouts;
        if (configVersion < 2) {
            migrateEquipmentAnchor();
        }
        if (configVersion < 3) {
            chatFixWords = false;
            chatFixCommands = false;
        }
        configVersion = CURRENT_VERSION;
        return this;
    }

    private void migrateEquipmentAnchor() {
        switch (EquipmentAnchor.parse(equipmentAnchor)) {
            case HOTBAR_RIGHT -> {
                equipmentHorizontal = true;
                HudLayout layout = HudLayout.of(HudLayout.Anchor.BOTTOM_CENTER, -1, 1, 97, 0);
                layout.opacity = 0.6f;
                layouts.put(HudElement.EQUIPMENT.id(), layout);
            }
            case TOP_LEFT -> {
                HudLayout layout = HudLayout.of(HudLayout.Anchor.TOP_LEFT, -1, -1, 4, 40);
                layout.opacity = 0.6f;
                layouts.put(HudElement.EQUIPMENT.id(), layout);
            }
            default -> {
            }
        }
    }

    public static QEConfig read(Path path) {
        if (!Files.isRegularFile(path)) {
            return new QEConfig().sanitize();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            QEConfig loaded = GSON.fromJson(reader, QEConfig.class);
            return loaded == null ? new QEConfig().sanitize() : loaded.sanitize();
        } catch (Exception exception) {
            Path broken = path.resolveSibling(path.getFileName() + ".broken");
            try {
                Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
            return new QEConfig().sanitize();
        }
    }

    public static boolean write(Path path, QEConfig config) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception exception) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static int historySize(int value) {
        int best = HISTORY_SIZES[0];
        for (int size : HISTORY_SIZES) {
            if (Math.abs(size - value) < Math.abs(best - value)) {
                best = size;
            }
        }
        return best;
    }

    public enum ChatSize {
        NORMAL(1.0),
        WIDE(1.5),
        WIDER(2.0),
        FULL(0.0);

        private final double factor;

        ChatSize(double factor) {
            this.factor = factor;
        }

        public double factor() {
            return factor;
        }

        static ChatSize parse(String value) {
            for (ChatSize size : values()) {
                if (size.name().equalsIgnoreCase(value)) {
                    return size;
                }
            }
            return NORMAL;
        }
    }

    public enum ChatTimestamps {
        OFF(null),
        MINUTES("HH:mm"),
        SECONDS("HH:mm:ss");

        private final String pattern;

        ChatTimestamps(String pattern) {
            this.pattern = pattern;
        }

        public String pattern() {
            return pattern;
        }

        static ChatTimestamps parse(String value) {
            for (ChatTimestamps style : values()) {
                if (style.name().equalsIgnoreCase(value)) {
                    return style;
                }
            }
            return OFF;
        }
    }

    public enum EquipmentAnchor {
        LEFT_CENTER,
        HOTBAR_RIGHT,
        TOP_LEFT;

        static EquipmentAnchor parse(String value) {
            for (EquipmentAnchor anchor : values()) {
                if (anchor.name().equalsIgnoreCase(value)) {
                    return anchor;
                }
            }
            return LEFT_CENTER;
        }

        public EquipmentAnchor next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum DarkPreset {
        SLATE(0x0D141F),
        BLACK(0x000000),
        GRAPHITE(0x1A1C20);

        private final int rgb;

        DarkPreset(int rgb) {
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }

        static DarkPreset parse(String value) {
            for (DarkPreset preset : values()) {
                if (preset.name().equalsIgnoreCase(value)) {
                    return preset;
                }
            }
            return SLATE;
        }

        public DarkPreset next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
