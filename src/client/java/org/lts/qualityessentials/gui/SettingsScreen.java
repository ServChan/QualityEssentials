package org.lts.qualityessentials.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
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
import org.lts.qualityessentials.hidden.HiddenDetails;
import org.lts.qualityessentials.keyboard.KeyboardModule;
import org.lts.qualityessentials.lookat.LookAtHud;
import org.lts.qualityessentials.mount.MountHud;
import org.lts.qualityessentials.newitems.NewItemTracker;
import org.lts.qualityessentials.screenshot.ScreenshotActions;
import org.lts.qualityessentials.shulker.ClientShulkerTooltip;
import org.lts.qualityessentials.shulker.ShulkerPreview;
import org.lts.qualityessentials.shulker.ShulkerTooltip;
import org.lts.qualityessentials.stats.StatsHud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SettingsScreen extends Screen {
    private static final int HEADER_HEIGHT = 26;
    private static final int TOP = HEADER_HEIGHT + 8;
    private static final int CARD_GAP = 2;
    private static final Identifier EFFECT_BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");

    private final Screen parent;
    private Module selected = Module.FOOD;
    private QEConfig working;
    private int sidebarWidth;
    private int sidebarX;
    private int contentX;
    private int contentWidth;
    private int descriptionY;
    private int bannerY;
    private int previewY;
    private int previewHeight;
    private int cardHeight = ModuleCard.HEIGHT;
    private final List<RowLabel> rowLabels = new ArrayList<>();
    private int mouseX;
    private int mouseY;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("qualityessentials.settings.title"));
        this.parent = parent;
        this.working = QE.config().copy();
    }

    @Override
    protected void init() {
        sidebarWidth = Math.max(140, Math.min(190, this.width / 4));
        int layoutWidth = Math.min(this.width - 16, sidebarWidth + 12 + 520);
        sidebarX = (this.width - layoutWidth) / 2;
        contentX = sidebarX + sidebarWidth + 12;
        contentWidth = layoutWidth - sidebarWidth - 12;
        int count = Module.values().length;
        int sidebarSpace = this.height - 34 - TOP;
        cardHeight = ModuleCard.HEIGHT;
        if (count * (cardHeight + CARD_GAP) > sidebarSpace) {
            cardHeight = ModuleCard.COMPACT_HEIGHT;
        }
        if (count * (cardHeight + CARD_GAP) > sidebarSpace) {
            cardHeight = Math.max(ModuleCard.MIN_HEIGHT, sidebarSpace / count - CARD_GAP);
        }
        int y = TOP;
        for (Module module : Module.values()) {
            Module target = module;
            addRenderableWidget(new ModuleCard(sidebarX, y, sidebarWidth, cardHeight, module, icon(module), () -> selected == target, () -> {
                selected = target;
                rebuildWidgets();
            }));
            y += cardHeight + CARD_GAP;
        }

        rowLabels.clear();
        descriptionY = TOP + 18;
        int descriptionLines = Math.min(3, this.font.split(description(), contentWidth).size());
        y = descriptionY + descriptionLines * 10 + 6;
        bannerY = -1;
        if (QE.blocker(selected) != null) {
            bannerY = y;
            y += 22;
        }
        List<Option> options = options(selected);
        int choiceRows = (int) options.stream().filter(option -> option.choices != null).count();
        int toggleRows = options.size() - choiceRows;
        int bottom = this.height - 34;
        previewY = y;
        previewHeight = previewHeight(false);
        int rowHeight = SwitchRow.HEIGHT;
        int columns = 1;
        if (previewY + previewHeight + 6 + rowsHeight(choiceRows, toggleRows, columns, rowHeight) > bottom) {
            rowHeight = SwitchRow.COMPACT_HEIGHT;
        }
        if (previewY + previewHeight + 6 + rowsHeight(choiceRows, toggleRows, columns, rowHeight) > bottom && toggleRows > 1) {
            columns = 2;
        }
        int rowsHeight = rowsHeight(choiceRows, toggleRows, columns, rowHeight);
        if (previewY + previewHeight + 6 + rowsHeight > bottom) {
            previewHeight = Math.max(40, bottom - previewY - 6 - rowsHeight);
        }
        y = previewY + previewHeight + 6;

        Component reset = Component.translatable("qualityessentials.settings.reset");
        int resetWidth = this.font.width(reset) + 16;
        addRenderableWidget(new FlatButton(contentX + contentWidth - resetWidth, TOP, resetWidth, 16, reset, FlatButton.Style.SECONDARY, () -> {
            working.resetModule(selected);
            apply();
        }));
        HudElement hudElement = hudElement(selected);
        if (hudElement != null) {
            Component place = Component.translatable("qualityessentials.settings.layout");
            int placeWidth = this.font.width(place) + 16;
            FlatButton layoutButton = addRenderableWidget(new FlatButton(contentX + contentWidth - resetWidth - placeWidth - 4, TOP, placeWidth, 16, place,
                    FlatButton.Style.PRIMARY, () -> {
                        QE.update(working);
                        this.minecraft.gui.setScreen(new HudEditorScreen(this, hudElement));
                    }));
            layoutButton.active = QE.blocker(selected) == null;
        }
        if (selected == Module.CHAT) {
            Component rules = Component.translatable("qualityessentials.settings.chat.rules");
            int rulesWidth = this.font.width(rules) + 16;
            FlatButton rulesButton = addRenderableWidget(new FlatButton(contentX + contentWidth - resetWidth - rulesWidth - 4, TOP, rulesWidth, 16, rules,
                    FlatButton.Style.SECONDARY, ChatView::openRules));
            rulesButton.active = QE.blocker(selected) == null;
        }
        Component editor = Component.translatable("qualityessentials.editor.open");
        int editorWidth = this.font.width(editor) + 16;
        addRenderableWidget(new FlatButton(this.width - 8 - editorWidth, 5, editorWidth, 16, editor, FlatButton.Style.SECONDARY, () -> {
            QE.update(working);
            this.minecraft.gui.setScreen(new HudEditorScreen(this, null));
        }));
        if (selected == Module.KEYBOARD) {
            Component openLabel = Component.translatable("qualityessentials.keyboard.button.open_short");
            int openWidth = this.font.width(openLabel) + 20;
            FlatButton open = addRenderableWidget(new FlatButton(contentX + contentWidth - openWidth - 8, previewY + previewHeight - 26, openWidth, 20,
                    openLabel, FlatButton.Style.PRIMARY, () -> this.minecraft.gui.setScreen(KeyboardModule.createScreen(this))));
            open.active = QE.active(Module.KEYBOARD);
        }

        String blocker = QE.blocker(selected);
        SwitchRow master = addRenderableWidget(new SwitchRow(contentX, y, contentWidth, rowHeight, Component.translatable("qualityessentials.settings.enabled"),
                QE.active(selected) ? null : ModuleCard.status(selected), () -> working.enabled(selected), () -> {
                    working.setEnabled(selected, !working.enabled(selected));
                    apply();
                }));
        master.active = blocker == null;
        y += rowHeight;
        boolean optionsActive = blocker == null && working.enabled(selected);
        int columnWidth = columns == 1 ? contentWidth : (contentWidth - 6) / 2;
        int column = 0;
        for (Option option : options) {
            if (option.choices != null) {
                if (column > 0) {
                    column = 0;
                    y += rowHeight;
                }
                y = addChoiceRow(option, y, optionsActive, rowHeight);
            } else {
                int rowX = contentX + column * (columnWidth + 6);
                SwitchRow row = addRenderableWidget(new SwitchRow(rowX, y, columnWidth, rowHeight, Component.translatable(option.key),
                        Component.translatable(option.key + ".hint"), () -> option.state.test(working), () -> {
                            option.action.accept(working);
                            apply();
                        }));
                row.active = optionsActive;
                column++;
                if (column == columns) {
                    column = 0;
                    y += rowHeight;
                }
            }
        }
        addRenderableWidget(new FlatButton(this.width / 2 - 75, this.height - 25, 150, 20, Component.translatable("qualityessentials.settings.done"),
                FlatButton.Style.PRIMARY, this::onClose));
    }

    private static int rowsHeight(int choiceRows, int toggleRows, int columns, int rowHeight) {
        return (1 + choiceRows + (toggleRows + columns - 1) / columns) * rowHeight;
    }

    private int previewHeight(boolean compact) {
        compact = compact || this.height < 420;
        return switch (selected) {
            case LOOK_AT -> LookAtHud.height(LookAtHud.preview(working)) + 22;
            case EQUIPMENT -> compact ? 52 : 60;
            case STATS -> statsPreview().height() + 22;
            case MOUNT -> MountHud.size(this.font, MountHud.preview(working))[1] + 26;
            case SHULKER -> ClientShulkerTooltip.height(ShulkerPreview.sample(working)) + 36;
            case HIDDEN_DETAILS -> HiddenDetails.preview(true).size() * 10 + 34;
            case SCREENSHOTS -> screenshotLines().size() * 10 + 30;
            case CHAT -> chatPreview().size() * 10 + 30;
            default -> compact ? 46 : 58;
        };
    }

    private StatsPreview statsPreview() {
        List<StatsHud.Stat> server = StatsHud.sample(working, false);
        List<StatsHud.Stat> single = StatsHud.sample(working, true);
        int[] a = StatsHud.size(this.font, server, working.statsCompact);
        int[] b = StatsHud.size(this.font, single, working.statsCompact);
        int room = contentWidth - 20;
        float scale = Math.min(1f, room / (float) Math.max(a[0], b[0]));
        int aw = Math.round(a[0] * scale);
        int ah = Math.round(a[1] * scale);
        int bh = Math.round(b[1] * scale);
        int x1 = contentX + 10;
        int sideX = Math.max(x1 + aw + 16, contentX + contentWidth / 2 + 4);
        boolean side = sideX + Math.round(b[0] * scale) <= contentX + contentWidth - 8;
        if (side) {
            return new StatsPreview(server, single, scale, x1, 0, sideX, 0, 11 + Math.max(ah, bh));
        }
        int secondY = 11 + ah + 6;
        return new StatsPreview(server, single, scale, x1, 0, x1, secondY, secondY + 11 + bh);
    }

    private List<FormattedCharSequence> screenshotLines() {
        QEConfig config = QE.blocker(Module.SCREENSHOTS) == null && working.enabled(Module.SCREENSHOTS) ? working : disabled(working);
        return this.font.split(ScreenshotActions.sample(config), contentWidth - 28);
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
        int left = contentX + 10;
        return selected == Module.CHAT && mouseX >= left && mouseX <= left + 4 + ChatOverlay.zoneWidth(this.font, working.chatTimestampStyle())
                && mouseY >= previewY && mouseY <= previewY + previewHeight;
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

    private static HudElement hudElement(Module module) {
        for (HudElement element : HudElement.values()) {
            if (element.module() == module) {
                return element;
            }
        }
        return null;
    }

    @Override
    public void added() {
        super.added();
        this.working = QE.config().copy();
    }

    private int addChoiceRow(Option option, int y, boolean active, int rowHeight) {
        rowLabels.add(new RowLabel(option.key, y, active, rowHeight));
        int x = contentX + contentWidth;
        for (int i = option.choices.size() - 1; i >= 0; i--) {
            Choice choice = option.choices.get(i);
            Component label = Component.translatable(choice.key);
            int width = this.font.width(label) + 14;
            x -= width - 1;
            FlatButton button = addRenderableWidget(new FlatButton(x, y + (rowHeight - 16) / 2, width, 16, label, FlatButton.Style.SEGMENT, () -> {
                choice.apply.accept(working);
                apply();
            }).selectedWhen(() -> choice.selected.test(working)));
            button.active = active;
        }
        return y + rowHeight;
    }

    private void apply() {
        QE.update(working);
        working = QE.config().copy();
        rebuildWidgets();
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
            case HIDDEN_DETAILS -> Items.ENCHANTED_BOOK;
            case NEW_ITEMS -> Items.NETHER_STAR;
            case SCREENSHOTS -> Items.PAINTING;
            case CHAT -> Items.WRITABLE_BOOK;
            case DARK_SCREEN -> Items.OBSIDIAN;
            case KEYBOARD -> Items.OAK_SIGN;
        });
    }

    private Component description() {
        return Component.translatable(selected.translationKey() + ".description");
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
            case HIDDEN_DETAILS -> list.add(toggle("qualityessentials.option.hidden_details.hint", c -> c.hiddenDetailsHint,
                    c -> c.hiddenDetailsHint = !c.hiddenDetailsHint));
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
        graphics.fill(0, 0, this.width, this.height, Palette.BACKGROUND);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, Palette.HEADER);
        graphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, Palette.BORDER);
        Ui.scaledText(graphics, this.font, Component.literal("QualityEssentials"), 10, 7, 1.5f, Palette.CYAN, true);
        int subtitleX = 10 + Math.round(this.font.width("QualityEssentials") * 1.5f) + 8;
        graphics.text(this.font, Component.translatable("qualityessentials.settings.subtitle"), subtitleX, 10, Palette.TEXT_DIM, false);
        long running = java.util.Arrays.stream(Module.values()).filter(QE::active).count();
        Component summary = Component.translatable("qualityessentials.settings.summary", running, Module.values().length);
        int pillWidth = this.font.width(summary) + 8;
        int editorWidth = this.font.width(Component.translatable("qualityessentials.editor.open")) + 16;
        Ui.pill(graphics, this.font, summary, this.width - editorWidth - pillWidth - 14, 8, Palette.SURFACE, Palette.TEXT_DIM);

        int sidebarBottom = TOP + Module.values().length * (cardHeight + CARD_GAP);
        graphics.fill(sidebarX + sidebarWidth + 5, TOP, sidebarX + sidebarWidth + 6, Math.max(sidebarBottom, this.height - 34), Palette.DIVIDER);

        Ui.scaledText(graphics, this.font, Component.translatable(selected.translationKey()), contentX, TOP, 1.5f, Palette.TEXT_BRIGHT, true);
        List<FormattedCharSequence> lines = this.font.split(description(), contentWidth);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), contentX, descriptionY + i * 10, Palette.TEXT_DIM, false);
        }
        String blocker = QE.blocker(selected);
        if (blocker != null && bannerY >= 0) {
            Ui.panel(graphics, contentX, bannerY, contentWidth, 18, 0x40FFB74D, Palette.WARNING);
            graphics.text(this.font, Component.translatable("qualityessentials.settings.banner.yielded", QE.modName(blocker)),
                    contentX + 6, bannerY + 5, Palette.WARNING, false);
        }
        drawPreview(graphics);
        for (RowLabel label : rowLabels) {
            graphics.text(this.font, Component.translatable(label.key), contentX + 6, label.y + (label.height - 9) / 2, label.active ? Palette.TEXT_BRIGHT : Palette.TEXT_DISABLED, false);
            graphics.fill(contentX, label.y + label.height - 1, contentX + contentWidth, label.y + label.height, Palette.DIVIDER);
        }
        Component footer = Component.translatable("qualityessentials.settings.footer");
        if (8 + this.font.width(footer) < this.width / 2 - 75 - 8) {
            graphics.text(this.font, footer, 8, this.height - 19, Palette.TEXT_DISABLED, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        Ui.panel(graphics, contentX, previewY, contentWidth, previewHeight, Palette.CASE, Palette.BORDER);
        graphics.text(this.font, Component.translatable("qualityessentials.settings.preview"), contentX + 6, previewY + 4, Palette.TEXT_DISABLED, false);
        boolean live = QE.blocker(selected) == null && working.enabled(selected);
        int cx = contentX + contentWidth / 2;
        int top = previewY + 14;
        int inner = previewHeight - 18;
        graphics.enableScissor(contentX + 1, previewY + 1, contentX + contentWidth - 1, previewY + previewHeight - 1);
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
                float scale = Math.min(1f, Math.min((contentWidth - 20) / (float) size[0], (inner - 2) / (float) size[1]));
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
                statsSample(graphics, "qualityessentials.settings.preview.server", stats.server(), stats.x1(), top + stats.y1(), stats.scale(), opacity);
                statsSample(graphics, "qualityessentials.settings.preview.single", stats.single(), stats.x2(), top + stats.y2(), stats.scale(), opacity);
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
                int w = Math.max(ClientShulkerTooltip.width(this.font, sample), this.font.width(name)) + 8;
                int h = 14 + ClientShulkerTooltip.height(sample) + 2;
                float scale = Math.min(1f, (inner - 2) / (float) h);
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx - w * scale / 2f, top + (inner - h * scale) / 2f);
                graphics.pose().scale(scale, scale);
                Ui.panel(graphics, 0, 0, w, h, 0xF0100010, 0xFF2D0F5C);
                graphics.text(this.font, name, 4, 4, Palette.TEXT_BRIGHT, true);
                ClientShulkerTooltip.draw(graphics, this.font, sample, 4, 14);
                graphics.pose().popMatrix();
                Component caption = Component.translatable("qualityessentials.settings.preview.shulker");
                graphics.text(this.font, caption, contentX + contentWidth - 6 - this.font.width(caption), previewY + 4, Palette.TEXT_DISABLED, false);
            }
            case HIDDEN_DETAILS -> {
                if (!live) {
                    disabledNote(graphics, cx, top + inner / 2 - 4);
                    break;
                }
                boolean shift = this.minecraft.hasShiftDown();
                List<Component> tip = new ArrayList<>(HiddenDetails.preview(shift));
                if (!shift && !working.hiddenDetailsHint) {
                    tip.removeLast();
                }
                int w = 0;
                for (Component line : tip) {
                    w = Math.max(w, this.font.width(line));
                }
                w += 8;
                int h = tip.size() * 10 + 6;
                int x0 = cx - w / 2;
                int y0 = top + (inner - h) / 2;
                Ui.panel(graphics, x0, y0, w, h, 0xF0100010, 0xFF2D0F5C);
                for (int i = 0; i < tip.size(); i++) {
                    graphics.text(this.font, tip.get(i), x0 + 4, y0 + 4 + i * 10, Palette.TEXT_BRIGHT, true);
                }
                Component caption = Component.translatable(shift ? "qualityessentials.settings.preview.hidden.release" : "qualityessentials.settings.preview.hidden.press");
                graphics.text(this.font, caption, contentX + contentWidth - 6 - this.font.width(caption), previewY + 4, Palette.TEXT_DISABLED, false);
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
                    graphics.fill(x, y0, x + 17, y0 + 17, Palette.SURFACE);
                    graphics.outline(x, y0, 17, 17, Palette.BORDER);
                    if (!row[i].isEmpty()) {
                        graphics.item(row[i], x + 1, y0 + 1);
                        graphics.itemDecorations(this.font, row[i], x + 1, y0 + 1);
                    }
                    if (i == 2 || i == 5) {
                        NewItemTracker.star(graphics, x + 1, y0 + 1, alpha);
                    }
                }
                Component note = Component.translatable("qualityessentials.settings.preview.new_items");
                graphics.text(this.font, note, cx - this.font.width(note) / 2, y0 + 21, Palette.TEXT_DIM, false);
            }
            case SCREENSHOTS -> {
                List<FormattedCharSequence> chat = screenshotLines();
                int chatHeight = chat.size() * 10 + 2;
                int y0 = top + (inner - chatHeight) / 2;
                graphics.fill(contentX + 10, y0 - 1, contentX + contentWidth - 10, y0 + chatHeight, 0x80000000);
                for (int i = 0; i < chat.size(); i++) {
                    graphics.text(this.font, chat.get(i), contentX + 14, y0 + 1 + i * 10, 0xFFFFFFFF, true);
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
                graphics.fill(contentX + 10, y0 - 1, contentX + contentWidth - 10, y0 + chatHeight, 0x80000000);
                for (int i = 0; i < chat.size(); i++) {
                    graphics.text(this.font, chat.get(i), contentX + 14, y0 + 1 + i * 10, 0xFFFFFFFF, true);
                }
                if (working.chatFixWords || working.chatFixCommands) {
                    Component caption = Component.translatable("qualityessentials.settings.preview.chat.key",
                            ChatModule.flipKey == null ? Component.literal("Pause") : ChatModule.flipKey.getTranslatedKeyMessage());
                    graphics.text(this.font, caption, contentX + contentWidth - 6 - this.font.width(caption), previewY + 4, Palette.TEXT_DISABLED, false);
                }
            }
            case DARK_SCREEN -> {
                int color = live ? 0xFF000000 | working.darkPreset().rgb() : 0xFFEF323D;
                int w = Math.min(200, contentWidth - 20);
                int h = inner - 4;
                int x = cx - w / 2;
                graphics.fill(x, top, x + w, top + h, color);
                graphics.outline(x, top, w, h, Palette.BORDER);
                Component logo = Component.literal("MOJANG STUDIOS");
                graphics.text(this.font, logo, cx - this.font.width(logo) / 2, top + h / 2 - 9, 0xFFFFFFFF, false);
                int barX = x + 30;
                int barW = w - 60;
                graphics.outline(barX, top + h / 2 + 4, barW, 6, 0xFFFFFFFF);
                graphics.fill(barX + 2, top + h / 2 + 6, barX + 2 + (barW - 4) * 2 / 3, top + h / 2 + 8, 0xFFFFFFFF);
            }
            case KEYBOARD -> drawKeyboardSketch(graphics, cx, top, inner);
        }
        graphics.disableScissor();
    }

    private void statsSample(GuiGraphicsExtractor graphics, String caption, List<StatsHud.Stat> stats, int x, int y, float scale, float opacity) {
        graphics.text(this.font, Component.translatable(caption), x, y, Palette.TEXT_DISABLED, false);
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
        graphics.text(this.font, note, cx - this.font.width(note) / 2, y, Palette.TEXT_DISABLED, false);
    }

    private void effectSample(GuiGraphicsExtractor graphics, int x, int y, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            int duration, boolean infinite, int amplifier, QEConfig config) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND, x, y, 24, 24);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect), x + 3, y + 3, 18, 18);
        EffectTimerHud.overlay(graphics, this.font, x, y, duration, infinite, amplifier, config);
    }

    private void drawKeyboardSketch(GuiGraphicsExtractor graphics, int cx, int top, int inner) {
        int key = Math.max(6, Math.min(10, inner / 5));
        int width = 15 * key;
        int x0 = cx - width / 2;
        int[] rowKeys = {14, 14, 13, 12};
        for (int row = 0; row < rowKeys.length; row++) {
            for (int k = 0; k < rowKeys[row]; k++) {
                int x = x0 + k * key + row * key / 3;
                int y = top + row * key;
                boolean conflict = row == 1 && k == 4 || row == 3 && k == 5;
                boolean bound = (k + row) % 3 != 0;
                graphics.fill(x, y, x + key - 1, y + key - 1, conflict ? 0xFF4A1D24 : bound ? 0xFF2A3A50 : 0xFF141C27);
                if (conflict) {
                    graphics.outline(x, y, key - 1, key - 1, Palette.DANGER);
                }
            }
        }
        int spaceY = top + 4 * key;
        graphics.fill(x0 + 4 * key, spaceY, x0 + 10 * key, spaceY + key - 1, 0xFF2A3A50);
        Component hint = Component.translatable("qualityessentials.settings.preview.keyboard",
                KeyboardModule.openKey == null ? Component.literal("F10") : KeyboardModule.openKey.getTranslatedKeyMessage());
        graphics.text(this.font, hint, cx + width / 2 + 10, top + key, Palette.TEXT_DIM, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_DOWN || event.key() == InputConstants.KEY_UP) {
            int step = event.key() == InputConstants.KEY_DOWN ? 1 : -1;
            Module[] modules = Module.values();
            selected = modules[Math.floorMod(selected.ordinal() + step, modules.length)];
            rebuildWidgets();
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

    private record RowLabel(String key, int y, boolean active, int height) {
    }

    private record StatsPreview(List<StatsHud.Stat> server, List<StatsHud.Stat> single, float scale, int x1, int y1, int x2, int y2, int height) {
    }

    private record Choice(String key, Consumer<QEConfig> apply, Predicate<QEConfig> selected) {
    }

    private record Option(String key, Predicate<QEConfig> state, Consumer<QEConfig> action, List<Choice> choices) {
    }
}
