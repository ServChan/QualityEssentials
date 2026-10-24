# QualityEssentials — User Guide

Mod version: 1.1.6 · Minecraft 26.3 · Fabric · client-only

[Русская версия](USER_GUIDE_RU.md)

## Contents

1. [What it is](#what-it-is)
2. [Installation](#installation)
3. [Settings and modules](#settings-and-modules)
4. [HUD layout editor](#hud-layout-editor)
5. [Food](#food)
6. [Look at](#look-at)
7. [Effect timers](#effect-timers)
8. [Armor and tools](#armor-and-tools)
9. [Riding](#riding)
10. [Performance](#performance)
11. [Shulker boxes](#shulker-boxes)
12. [New items](#new-items)
13. [Item details](#item-details)
14. [Screenshots](#screenshots)
15. [Chat](#chat)
16. [Books](#books)
17. [Signs](#signs)
18. [Dark loading screen](#dark-loading-screen)
19. [Keyboard map](#keyboard-map)
20. [Keys](#keys)
21. [Files](#files)
22. [FAQ](#faq)
23. [Building from source](#building-from-source)

## What it is

QualityEssentials (QE) bundles quality-of-life features into one client mod: hunger and saturation, a look-at panel, effect timers, armor durability, a riding panel, FPS and TPS, shulker box contents, a better chat, book and sign editors, a keyboard map and more. There are 15 modules.

- Every module can be turned on and off separately.
- If the original mod with the same feature is installed (AppleSkin or Jade, for example), the QE module turns itself off so you never get two interfaces.
- The code is written from scratch; nothing is copied from the original mods.
- The mod shows only what the client already knows: no mobs through walls, no ore highlighting, no other advantage on a server.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3` or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 into the `mods` folder.
3. Put `quality-essentials-1.1.6.jar` into `mods` (see [Building](#building-from-source)).
4. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) to open the settings from the mod list.

Java 25 is required. The server does not need the mod.

## Settings and modules

Open the settings through **Mod Menu → QualityEssentials** or with the Open QE settings key (unbound by default). The module list is on the left; on the right are the module switch, its description and its settings. The HUD editor and chat Tab rules buttons are there too.

Every module is on by default.

| Module | What it does | Steps aside for |
|---|---|---|
| Food | saturation on the hunger bar, held food preview, food values in tooltips | AppleSkin |
| Look at | a panel at the top: the block or creature you aim at and its properties | Jade, WTHIT |
| Effect timers | time and level on effect icons | Status Effect Timer, Status Effect Bars |
| Armor and tools | armor and item durability, arrows, how many blocks are in the inventory | Armor HUD, Armor Durability HUD |
| Riding | hunger while riding and a mount panel | Better Mount HUD |
| Performance | FPS, ping, TPS, memory, coordinates, time and weather | FPS Display |
| Shulker boxes | box contents as a grid in tooltips and for placed boxes | Shulker Box Tooltip |
| New items | a star on freshly picked up items | Item Highlighter |
| Item details | tooltip lines the server hid, plus technical data | — |
| Screenshots | Open, Folder and Copy path buttons | — |
| Chat | long history, tabs, search, timestamps, layout fix, long messages | Chat Patches |
| Books | colors, pages, long text paste, files | Scribble |
| Signs | copy all lines, mouse, &-code palette, auto fill | — |
| Dark loading screen | a dark background instead of the red flash | Dark Loading Screen |
| Keyboard map | every binding on a keyboard and mouse layout | BindBoard |

## HUD layout editor

The Look at, Armor and tools, Riding and Performance panels can be moved. Open the editor with the HUD editor button in the settings or the HUD layout editor key.

- Drag panels with the mouse: they snap to the edges and the centre of the screen.
- The mouse wheel changes a panel's size.
- Arrow keys move the selected panel by one pixel, by 10 with Shift.
- Hold Alt while dragging to turn snapping off.
- Tab selects the next panel.
- Background opacity is a separate setting.

Positions are stored relative to the nearest edge or the centre, so a panel stays in place at a different resolution.

## Food

- Gold frames over the hunger bar show saturation, the hidden reserve that is used up before hunger.
- Food in your hand flashes on the bar with what it will restore.
- Food tooltips show hunger and saturation.

| Setting | Default |
|---|---|
| Show saturation | on |
| Preview held food | on |
| Food values in tooltip | on |

## Look at

A panel at the top of the screen shows the block or creature you look at:

- its name and source mod;
- crop growth, redstone power and other block properties;
- whether a tool is needed for the block to drop;
- breaking progress;
- a creature's health and armor, a villager's profession, a horse's speed and jump.

| Setting | Default |
|---|---|
| Creature health and armor | on |
| Block states (growth, power) | on |
| Required tool | on |
| Breaking progress | on |
| Mod name | on |

## Effect timers

Time left and level are written right on the effect icons in the corner. The color changes when an effect is about to end.

| Setting | Default |
|---|---|
| Time left | on |
| Effect level | on |

## Armor and tools

The panel shows the durability of armor and held items, arrows for bows and crossbows, and how many of the held block you carry in total. Positions: left centre (default), top left or right of the hotbar.

| Setting | Default | What it does |
|---|---|---|
| Orientation | vertical | a vertical or horizontal row |
| Durability in percent | off | percent instead of a number |
| Arrows for bow and crossbow | on | arrows in the inventory |
| Total in inventory | on | total count of the held block |

## Riding

- Your hunger bar stays on screen while you ride.
- A panel next to the hotbar shows the mount's health, top speed, jump height and current speed.

| Setting | Default |
|---|---|
| Hunger while riding | on |
| Mount panel | on |
| Current speed | on |

## Performance

An overlay with FPS and server data.

- On a server it shows ping and an estimate of server TPS (from how fast game time advances).
- In singleplayer it shows the real TPS and MSPT of the integrated server.
- Coordinates are hidden when the server turns on reduced debug info.
- The time of day comes with the day number, in blue when you can sleep.
- Weather is shown for the player's location.

| Setting | Default |
|---|---|
| Style | compact (one line) or column; compact by default |
| FPS | on |
| 1% low | off |
| Ping | on |
| TPS | on |
| MSPT | off |
| Memory | off |
| Entities | off |
| Coordinates | off |
| Time | on |
| Weather | on |

## Shulker boxes

- Hover a shulker box or another container item and hold **Shift**: the tooltip shows the contents as a grid laid out like the box. The strip on top takes the box color.
- Without Shift there is a hint line with the item count.
- Summary mode merges identical items into one cell.
- **Placed boxes.** Aim at a box in the world and hold Shift: a window shows what it held when you last opened or placed it. The server never sends box contents to the client, so the mod remembers them itself. Boxes you have never opened stay unknown.

| Setting | Default | What it does |
|---|---|---|
| Style | Grid | Grid or Summary |
| Hide the text list | on | removes the vanilla "and N more" list under the grid |
| Contents of placed boxes | on | remember and show the contents of boxes in the world |

Up to 4096 boxes are remembered, separately for every server and world.

## New items

Freshly picked up items get a small star in the inventory. It disappears once you hover over the item.

| Setting | Default |
|---|---|
| Twinkle | on |

## Item details

Servers often hide part of an item's tooltip with the `tooltip_display` component. With **Shift** held, QE brings the hidden lines back (enchantments, attributes and more) and adds a details block:

- the item ID and model (`item_model`);
- durability with percent, anvil repair cost, rarity, stack size;
- what exactly the server hid;
- server data from `custom_data`, where plugins keep their own item IDs;
- components the server added or removed compared with the normal item.

Without Shift a line "Shift — hidden (N) and details" appears at the bottom when something is hidden.

| Setting | Default |
|---|---|
| Details block | on |
| Server data | on |
| Shift hint | on |

## Screenshots

The chat message about a saved screenshot gets buttons: open the file, open the folder, copy the path. Each can be turned off: Open, Folder, Copy path (all on).

## Chat

### History and restoring after rejoin

- History size: 100, 1,000, 4,096 or 16,384 messages (16,384 by default). The list of sent messages you scroll with the arrow keys has the same limit.
- When you leave a server or a world, the last 1000 messages are saved separately for every server address and every singleplayer world. Next time they are placed in the history above the new messages, separated by a "Chat from last session" line and a grey bar on the left. The restored chat is visible only in the open chat and never pops up on screen.

### Window and timestamps

- Window width and height: Normal, ×1.5, ×2 or Max. In Max mode the vanilla Width and Height sliders in the chat settings stretch to the whole screen.
- Timestamps: Off, `12:30` or `12:30:45`. With Time on hover only (the default) the time slides out on the left while the cursor is at the left edge of the open chat, and takes no space in the lines.
- Identical messages in a row merge into one line with an (×N) mark.

### Tabs

Tabs appear above the input line:

| Tab | What it holds |
|---|---|
| All | the whole chat |
| Talk | player messages and your private messages only |
| Global, Local | messages from the global and local chat (by the channel icon) |
| Private | private messages |
| Joins | players joining and leaving |
| Moderation | anti-cheat, swear filters, kicks and bans, private message spying, CoreProtect output |

- A tab appears once the history has at least one of its messages. The number next to it counts new messages since your last visit.
- Switch with a click or `Ctrl+Tab` / `Ctrl+Shift+Tab`.
- The selected tab also filters the on-screen chat when it is closed, and it is remembered between launches.
- Hidden messages are not lost: they stay in the history and show on their own tab.

**Your own tab rules.** The rules live in `config/qualityessentials/chat_tabs.json` (the Tab rules button opens the file). A group is a set of regular expressions matched against the message text. A tab shows the groups listed in `show` (`*` means every message) and removes the groups in `hide`. The file is reloaded on the fly; errors in expressions go to the log and do not break the other rules. Before matching, text is normalized: small caps and decorative brackets such as `❪ᴠᴜʟᴄᴀɴ❫` read as `(vulcan)`. If an expression uses `\w` or `\W`, add the `(?U)` flag, or Java will not treat Cyrillic letters as letters.

The default rules target the chat format of the server the mod was made on (channels marked with the `ꐙ` and `ꐘ` icons). For another server, adjust the `global` and `local` groups to its format.

### Search and navigation

- `Ctrl+F` (or "Ctrl+F search" above the input) turns the input line into a search: only messages containing that text in the current tab stay, and the arrow keys and wheel go through the matches.
- `Enter` closes the search and scrolls the chat to the message found, highlighted; `Esc` closes the search. The text you typed before the search comes back.
- When the chat is scrolled up, "↓ N new" above the input shows how many messages arrived below; click it to return to the latest.

### Long messages

- The input line takes up to 1280 characters.
- A message longer than 256 characters is sent in parts split by words, 1.5 s apart.
- In `/msg`, `/tell`, `/w`, `/r`, `/me` and similar each part repeats the command and the name.
- Other commands longer than 256 characters are not sent; a note above the input explains why.
- When the text is longer than 216 characters a counter appears above the input.

### Keyboard layout fix

Off by default. Turn on Fix layout in messages and Fix layout in commands. This is for Russian players who type with the wrong layout.

- It works when you send: `ghbdtn` → `привет`, `руддщ` → `hello`, `/ызфцт` and `.ызфцт` → `/spawn`.
- A word changes only when it looks clearly more plausible in the other layout.
- Online player names, links and addresses, words with digits and `_`, color codes such as `&a`, words like `iPhone` and words in the `chatLayoutIgnore` list in `config/qualityessentials.json` are left alone.
- A command is fixed only when the other layout gives a command the client knows.
- The `Pause` key in the open chat converts the selected text or the whole line by hand.

### Chat settings

| Setting | Default |
|---|---|
| Fix layout in messages | off |
| Fix layout in commands | off |
| Chat tabs | on |
| Long messages | on |
| Restore chat on rejoin | on |
| Stack repeated messages | on |
| Time on hover only | on |
| History size | 16,384 |
| Window width | Normal |
| Window height | Normal |
| Timestamps | Off |

## Books

### Colors and styles

- A panel to the left of the book and quill: 16 colors and styles (bold, italic, underlined, strikethrough, obfuscated, plain text).
- With a selection, formatting applies to the selection and leaves the text after it alone; without one, it applies to the text that follows.
- `Ctrl+B`, `Ctrl+I`, `Ctrl+U` toggle styles.
- The editor page shows colors the same way a signed book does, and small colored ticks mark where each code sits. Arrow keys, `Backspace` and `Delete` jump over invisible codes.

A vanilla server keeps `§` codes in a book and quill and turns them into colors when the book is signed. Plugins may strip them; that depends on the server.

### Pages

- "+ New" inserts an empty page after the current one, "Delete" removes the current page.
- "← Left" and "Right →" swap the page with its neighbour.
- `Ctrl+Enter` splits the page at the cursor (formatting carries over).
- `Ctrl+PageUp` and `Ctrl+PageDown` go to the first and last page, the mouse wheel turns pages.
- At most 100 pages and 1024 characters per page; counters on the right show what is left.

### Paste, undo and files

- `Ctrl+V` (and the Paste text button) inserts the clipboard at the cursor, and whatever does not fit continues on new pages: words are not split, and color carries onto the next page. A `---` line starts a new page.
- `§` codes from the clipboard are kept; `&c`, `&l` and the like become formatting when &-codes on paste is on.
- `Ctrl+Z` undoes typing, formatting and page actions; `Ctrl+Y` or `Ctrl+Shift+Z` redoes.
- Save writes the book to `config/qualityessentials/books/<title>_<date>.txt` (pages separated by a `---` line).
- Load opens a list of files from that folder with a preview: Insert at cursor or Replace book. You can put your own UTF-8 texts there too, up to 512 KB.

### Reading

A signed book or a book on a lectern gets buttons on the right: Copy page / whole book (with Shift, without colors), Save, To start, To end. `Ctrl+C` copies the page, `Ctrl+Shift+C` the whole book, `Home` and `End` jump to the start and end.

### Book settings

| Setting | Default | What it does |
|---|---|---|
| Colors and styles | on | the color and style panel |
| Code marks | on | colored ticks where codes sit |
| Pages, clipboard and files | on | page, paste, save and load buttons |
| Long paste | on | spread long text over pages |
| &-codes on paste | off | turn `&c` and the like into formatting |
| Reading buttons | on | buttons on signed books |
| Wheel turns pages | on | the mouse wheel turns pages |
| Titles up to 32 characters | off | lifts the vanilla 15-character limit. Off because server plugins may reject such a title |

## Signs

- **Copy** puts every line on the clipboard (one per line) and remembers them, **Paste** spreads the clipboard lines over the sign, **Clear** wipes everything. `Ctrl+Shift+C` and `Ctrl+Shift+V` do the same from the keyboard, and `Ctrl+V` with several lines on the clipboard fills the current line and the next ones.
- Clicking a line selects it and places the cursor, `Shift`+click selects text; `Tab` and `Shift+Tab` move between lines; `Ctrl+Z` and `Ctrl+Y` undo and redo.
- **&-codes.** A vanilla server strips `§` codes from signs, so the palette on the left inserts `&c`, `&l` and others, which EssentialsX and similar plugins understand. Below the palette you see how the sign will look if the server understands them.
- **Remember and repeat.** A middle click on a sign remembers the text of the side you face. The next sign you place is filled with that text at once; Clear removes it and Paste remembered brings it back. Confirm at once closes such a sign without pressing Done, which is handy for placing many identical signs.

| Setting | Default |
|---|---|
| Copy and paste | on |
| &-code palette | on |
| Click a line | on |
| Remember with middle click | on |
| Fill new signs | on |
| Confirm at once | off |

## Dark loading screen

Removes the red flash on startup and resource pack reloads: the loading background turns dark. Background color: dark blue (default), graphite or black.

## Keyboard map

Opens with `F10` or a button in **Options → Controls → Key Binds**.

- The whole keyboard with numpad and mouse, each key showing its bindings.
- Conflicting keys are highlighted, and a summary at the bottom counts bindings, conflicts and overlaps with modifiers.
- Hold `Ctrl`, `Shift` or `Alt` to see that modifier's layer.
- Click a key to see its bindings. The Change button on a binding waits for a new key: press it on the keyboard or click it on the map. `Esc` cancels.
- The category filter removes bindings of other categories from the map.

The Button in Controls setting (on) adds the keyboard map button to the controls menu.

## Keys

| Action | Default |
|---|---|
| Open keyboard map | `F10` |
| Switch chat keyboard layout (in the open chat) | `Pause` |
| Open QE settings | unbound |
| HUD layout editor | unbound |
| Chat search | `Ctrl+F` in the open chat |
| Next and previous chat tab | `Ctrl+Tab` / `Ctrl+Shift+Tab` |

Keys are set in **Options → Controls**, category QualityEssentials.

## Files

| Path | Contents |
|---|---|
| `config/qualityessentials.json` | every setting, HUD panel positions, the `chatLayoutIgnore` list |
| `config/qualityessentials/chat/` | chat from the last session, per server and world |
| `config/qualityessentials/chat_tabs.json` | chat tab rules |
| `config/qualityessentials/shulkers/` | remembered shulker box contents, per server and world |
| `config/qualityessentials/books/` | saved and loadable books |

## FAQ

**A module does not turn on.** Most likely the original mod with the same feature is installed (AppleSkin for Food, for example). QE steps aside for it, and the module description says so.

**Messages vanished from chat.** Check the tab: every tab except All hides some messages. They are not lost and show on their own tab.

**The Global and Local tabs are empty.** The channel rules target one particular server's chat format. Adjust the `global` and `local` groups in `chat_tabs.json` for your server.

**Book colors disappeared after signing.** A server plugin stripped them. On a vanilla server colors stay.

**`&c` codes show as text on a sign.** The server does not understand them. `&` codes work only with plugins such as EssentialsX.

**A placed shulker box shows nothing.** The mod only knows what you have seen: open the box once and QE remembers it.

**The layout fix changed something it should not.** Add the word to `chatLayoutIgnore` in `config/qualityessentials.json` or turn the fix off.

## Building from source

JDK 25 is required.

```powershell
git clone https://github.com/ServChan/QualityEssentials.git
cd QualityEssentials
.\gradlew.bat build
```

On Linux and macOS use `./gradlew`. The jar is `build/libs/quality-essentials-1.1.6.jar`.

The in-game test (`.\gradlew.bat runClientGameTest`) opens a book, pastes long colored text, formats a selection, undoes, saves and loads a file, and checks book reading, signs and the settings in 1280×720 and 854×480 windows. Screenshots go to `build/run/clientGameTest/screenshots`.

The layout fix model is built by `tools/layout/build_layout_model.py` from the Minecraft language files and the dictionaries `tools/layout/seed_ru.txt` and `seed_en.txt`. Rebuild the model and run the tests after editing the dictionaries.
