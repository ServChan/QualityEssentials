package org.lts.qualityessentials.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lts.qualityessentials.books.BookFiles;
import org.lts.qualityessentials.chat.ChatDecor;
import org.lts.qualityessentials.chat.ChatModule;
import org.lts.qualityessentials.chat.ChatOverlay;
import org.lts.qualityessentials.chat.ChatView;
import org.lts.qualityessentials.core.HudElement;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.core.QEConfig;
import org.lts.qualityessentials.effects.EffectTimerHud;
import org.lts.qualityessentials.equipment.EquipmentHud;
import org.lts.qualityessentials.food.FoodHud;
import org.lts.qualityessentials.hidden.ItemDetails;
import org.lts.qualityessentials.keyboard.KeyboardModule;
import org.lts.qualityessentials.lookat.LookAtHud;
import org.lts.qualityessentials.mount.MountHud;
import org.lts.qualityessentials.newitems.NewItemTracker;
import org.lts.qualityessentials.screenshot.ScreenshotActions;
import org.lts.qualityessentials.shulker.ClientShulkerTooltip;
import org.lts.qualityessentials.shulker.ShulkerPreview;
import org.lts.qualityessentials.shulker.ShulkerTooltip;
import org.lts.qualityessentials.stats.StatsHud;
import org.lts.qualityessentials.text.FormatCode;
import org.lts.qualityessentials.text.FormatCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SettingsScreen extends Screen {
    private static final int HEADER = 33;
    private static final int FOOTER = 33;
    private static final int PANE_GAP = 10;
    private static final int SWITCH_WIDTH = 44;
    private static final int ROW = 24;
    private static final int COLUMN_GAP = 12;
    private static final int TWO_COLUMNS_FROM = 360;
    private static final Identifier EFFECT_BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");

    private final Screen parent;
    private Module selected;
    private QEConfig working;
    private ModuleList modules;
    private DetailPane details;
    private Module detailsFor;
    private PreviewRow previewRow;
    private int switchColumns = 1;
    private final List<Runnable> refreshers = new ArrayList<>();
    private int px;
    private int py;
    private int pw;
    private int ph;
    private int mouseX;
    private int mouseY;

    public SettingsScreen(Screen parent) {
        this(parent, Module.FOOD);
    }

    public SettingsScreen(Screen parent, Module module) {
        super(Component.translatable("qualityessentials.settings.title"));
        this.parent = parent;
        this.selected = module;
        this.working = QE.config().copy();
    }

    @Override
    public void added() {
        super.added();
        this.working = QE.config().copy();
    }

    @Override
    protected void init() {
        int bodyHeight = this.height - HEADER - FOOTER;
        int listWidth = Mth.clamp(this.width * 3 / 10, 116, 172);
        int paneWidth = Math.min(this.width - 12 - listWidth - PANE_GAP, 430);
        int left = (this.width - listWidth - PANE_GAP - paneWidth) / 2;
        int rowHeight = Mth.clamp((bodyHeight - 4) / Module.values().length, 18, 22);
        if (modules == null || modules.rowHeight() != rowHeight) {
            modules = new ModuleList(this.minecraft, rowHeight);
        }
        modules.updateSizeAndPosition(listWidth, bodyHeight, left, HEADER);
        modules.select(selected);
        addRenderableWidget(modules);

        double keep = selected == detailsFor && details != null ? details.scrollAmount() : 0;
        details = new DetailPane(this.minecraft, paneWidth, bodyHeight, left + listWidth + PANE_GAP, HEADER);
        refreshers.clear();
        fill(details);
        details.settle();
        details.setScrollAmount(keep);
        detailsFor = selected;
        addRenderableWidget(details);

        HudElement element = hudElement(selected);
        addRenderableWidget(Button.builder(Component.translatable("qualityessentials.editor.open"), button -> {
            QE.update(working);
            this.minecraft.gui.setScreen(new HudEditorScreen(this, element));
        }).bounds(this.width / 2 - 155, this.height - 27, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("qualityessentials.settings.done"), button -> onClose())
                .bounds(this.width / 2 + 5, this.height - 27, 150, 20).build());
    }

    private void fill(DetailPane pane) {
        int inner = pane.innerWidth();
        pane.add(new TitleRow(), QE.blocker(selected) != null ? ROW + 12 : ROW);
        pw = inner;
        int preview = previewHeight();
        previewRow = new PreviewRow();
        pane.add(previewRow, preview + 6);
        List<FormattedCharSequence> lines = this.font.split(Component.translatable(selected.translationKey() + ".description"), inner);
        pane.add(new TextRow(lines), lines.size() * 10 + 8);

        List<Option> options = options(selected);
        switchColumns = columnsFor(options, inner);
        List<Option> pending = new ArrayList<>();
        for (Option option : options) {
            if (option.choices != null) {
                flushSwitches(pane, pending);
                pane.add(new ChoiceRow(option), ROW);
            } else {
                pending.add(option);
            }
        }
        flushSwitches(pane, pending);

        List<Button> actions = new ArrayList<>();
        if (selected == Module.KEYBOARD) {
            Button open = Button.builder(Component.translatable("qualityessentials.keyboard.button.open_short"),
                    button -> this.minecraft.gui.setScreen(KeyboardModule.createScreen(this))).build();
            refreshers.add(() -> open.active = QE.active(Module.KEYBOARD));
            actions.add(open);
        }
        if (selected == Module.CHAT) {
            Button rules = Button.builder(Component.translatable("qualityessentials.settings.chat.rules"), button -> ChatView.openRules()).build();
            refreshers.add(() -> rules.active = QE.blocker(Module.CHAT) == null);
            actions.add(rules);
        }
        if (selected == Module.BOOKS) {
            actions.add(Button.builder(Component.translatable("qualityessentials.settings.books.folder"), button -> BookFiles.openFolder()).build());
        }
        actions.add(Button.builder(Component.translatable("qualityessentials.settings.reset"), button -> {
            working.resetModule(selected);
            QE.update(working);
            working = QE.config().copy();
            rebuildWidgets();
        }).build());
        pane.add(new ActionsRow(actions), ROW + 8);
        refresh();
    }

    private void flushSwitches(DetailPane pane, List<Option> pending) {
        for (int i = 0; i < pending.size(); i += switchColumns) {
            pane.add(new SwitchRow(pending.subList(i, Math.min(pending.size(), i + switchColumns))), ROW);
        }
        pending.clear();
    }

    private int columnsFor(List<Option> options, int inner) {
        if (inner < TWO_COLUMNS_FROM) {
            return 1;
        }
        int room = (inner - COLUMN_GAP) / 2 - SWITCH_WIDTH - 6;
        for (Option option : options) {
            if (option.choices == null && this.font.width(Component.translatable(option.key)) > room) {
                return 1;
            }
        }
        return 2;
    }

    private boolean optionsActive() {
        return QE.blocker(selected) == null && working.enabled(selected);
    }

    private void commit() {
        QE.update(working);
        working = QE.config().copy();
        refresh();
        if (previewRow != null && details != null) {
            int height = previewHeight() + 6;
            if (previewRow.getHeight() != height) {
                previewRow.setHeight(height);
                details.settle();
            }
        }
    }

    private void refresh() {
        for (Runnable refresher : refreshers) {
            refresher.run();
        }
    }

    private void pick(Module module) {
        if (module != selected) {
            selected = module;
            rebuildWidgets();
        }
    }

    public static Component status(Module module) {
        String blocker = QE.blocker(module);
        if (blocker != null) {
            return Component.translatable("qualityessentials.settings.card.yielded", QE.modName(blocker));
        }
        return Component.translatable(QE.active(module) ? "qualityessentials.settings.card.on" : "qualityessentials.settings.card.off");
    }

    private static HudElement hudElement(Module module) {
        for (HudElement element : HudElement.values()) {
            if (element.module() == module) {
                return element;
            }
        }
        return null;
    }

    private static ItemStack icon(Module module) {
        return new ItemStack(switch (module) {
            case FOOD -> Items.COOKED_BEEF;
            case LOOK_AT -> Items.SPYGLASS;
            case EFFECTS -> Items.BREWING_STAND;
            case EQUIPMENT -> Items.DIAMOND_CHESTPLATE;
            case MOUNT -> Items.SADDLE;
            case STATS -> Items.CLOCK;
            case SHULKER -> Items.SHULKER_BOX;
            case ITEM_DETAILS -> Items.ENCHANTED_BOOK;
            case NEW_ITEMS -> Items.NETHER_STAR;
            case SCREENSHOTS -> Items.PAINTING;
            case CHAT -> Items.WRITABLE_BOOK;
            case BOOKS -> Items.WRITTEN_BOOK;
            case SIGNS -> Items.CHERRY_SIGN;
            case DARK_SCREEN -> Items.OBSIDIAN;
            case KEYBOARD -> Items.OAK_SIGN;
        });
    }

    private String clip(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
    }

    private List<Option> options(Module module) {
        List<Option> list = new ArrayList<>();
        switch (module) {
            case FOOD -> {
                list.add(toggle("qualityessentials.option.food.saturation", c -> c.foodSaturationOverlay, c -> c.foodSaturationOverlay = !c.foodSaturationOverlay));
                list.add(toggle("qualityessentials.option.food.preview", c -> c.foodHeldPreview, c -> c.foodHeldPreview = !c.foodHeldPreview));
                list.add(toggle("qualityessentials.option.food.tooltip", c -> c.foodTooltip, c -> c.foodTooltip = !c.foodTooltip));
            }
            case LOOK_AT -> {
                list.add(toggle("qualityessentials.option.lookat.health", c -> c.lookAtShowHealth, c -> c.lookAtShowHealth = !c.lookAtShowHealth));
                list.add(toggle("qualityessentials.option.lookat.states", c -> c.lookAtShowStates, c -> c.lookAtShowStates = !c.lookAtShowStates));
                list.add(toggle("qualityessentials.option.lookat.harvest", c -> c.lookAtShowHarvest, c -> c.lookAtShowHarvest = !c.lookAtShowHarvest));
                list.add(toggle("qualityessentials.option.lookat.progress", c -> c.lookAtShowProgress, c -> c.lookAtShowProgress = !c.lookAtShowProgress));
                list.add(toggle("qualityessentials.option.lookat.mod", c -> c.lookAtShowMod, c -> c.lookAtShowMod = !c.lookAtShowMod));
            }
            case EFFECTS -> {
                list.add(toggle("qualityessentials.option.effects.timer", c -> c.effectsShowTimer, c -> c.effectsShowTimer = !c.effectsShowTimer));
                list.add(toggle("qualityessentials.option.effects.level", c -> c.effectsShowLevel, c -> c.effectsShowLevel = !c.effectsShowLevel));
            }
            case EQUIPMENT -> {
                list.add(new Option("qualityessentials.option.equipment.orientation", null, null, List.of(
                        new Choice("qualityessentials.editor.vertical", c -> c.equipmentHorizontal = false, c -> !c.equipmentHorizontal),
                        new Choice("qualityessentials.editor.horizontal", c -> c.equipmentHorizontal = true, c -> c.equipmentHorizontal))));
                list.add(toggle("qualityessentials.option.equipment.percent", c -> c.equipmentShowPercent, c -> c.equipmentShowPercent = !c.equipmentShowPercent));
                list.add(toggle("qualityessentials.option.equipment.ammo", c -> c.equipmentShowAmmo, c -> c.equipmentShowAmmo = !c.equipmentShowAmmo));
                list.add(toggle("qualityessentials.option.equipment.totals", c -> c.equipmentShowStackTotals, c -> c.equipmentShowStackTotals = !c.equipmentShowStackTotals));
            }
            case STATS -> {
                list.add(new Option("qualityessentials.option.stats.style", null, null, List.of(
                        new Choice("qualityessentials.editor.compact", c -> c.statsCompact = true, c -> c.statsCompact),
                        new Choice("qualityessentials.editor.column", c -> c.statsCompact = false, c -> !c.statsCompact))));
                list.add(toggle("qualityessentials.option.stats.fps", c -> c.statsFps, c -> c.statsFps = !c.statsFps));
                list.add(toggle("qualityessentials.option.stats.lows", c -> c.statsLows, c -> c.statsLows = !c.statsLows));
                list.add(toggle("qualityessentials.option.stats.ping", c -> c.statsPing, c -> c.statsPing = !c.statsPing));
                list.add(toggle("qualityessentials.option.stats.tps", c -> c.statsTps, c -> c.statsTps = !c.statsTps));
                list.add(toggle("qualityessentials.option.stats.mspt", c -> c.statsMspt, c -> c.statsMspt = !c.statsMspt));
                list.add(toggle("qualityessentials.option.stats.memory", c -> c.statsMemory, c -> c.statsMemory = !c.statsMemory));
                list.add(toggle("qualityessentials.option.stats.entities", c -> c.statsEntities, c -> c.statsEntities = !c.statsEntities));
                list.add(toggle("qualityessentials.option.stats.coords", c -> c.statsCoordinates, c -> c.statsCoordinates = !c.statsCoordinates));
                list.add(toggle("qualityessentials.option.stats.time", c -> c.statsTime, c -> c.statsTime = !c.statsTime));
                list.add(toggle("qualityessentials.option.stats.weather", c -> c.statsWeather, c -> c.statsWeather = !c.statsWeather));
            }
            case MOUNT -> {
                list.add(toggle("qualityessentials.option.mount.food", c -> c.mountPlayerFood, c -> c.mountPlayerFood = !c.mountPlayerFood));
                list.add(toggle("qualityessentials.option.mount.panel", c -> c.mountPanel, c -> c.mountPanel = !c.mountPanel));
                list.add(toggle("qualityessentials.option.mount.current", c -> c.mountCurrentSpeed, c -> c.mountCurrentSpeed = !c.mountCurrentSpeed));
            }
            case SHULKER -> {
                list.add(new Option("qualityessentials.option.shulker.style", null, null, List.of(
                        new Choice("qualityessentials.option.shulker.grid", c -> c.shulkerCompact = false, c -> !c.shulkerCompact),
                        new Choice("qualityessentials.option.shulker.summary", c -> c.shulkerCompact = true, c -> c.shulkerCompact))));
                list.add(toggle("qualityessentials.option.shulker.hide_text", c -> c.shulkerHideText, c -> c.shulkerHideText = !c.shulkerHideText));
            }
            case ITEM_DETAILS -> {
                list.add(toggle("qualityessentials.option.item_details.block", c -> c.itemDetailsBlock, c -> c.itemDetailsBlock = !c.itemDetailsBlock));
                list.add(toggle("qualityessentials.option.item_details.data", c -> c.itemDetailsData, c -> c.itemDetailsData = !c.itemDetailsData));
                list.add(toggle("qualityessentials.option.item_details.hint", c -> c.itemDetailsHint, c -> c.itemDetailsHint = !c.itemDetailsHint));
            }
            case NEW_ITEMS -> list.add(toggle("qualityessentials.option.new_items.animate", c -> c.newItemsAnimate,
                    c -> c.newItemsAnimate = !c.newItemsAnimate));
            case SCREENSHOTS -> {
                list.add(toggle("qualityessentials.option.screenshots.open", c -> c.screenshotOpen, c -> c.screenshotOpen = !c.screenshotOpen));
                list.add(toggle("qualityessentials.option.screenshots.folder", c -> c.screenshotFolder, c -> c.screenshotFolder = !c.screenshotFolder));
                list.add(toggle("qualityessentials.option.screenshots.copy", c -> c.screenshotCopy, c -> c.screenshotCopy = !c.screenshotCopy));
            }
            case CHAT -> {
                list.add(toggle("qualityessentials.option.chat.fix_words", c -> c.chatFixWords, c -> c.chatFixWords = !c.chatFixWords));
                list.add(toggle("qualityessentials.option.chat.fix_commands", c -> c.chatFixCommands, c -> c.chatFixCommands = !c.chatFixCommands));
                list.add(toggle("qualityessentials.option.chat.tabs", c -> c.chatTabs, c -> c.chatTabs = !c.chatTabs));
                list.add(toggle("qualityessentials.option.chat.long", c -> c.chatLongMessages, c -> c.chatLongMessages = !c.chatLongMessages));
                list.add(toggle("qualityessentials.option.chat.restore", c -> c.chatRestore, c -> c.chatRestore = !c.chatRestore));
                list.add(toggle("qualityessentials.option.chat.collapse", c -> c.chatCollapse, c -> c.chatCollapse = !c.chatCollapse));
                list.add(toggle("qualityessentials.option.chat.timestamps_hover", c -> c.chatTimestampsHover,
                        c -> c.chatTimestampsHover = !c.chatTimestampsHover));
                List<Choice> sizes = new ArrayList<>();
                for (int size : QEConfig.HISTORY_SIZES) {
                    sizes.add(new Choice("qualityessentials.chat.history." + size, c -> c.chatHistorySize = size, c -> c.chatHistorySize == size));
                }
                list.add(new Option("qualityessentials.option.chat.history", null, null, sizes));
                List<Choice> widths = new ArrayList<>();
                List<Choice> heights = new ArrayList<>();
                for (QEConfig.ChatSize size : QEConfig.ChatSize.values()) {
                    String key = "qualityessentials.chat.size." + size.name().toLowerCase(Locale.ROOT);
                    widths.add(new Choice(key, c -> c.chatWidth = size.name(), c -> c.chatWidthSize() == size));
                    heights.add(new Choice(key, c -> c.chatHeight = size.name(), c -> c.chatHeightSize() == size));
                }
                list.add(new Option("qualityessentials.option.chat.width", null, null, widths));
                list.add(new Option("qualityessentials.option.chat.height", null, null, heights));
                List<Choice> stamps = new ArrayList<>();
                for (QEConfig.ChatTimestamps style : QEConfig.ChatTimestamps.values()) {
                    stamps.add(new Choice("qualityessentials.chat.time." + style.name().toLowerCase(Locale.ROOT),
                            c -> c.chatTimestamps = style.name(), c -> c.chatTimestampStyle() == style));
                }
                list.add(new Option("qualityessentials.option.chat.timestamps", null, null, stamps));
            }
            case BOOKS -> {
                list.add(toggle("qualityessentials.option.books.formatting", c -> c.booksFormatting, c -> c.booksFormatting = !c.booksFormatting));
                list.add(toggle("qualityessentials.option.books.marks", c -> c.booksCodeMarks, c -> c.booksCodeMarks = !c.booksCodeMarks));
                list.add(toggle("qualityessentials.option.books.tools", c -> c.booksPageTools, c -> c.booksPageTools = !c.booksPageTools));
                list.add(toggle("qualityessentials.option.books.smart_paste", c -> c.booksSmartPaste, c -> c.booksSmartPaste = !c.booksSmartPaste));
                list.add(toggle("qualityessentials.option.books.ampersand", c -> c.booksAmpersand, c -> c.booksAmpersand = !c.booksAmpersand));
                list.add(toggle("qualityessentials.option.books.reader", c -> c.booksReaderTools, c -> c.booksReaderTools = !c.booksReaderTools));
                list.add(toggle("qualityessentials.option.books.wheel", c -> c.booksWheel, c -> c.booksWheel = !c.booksWheel));
                list.add(toggle("qualityessentials.option.books.long_title", c -> c.booksLongTitle, c -> c.booksLongTitle = !c.booksLongTitle));
            }
            case SIGNS -> {
                list.add(toggle("qualityessentials.option.signs.tools", c -> c.signsTools, c -> c.signsTools = !c.signsTools));
                list.add(toggle("qualityessentials.option.signs.palette", c -> c.signsPalette, c -> c.signsPalette = !c.signsPalette));
                list.add(toggle("qualityessentials.option.signs.mouse", c -> c.signsMouse, c -> c.signsMouse = !c.signsMouse));
                list.add(toggle("qualityessentials.option.signs.remember", c -> c.signsRemember, c -> c.signsRemember = !c.signsRemember));
                list.add(toggle("qualityessentials.option.signs.autofill", c -> c.signsAutoFill, c -> c.signsAutoFill = !c.signsAutoFill));
                list.add(toggle("qualityessentials.option.signs.autoconfirm", c -> c.signsAutoConfirm, c -> c.signsAutoConfirm = !c.signsAutoConfirm));
            }
            case DARK_SCREEN -> {
                List<Choice> presets = new ArrayList<>();
                for (QEConfig.DarkPreset preset : QEConfig.DarkPreset.values()) {
                    presets.add(new Choice("qualityessentials.dark." + preset.name().toLowerCase(Locale.ROOT),
                            c -> c.darkScreenPreset = preset.name(), c -> c.darkPreset() == preset));
                }
                list.add(new Option("qualityessentials.option.dark.color", null, null, presets));
            }
            case KEYBOARD -> list.add(toggle("qualityessentials.option.keyboard.controls", c -> c.keyboardControlsButton,
                    c -> c.keyboardControlsButton = !c.keyboardControlsButton));
        }
        return list;
    }

    private Option toggle(String key, Predicate<QEConfig> state, Consumer<QEConfig> flip) {
        return new Option(key, state, flip, null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        boolean inWorld = this.minecraft.level != null;
        Identifier top = inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR;
        Identifier bottom = inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR;
        graphics.blit(RenderPipelines.GUI_TEXTURED, top, 0, HEADER - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, bottom, 0, this.height - FOOTER, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, (HEADER - 9) / 2, Palette.TEXT_BRIGHT);
    }

    private int previewHeight() {
        boolean compact = this.height < 420;
        return switch (selected) {
            case LOOK_AT -> LookAtHud.height(LookAtHud.preview(working)) + 22;
            case EQUIPMENT -> compact ? 52 : 60;
            case STATS -> statsPreview().height() + 22;
            case MOUNT -> MountHud.size(this.font, MountHud.preview(working))[1] + 26;
            case SHULKER -> ClientShulkerTooltip.height(ShulkerPreview.sample(working)) + 36;
            case ITEM_DETAILS -> compact ? 100 : 130;
            case SCREENSHOTS -> screenshotLines().size() * 10 + 30;
            case CHAT -> chatPreview().size() * 10 + 30;
            case BOOKS -> compact ? 84 : 96;
            case SIGNS -> compact ? 64 : 72;
            case KEYBOARD -> 60;
            case NEW_ITEMS -> compact ? 66 : 72;
            default -> compact ? 46 : 58;
        };
    }

    private StatsPreview statsPreview() {
        List<StatsHud.Stat> server = StatsHud.sample(working, false);
        List<StatsHud.Stat> single = StatsHud.sample(working, true);
        int[] a = StatsHud.size(this.font, server, working.statsCompact);
        int[] b = StatsHud.size(this.font, single, working.statsCompact);
        int room = pw - 20;
        float scale = Math.min(1f, room / (float) Math.max(a[0], b[0]));
        int aw = Math.round(a[0] * scale);
        int ah = Math.round(a[1] * scale);
        int bh = Math.round(b[1] * scale);
        int x1 = 10;
        int sideX = Math.max(x1 + aw + 16, pw / 2 + 4);
        boolean side = sideX + Math.round(b[0] * scale) <= pw - 8;
        if (side) {
            return new StatsPreview(server, single, scale, x1, 0, sideX, 0, 11 + Math.max(ah, bh));
        }
        int secondY = 11 + ah + 6;
        return new StatsPreview(server, single, scale, x1, 0, x1, secondY, secondY + 11 + bh);
    }

    private List<FormattedCharSequence> screenshotLines() {
        QEConfig config = QE.blocker(Module.SCREENSHOTS) == null && working.enabled(Module.SCREENSHOTS) ? working : disabled(working);
        return this.font.split(ScreenshotActions.sample(config), pw - 28);
    }

    private List<Component> chatPreview() {
        List<Component> lines = new ArrayList<>();
        QEConfig.ChatTimestamps style = working.chatTimestampStyle();
        if (ChatOverlay.onHover(working) && !overChatTimes()) {
            style = QEConfig.ChatTimestamps.OFF;
        }
        long now = System.currentTimeMillis();
        lines.add(chatLine(style, now - 90_000L, Component.literal("<Steve> ").append(Component.translatable("qualityessentials.settings.preview.chat.hello")), 1));
        lines.add(chatLine(style, now - 30_000L, Component.translatable("qualityessentials.settings.preview.chat.restart").withStyle(ChatFormatting.YELLOW),
                working.chatCollapse ? 3 : 1));
        if (working.chatFixWords) {
            lines.add(fixLine("ghbdtn? rfr ltkf&", "привет, как дела?"));
        }
        if (working.chatFixCommands) {
            lines.add(fixLine("/ызфцт", "/spawn"));
        }
        return lines;
    }

    private boolean overChatTimes() {
        int left = px + 10;
        return selected == Module.CHAT && mouseX >= left && mouseX <= left + 4 + ChatOverlay.zoneWidth(this.font, working.chatTimestampStyle())
                && mouseY >= py && mouseY <= py + ph;
    }

    private static Component chatLine(QEConfig.ChatTimestamps style, long time, Component text, int count) {
        MutableComponent line = Component.empty();
        if (style != QEConfig.ChatTimestamps.OFF) {
            line.append(Component.literal(ChatDecor.format(style, time) + " ").withStyle(ChatFormatting.DARK_GRAY));
        }
        line.append(text);
        if (count > 1) {
            line.append(Component.literal(" (×" + count + ")").withStyle(ChatFormatting.GRAY));
        }
        return line;
    }

    private static Component fixLine(String typed, String fixed) {
        return Component.literal(typed).withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH)
                .append(Component.literal("  →  ").withStyle(Style.EMPTY.withStrikethrough(false).withColor(ChatFormatting.DARK_GRAY)))
                .append(Component.literal(fixed).withStyle(Style.EMPTY.withStrikethrough(false).withColor(ChatFormatting.GREEN)));
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        boolean inWorld = this.minecraft.level != null;
        graphics.fill(px, py, px + pw, py + ph, Palette.CASE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR, px, py, 0.0F, 0.0F, pw, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR, px, py + ph - 2, 0.0F, 0.0F, pw, 2, 32, 2);
        boolean live = QE.blocker(selected) == null && working.enabled(selected);
        int cx = px + pw / 2;
        int top = py + 14;
        int inner = ph - 18;
        graphics.enableScissor(px, py + 2, px + pw, py + ph - 2);
        switch (selected) {
            case FOOD -> {
                QEConfig config = live ? working : disabled(working);
                FoodHud.preview(graphics, cx + 40, top + inner / 2 - 8, config);
                if (config.foodTooltip) {
                    Component tip = Component.translatable("qualityessentials.food.tooltip", 8, "12.8");
                    int width = this.font.width(tip);
                    graphics.text(this.font, tip, cx - width / 2, top + inner / 2 + 6, 0xFFFFAA00, true);
                }
            }
            case LOOK_AT -> {
                LookAtHud.Info info = LookAtHud.preview(working);
                int width = LookAtHud.measure(this.font, info);
                if (live) {
                    LookAtHud.draw(graphics, this.font, info, cx - width / 2, top + 2, width, working.layout(HudElement.LOOK_AT).opacity);
                } else {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                }
            }
            case EFFECTS -> {
                QEConfig config = live ? working : disabled(working);
                int x = cx - 38;
                int y = top + (inner - 24) / 2;
                effectSample(graphics, x, y, MobEffects.SPEED, 185 * 20, false, 1, config);
                effectSample(graphics, x + 26, y, MobEffects.REGENERATION, 8 * 20, false, 0, config);
                effectSample(graphics, x + 52, y, MobEffects.NIGHT_VISION, 0, true, 0, config);
            }
            case EQUIPMENT -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                List<EquipmentHud.Entry> entries = EquipmentHud.preview(working);
                int[] size = EquipmentHud.size(this.font, entries, working.equipmentHorizontal);
                float scale = Math.min(1f, Math.min((pw - 20) / (float) size[0], (inner - 2) / (float) size[1]));
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx - size[0] * scale / 2f, top + (inner - size[1] * scale) / 2f);
                graphics.pose().scale(scale, scale);
                EquipmentHud.draw(graphics, this.font, entries, working.equipmentHorizontal, working.layout(HudElement.EQUIPMENT).opacity);
                graphics.pose().popMatrix();
            }
            case STATS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                float opacity = working.layout(HudElement.STATS).opacity;
                StatsPreview stats = statsPreview();
                statsSample(graphics, "qualityessentials.settings.preview.server", stats.server(), px + stats.x1(), top + stats.y1(), stats.scale(), opacity);
                statsSample(graphics, "qualityessentials.settings.preview.single", stats.single(), px + stats.x2(), top + stats.y2(), stats.scale(), opacity);
            }
            case MOUNT -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                MountHud.Info info = MountHud.preview(working);
                int[] size = MountHud.size(this.font, info);
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx - size[0] / 2f, top + (inner - size[1]) / 2f);
                MountHud.draw(graphics, this.font, info, working.layout(HudElement.MOUNT).opacity);
                graphics.pose().popMatrix();
            }
            case SHULKER -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                ShulkerTooltip sample = ShulkerPreview.sample(working);
                Component name = Component.translatable("block.minecraft.light_blue_shulker_box");
                int w = Math.max(ClientShulkerTooltip.width(this.font, sample), this.font.width(name));
                int h = 12 + ClientShulkerTooltip.height(sample);
                float scale = Math.min(1f, (inner - 8) / (float) h);
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx - w * scale / 2f, top + 3 + (inner - 6 - h * scale) / 2f);
                graphics.pose().scale(scale, scale);
                TooltipRenderUtil.extractTooltipBackground(graphics, 0, 0, w, h, null);
                graphics.text(this.font, name, 0, 0, Palette.TEXT_BRIGHT, true);
                ClientShulkerTooltip.draw(graphics, this.font, sample, 0, 12);
                graphics.pose().popMatrix();
                caption(graphics, Component.translatable("qualityessentials.settings.preview.shulker"));
            }
            case ITEM_DETAILS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                boolean shift = this.minecraft.hasShiftDown();
                List<Component> tip = ItemDetails.preview(shift, working);
                int w = 0;
                for (Component line : tip) {
                    w = Math.max(w, this.font.width(line));
                }
                int h = tip.size() * 10 - 2;
                float scale = Math.min(1f, Math.min((pw - 28) / (float) w, (inner - 8) / (float) h));
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx - w * scale / 2f, top + 3 + (inner - 6 - h * scale) / 2f);
                graphics.pose().scale(scale, scale);
                TooltipRenderUtil.extractTooltipBackground(graphics, 0, 0, w, h, null);
                for (int i = 0; i < tip.size(); i++) {
                    graphics.text(this.font, tip.get(i), 0, i * 10, Palette.TEXT_BRIGHT, true);
                }
                graphics.pose().popMatrix();
                caption(graphics, Component.translatable(shift ? "qualityessentials.settings.preview.details.release" : "qualityessentials.settings.preview.details.press"));
            }
            case NEW_ITEMS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                ItemStack[] row = {new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.COOKED_BEEF, 23), new ItemStack(Items.RAW_IRON, 9),
                        new ItemStack(Items.TORCH, 41), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 3), new ItemStack(Items.COBBLESTONE, 64),
                        ItemStack.EMPTY, new ItemStack(Items.WATER_BUCKET)};
                int x0 = cx - row.length * 18 / 2;
                int y0 = top + (inner - 18) / 2 - 4;
                float alpha = working.newItemsAnimate ? 0.7f + 0.3f * Ui.pulse(net.minecraft.util.Util.getMillis(), 1400f) : 1f;
                for (int i = 0; i < row.length; i++) {
                    int x = x0 + i * 18;
                    Ui.slot(graphics, x, y0);
                    if (!row[i].isEmpty()) {
                        graphics.item(row[i], x + 1, y0 + 1);
                        graphics.itemDecorations(this.font, row[i], x + 1, y0 + 1);
                    }
                    if (i == 2 || i == 5) {
                        NewItemTracker.star(graphics, x + 1, y0 + 1, alpha);
                    }
                }
                Component note = Component.translatable("qualityessentials.settings.preview.new_items");
                graphics.text(this.font, note, cx - this.font.width(note) / 2, y0 + 22, Palette.TEXT_DIM, false);
            }
            case SCREENSHOTS -> {
                List<FormattedCharSequence> chat = screenshotLines();
                int chatHeight = chat.size() * 10 + 2;
                int y0 = top + (inner - chatHeight) / 2;
                graphics.fill(px + 10, y0 - 1, px + pw - 10, y0 + chatHeight, 0x80000000);
                for (int i = 0; i < chat.size(); i++) {
                    graphics.text(this.font, chat.get(i), px + 14, y0 + 1 + i * 10, 0xFFFFFFFF, true);
                }
            }
            case CHAT -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                List<Component> chat = chatPreview();
                int chatHeight = chat.size() * 10 + 2;
                int y0 = top + (inner - chatHeight) / 2;
                graphics.fill(px + 10, y0 - 1, px + pw - 10, y0 + chatHeight, 0x80000000);
                for (int i = 0; i < chat.size(); i++) {
                    graphics.text(this.font, chat.get(i), px + 14, y0 + 1 + i * 10, 0xFFFFFFFF, true);
                }
                if (working.chatFixWords || working.chatFixCommands) {
                    caption(graphics, Component.translatable("qualityessentials.settings.preview.chat.key",
                            ChatModule.flipKey == null ? Component.literal("Pause") : ChatModule.flipKey.getTranslatedKeyMessage()));
                }
            }
            case DARK_SCREEN -> {
                int color = live ? 0xFF000000 | working.darkPreset().rgb() : 0xFFEF323D;
                int w = Math.min(200, pw - 20);
                int h = inner - 4;
                int x = cx - w / 2;
                graphics.fill(x, top, x + w, top + h, color);
                Component logo = Component.literal("MOJANG STUDIOS");
                graphics.text(this.font, logo, cx - this.font.width(logo) / 2, top + h / 2 - 9, 0xFFFFFFFF, false);
                int barX = x + 30;
                int barW = w - 60;
                graphics.outline(barX, top + h / 2 + 4, barW, 6, 0xFFFFFFFF);
                graphics.fill(barX + 2, top + h / 2 + 6, barX + 2 + (barW - 4) * 2 / 3, top + h / 2 + 8, 0xFFFFFFFF);
            }
            case KEYBOARD -> drawKeyboardSketch(graphics, cx, top, inner);
            case BOOKS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                drawBookSketch(graphics, cx, top, inner);
            }
            case SIGNS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                drawSignSketch(graphics, cx, top, inner);
            }
        }
        graphics.disableScissor();
    }

    private void caption(GuiGraphicsExtractor graphics, Component text) {
        String line = clip(text.getString(), pw - 12);
        graphics.text(this.font, line, px + pw - 6 - this.font.width(line), py + 5, Palette.TEXT_DIM, false);
    }

    private void statsSample(GuiGraphicsExtractor graphics, String caption, List<StatsHud.Stat> stats, int x, int y, float scale, float opacity) {
        graphics.text(this.font, Component.translatable(caption), x, y, Palette.TEXT_DIM, false);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + 11);
        graphics.pose().scale(scale, scale);
        StatsHud.draw(graphics, this.font, stats, working.statsCompact, opacity);
        graphics.pose().popMatrix();
    }

    private static QEConfig disabled(QEConfig config) {
        QEConfig copy = config.copy();
        copy.screenshotOpen = false;
        copy.screenshotFolder = false;
        copy.screenshotCopy = false;
        copy.foodSaturationOverlay = false;
        copy.foodHeldPreview = false;
        copy.foodTooltip = false;
        copy.effectsShowTimer = false;
        copy.effectsShowLevel = false;
        return copy;
    }

    private void disabledNote(GuiGraphicsExtractor graphics, int cx, int y) {
        Component note = Component.translatable("qualityessentials.settings.preview.off");
        graphics.text(this.font, note, cx - this.font.width(note) / 2, y, Palette.TEXT_DIM, false);
    }

    private void effectSample(GuiGraphicsExtractor graphics, int x, int y, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            int duration, boolean infinite, int amplifier, QEConfig config) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND, x, y, 24, 24);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect), x + 3, y + 3, 18, 18);
        EffectTimerHud.overlay(graphics, this.font, x, y, duration, infinite, amplifier, config);
    }

    private void drawKeyboardSketch(GuiGraphicsExtractor graphics, int cx, int top, int inner) {
        int key = Math.max(6, Math.min(9, inner / 5));
        int width = 15 * key;
        Component hint = Component.translatable("qualityessentials.settings.preview.keyboard",
                KeyboardModule.openKey == null ? Component.literal("F10") : KeyboardModule.openKey.getTranslatedKeyMessage());
        int hintWidth = this.font.width(hint);
        boolean beside = width + 12 + hintWidth <= pw - 16;
        int x0 = beside ? cx - (width + 12 + hintWidth) / 2 : cx - width / 2;
        int y0 = top - 4;
        int[] rowKeys = {14, 14, 13, 12};
        for (int row = 0; row < rowKeys.length; row++) {
            for (int k = 0; k < rowKeys[row]; k++) {
                int x = x0 + k * key + row * key / 3;
                int y = y0 + row * key;
                boolean conflict = row == 1 && k == 4 || row == 3 && k == 5;
                boolean bound = (k + row) % 3 != 0;
                int face = conflict ? Palette.CAP_CONFLICT : bound ? Palette.CAP : Palette.CAP_FREE;
                int edge = conflict ? Palette.CAP_CONFLICT_EDGE : bound ? Palette.CAP_EDGE : Palette.CAP_FREE_EDGE;
                graphics.fill(x, y, x + key - 1, y + key - 1, edge);
                graphics.fill(x, y, x + key - 1, y + key - 2, face);
                if (conflict) {
                    graphics.outline(x, y, key - 1, key - 1, Palette.DANGER);
                }
            }
        }
        int spaceY = y0 + 4 * key;
        graphics.fill(x0 + 4 * key, spaceY, x0 + 10 * key, spaceY + key - 1, Palette.CAP_EDGE);
        graphics.fill(x0 + 4 * key, spaceY, x0 + 10 * key, spaceY + key - 2, Palette.CAP);
        if (beside) {
            graphics.text(this.font, hint, x0 + width + 12, y0 + key * 2 - 2, Palette.TEXT_DIM, false);
        }
    }

    private void drawBookSketch(GuiGraphicsExtractor graphics, int cx, int top, int inner) {
        int pageWidth = 150;
        int pageHeight = Math.min(inner - 2, 76);
        int pageX = cx - pageWidth / 2 + 24;
        int y = top + (inner - pageHeight) / 2;
        int swatchX = pageX - 44;
        List<FormatCode> colors = FormatCode.colors();
        for (int i = 0; i < colors.size(); i++) {
            int sx = swatchX + (i % 4) * 9;
            int sy = y + 2 + (i / 4) * 9;
            graphics.fill(sx, sy, sx + 8, sy + 8, 0xFF000000);
            graphics.fill(sx + 1, sy + 1, sx + 7, sy + 7, 0xFF000000 | colors.get(i).rgb());
        }
        graphics.fill(pageX, y, pageX + pageWidth, y + pageHeight, 0xFFF4EAD5);
        graphics.outline(pageX, y, pageWidth, pageHeight, 0xFFB59D74);
        String[] lines = previewLines("qualityessentials.settings.preview.books");
        for (int i = 0; i < lines.length && 6 + i * 10 < pageHeight - 8; i++) {
            graphics.text(this.font, FormatCodes.convert(lines[i], FormatCode.AMPERSAND, FormatCode.SECTION), pageX + 6, y + 6 + i * 10, 0xFF000000, false);
        }
        caption(graphics, Component.translatable("qualityessentials.settings.preview.books.caption"));
    }

    private void drawSignSketch(GuiGraphicsExtractor graphics, int cx, int top, int inner) {
        String[] lines = previewLines("qualityessentials.settings.preview.signs");
        int signWidth = 104;
        int signHeight = Math.min(inner - 2, 46);
        int y = top + (inner - signHeight) / 2;
        int signX = cx - signWidth - 14;
        graphics.fill(signX, y, signX + signWidth, y + signHeight, 0xFFB5874F);
        graphics.outline(signX, y, signWidth, signHeight, 0xFF6B4A26);
        int previewX = cx + 14;
        graphics.fill(previewX, y, previewX + signWidth, y + signHeight, 0x80000000);
        for (int i = 0; i < lines.length && i < 4; i++) {
            int lineY = y + 4 + i * 10;
            graphics.text(this.font, lines[i], signX + (signWidth - this.font.width(lines[i])) / 2, lineY, 0xFF2B1D0E, false);
            String colored = FormatCodes.convert(lines[i], FormatCode.AMPERSAND, FormatCode.SECTION);
            graphics.text(this.font, colored, previewX + (signWidth - this.font.width(colored)) / 2, lineY, Palette.TEXT_BRIGHT, true);
        }
        graphics.text(this.font, ">", cx - this.font.width(">") / 2, y + signHeight / 2 - 4, Palette.TEXT_DIM, false);
        caption(graphics, Component.translatable("qualityessentials.settings.preview.signs.caption"));
    }

    private static String[] previewLines(String key) {
        return Component.translatable(key).getString().split("\\|");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_DOWN || event.key() == InputConstants.KEY_UP) {
            int step = event.key() == InputConstants.KEY_DOWN ? 1 : -1;
            Module[] all = Module.values();
            pick(all[Math.floorMod(selected.ordinal() + step, all.length)]);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private final class ModuleList extends ObjectSelectionList<ModuleEntry> {
        private final int rowHeight;
        private boolean quiet;

        ModuleList(Minecraft minecraft, int rowHeight) {
            super(minecraft, 0, 0, 0, rowHeight);
            this.rowHeight = rowHeight;
            for (Module module : Module.values()) {
                addEntry(new ModuleEntry(module));
            }
        }

        int rowHeight() {
            return rowHeight;
        }

        void select(Module module) {
            quiet = true;
            for (ModuleEntry entry : children()) {
                if (entry.module == module) {
                    setSelected(entry);
                }
            }
            quiet = false;
        }

        @Override
        public void setSelected(ModuleEntry entry) {
            super.setSelected(entry);
            if (!quiet && entry != null) {
                pick(entry.module);
            }
        }

        @Override
        public int getRowLeft() {
            return getX() + 4;
        }

        @Override
        public int getRowWidth() {
            return this.width - (scrollable() ? 18 : 8);
        }

        @Override
        protected int scrollBarX() {
            return getRight() - 10;
        }
    }

    private final class ModuleEntry extends ObjectSelectionList.Entry<ModuleEntry> {
        private final Module module;
        private final ItemStack icon;

        ModuleEntry(Module module) {
            this.module = module;
            this.icon = icon(module);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            boolean on = QE.active(module);
            int x = getContentX();
            graphics.item(icon, x + 1, getContentYMiddle() - 8);
            int textX = x + 21;
            Component full = Component.translatable(module.translationKey());
            String name = clip(full.getString(), getContentRight() - textX - 2);
            Component label = on ? Component.literal(name) : Component.literal(name).withStyle(ChatFormatting.ITALIC);
            graphics.text(SettingsScreen.this.font, label, textX, getContentYMiddle() - 4, on ? Palette.TEXT_BRIGHT : Palette.TEXT_DIM, true);
            boolean clipped = !name.equals(full.getString());
            if (hovered && (clipped || !on)) {
                Component tip = clipped && !on ? full.copy().append("\n").append(status(module)) : clipped ? full : status(module);
                graphics.setTooltipForNextFrame(SettingsScreen.this.font, tip, mouseX, mouseY);
            }
        }

        @Override
        public Component getNarration() {
            return CommonComponents.joinForNarration(Component.translatable(module.translationKey()), status(module));
        }
    }

    private final class DetailPane extends ContainerObjectSelectionList<Row> {
        DetailPane(Minecraft minecraft, int width, int height, int x, int y) {
            super(minecraft, width, height, y, ROW);
            updateSizeAndPosition(width, height, x, y);
        }

        void add(Row row, int height) {
            addEntry(row, height);
        }

        void settle() {
            updateSizeAndPosition(getWidth(), getHeight(), getX(), getY());
        }

        int innerWidth() {
            return this.width - 26;
        }

        @Override
        public int getRowLeft() {
            return getX() + 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - (scrollable() ? 18 : 8) - 4;
        }

        @Override
        protected int scrollBarX() {
            return getRight() - 10;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected List<AbstractWidget> widgets() {
            return List.of();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets();
        }
    }

    private final class TitleRow extends Row {
        private final CycleButton<Boolean> master;

        TitleRow() {
            Component name = Component.translatable(selected.translationKey());
            master = CycleButton.onOffBuilder(working.enabled(selected))
                    .displayOnlyValue()
                    .withTooltip(value -> Tooltip.create(Component.translatable("qualityessentials.settings.enabled")))
                    .withCustomNarration(button -> CommonComponents.joinForNarration(name, button.createDefaultNarrationMessage()))
                    .create(0, 0, SWITCH_WIDTH, 20, name, (button, value) -> {
                        working.setEnabled(selected, value);
                        commit();
                    });
            refreshers.add(() -> {
                master.active = QE.blocker(selected) == null;
                master.setValue(working.enabled(selected));
            });
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return List.of(master);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int x = getContentX();
            int y = getContentY();
            master.setPosition(getContentRight() - SWITCH_WIDTH, y);
            master.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.item(icon(selected), x, y + 2);
            String name = clip(Component.translatable(selected.translationKey()).getString(), getContentRight() - SWITCH_WIDTH - x - 28);
            graphics.text(SettingsScreen.this.font, name, x + 22, y + 6, Palette.TEXT_BRIGHT, true);
            String blocker = QE.blocker(selected);
            if (blocker != null) {
                String warning = clip(Component.translatable("qualityessentials.settings.card.yielded", QE.modName(blocker)).getString(), getContentWidth());
                graphics.text(SettingsScreen.this.font, warning, x, y + 24, Palette.WARNING, true);
                if (mouseY >= y + 22 && mouseY < y + 34 && mouseX >= x && mouseX < getContentRight()) {
                    graphics.setTooltipForNextFrame(SettingsScreen.this.font, Component.translatable("qualityessentials.settings.banner.yielded", QE.modName(blocker)),
                            mouseX, mouseY);
                }
            }
        }
    }

    private final class PreviewRow extends Row {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            px = getContentX();
            py = getContentY();
            pw = getContentWidth();
            ph = getHeight() - 6;
            drawPreview(graphics);
        }
    }

    private final class TextRow extends Row {
        private final List<FormattedCharSequence> lines;

        TextRow(List<FormattedCharSequence> lines) {
            this.lines = lines;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(SettingsScreen.this.font, lines.get(i), getContentX(), getContentY() + 1 + i * 10, Palette.TEXT_DIM, false);
            }
        }
    }

    private final class SwitchRow extends Row {
        private final List<Option> options;
        private final List<AbstractWidget> buttons = new ArrayList<>();

        SwitchRow(List<Option> options) {
            this.options = List.copyOf(options);
            for (Option option : this.options) {
                Component label = Component.translatable(option.key);
                CycleButton<Boolean> button = CycleButton.onOffBuilder(option.state.test(working))
                        .displayOnlyValue()
                        .withTooltip(value -> Tooltip.create(Component.translatable(option.key + ".hint")))
                        .withCustomNarration(cycle -> CommonComponents.joinForNarration(label, cycle.createDefaultNarrationMessage()))
                        .create(0, 0, SWITCH_WIDTH, 20, label, (cycle, value) -> {
                            if (option.state.test(working) != value) {
                                option.action.accept(working);
                            }
                            commit();
                        });
                refreshers.add(() -> {
                    button.active = optionsActive();
                    button.setValue(option.state.test(working));
                });
                buttons.add(button);
            }
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return buttons;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int cell = (getContentWidth() - COLUMN_GAP * (switchColumns - 1)) / switchColumns;
            for (int i = 0; i < options.size(); i++) {
                int x = getContentX() + i * (cell + COLUMN_GAP);
                AbstractWidget button = buttons.get(i);
                button.setPosition(x + cell - SWITCH_WIDTH, getContentY());
                button.extractRenderState(graphics, mouseX, mouseY, a);
                label(graphics, Component.translatable(options.get(i).key), Component.translatable(options.get(i).key + ".hint"),
                        x, getContentY(), cell - SWITCH_WIDTH - 6, button.active, mouseX, mouseY);
            }
        }
    }

    private final class ChoiceRow extends Row {
        private final Option option;
        private final CycleButton<Choice> button;
        private final int buttonWidth;

        ChoiceRow(Option option) {
            this.option = option;
            Component label = Component.translatable(option.key);
            Choice current = option.choices.get(0);
            int widest = 0;
            for (Choice choice : option.choices) {
                widest = Math.max(widest, SettingsScreen.this.font.width(Component.translatable(choice.key)));
                if (choice.selected.test(working)) {
                    current = choice;
                }
            }
            buttonWidth = Math.max(SWITCH_WIDTH + 16, widest + 20);
            button = CycleButton.<Choice>builder(choice -> Component.translatable(choice.key), current)
                    .withValues(option.choices)
                    .displayOnlyValue()
                    .withCustomNarration(cycle -> CommonComponents.joinForNarration(label, cycle.createDefaultNarrationMessage()))
                    .create(0, 0, buttonWidth, 20, label, (cycle, value) -> {
                        value.apply.accept(working);
                        commit();
                    });
            refreshers.add(() -> {
                button.active = optionsActive();
                for (Choice choice : option.choices) {
                    if (choice.selected.test(working)) {
                        button.setValue(choice);
                    }
                }
            });
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return List.of(button);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            button.setPosition(getContentRight() - buttonWidth, getContentY());
            button.extractRenderState(graphics, mouseX, mouseY, a);
            label(graphics, Component.translatable(option.key), null, getContentX(), getContentY(), getContentWidth() - buttonWidth - 6, button.active, mouseX, mouseY);
        }
    }

    private final class ActionsRow extends Row {
        private final List<AbstractWidget> buttons;

        ActionsRow(List<Button> buttons) {
            this.buttons = List.copyOf(buttons);
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return buttons;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int gap = 4;
            int each = Math.min(150, (getContentWidth() - gap * (buttons.size() - 1)) / buttons.size());
            int x = getContentRight() - each * buttons.size() - gap * (buttons.size() - 1);
            for (AbstractWidget button : buttons) {
                button.setWidth(each);
                button.setPosition(x, getContentY() + 6);
                button.extractRenderState(graphics, mouseX, mouseY, a);
                x += each + gap;
            }
        }
    }

    private void label(GuiGraphicsExtractor graphics, Component text, Component hint, int x, int y, int width, boolean active,
            int mouseX, int mouseY) {
        String full = text.getString();
        String shown = clip(full, width);
        graphics.text(this.font, shown, x, y + 6, active ? Palette.TEXT_BRIGHT : Palette.TEXT_DIM, true);
        boolean over = mouseX >= x && mouseX < x + Math.min(width, this.font.width(shown)) && mouseY >= y + 4 && mouseY < y + 16;
        if (over && (hint != null || !shown.equals(full))) {
            graphics.setTooltipForNextFrame(this.font, hint == null ? text : shown.equals(full) ? hint : text.copy().append("\n").append(hint), mouseX, mouseY);
        }
    }

    private record StatsPreview(List<StatsHud.Stat> server, List<StatsHud.Stat> single, float scale, int x1, int y1, int x2, int y2, int height) {
    }

    private record Choice(String key, Consumer<QEConfig> apply, Predicate<QEConfig> selected) {
    }

    private record Option(String key, Predicate<QEConfig> state, Consumer<QEConfig> action, List<Choice> choices) {
    }
}
