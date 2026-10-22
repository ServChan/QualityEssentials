# QualityEssentials (QE)

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.1.6-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Quality of life in one mod: food, a what-am-I-looking-at panel, effect timers, armor and riding HUDs, performance overlay, shulker previews, a chat section, books, signs and a keyboard map. Every module can be turned off and steps aside when the original mod is installed.

## Русский

### Что это

`QualityEssentials` (mod id `qualityessentials`) — клиентский Fabric-мод для Minecraft 26.3 от LTS_Server: набор удобств в одном моде. Каждый модуль включается и выключается в настройках и сам отключается, если установлен оригинальный мод с той же функцией.

Код написан с нуля, из оригинальных модов ничего не скопировано. Мод показывает только то, что клиент и так знает: никаких мобов сквозь стены и подсветки руд.

### Возможности

| Модуль | Что делает | Уступает место |
|---|---|---|
| Сытость | Золотые рамки насыщения поверх полосы еды, мигающее превью еды в руке, сытость и насыщение в подсказке | AppleSkin |
| Что это | Панель сверху: блок или существо в прицеле, рост, сигнал и другие свойства блока, нужный инструмент, прогресс ломания, здоровье и броня, профессия жителя, скорость и прыжок лошади, мод-источник | Jade, WTHIT |
| Таймеры эффектов | Оставшееся время и уровень на иконках эффектов | Status Effect Timer, Status Effect Bars |
| Броня и инструменты | Прочность брони и предметов в руках, стрелы для лука и арбалета, сколько всего блоков в инвентаре; три положения | Armor HUD, Armor Durability HUD |
| Езда | Полоса сытости игрока не пропадает при езде, панель животного: здоровье, максимальная скорость, высота прыжка, текущая скорость | Better Mount HUD |
| Производительность | Оверлей FPS (и 1% low), пинг и оценка TPS сервера на сервере, настоящие TPS и MSPT в одиночной игре, память, сущности, координаты (скрываются при reduced debug info), время суток с номером дня (синим, когда можно спать) и погода на месте игрока | FPS Display |
| Шалкеры | При зажатом Shift содержимое шалкера и любого предмета-контейнера сеткой в подсказке (без Shift — строка-подсказка с числом предметов), цветная полоска по цвету ящика, режим сводки с объединением одинаковых предметов. Поставленный шалкер: наведите на него и зажмите Shift — окно покажет, что в нём было, когда вы его последний раз открывали или ставили (сервер не присылает клиенту содержимое ящика, поэтому мод запоминает его сам; чужие ящики остаются неизвестными, пока вы их не откроете) | Shulker Box Tooltip |
| Новые предметы | Маленькая звёздочка на только что подобранных предметах в инвентаре, исчезает при наведении | Item Highlighter |
| Подробности о предмете | С зажатым Shift подсказка возвращает строки, которые сервер спрятал компонентом `tooltip_display` (зачарования, атрибуты и прочее), и показывает блок подробностей: ID, модель (`item_model`), прочность с процентами, цену ремонта в наковальне, редкость, размер стака, что скрыто сервером, данные сервера из `custom_data` (там плагины хранят ID своих предметов) и компоненты, которые сервер добавил или убрал. Без Shift внизу строка «Shift — скрытое (N) и подробности», если что-то спрятано | — |
| Скриншоты | Кнопки «Открыть», «Папка» и «Копировать путь» у сообщения о сохранённом скриншоте | — |
| Чат | Длинная история и её возврат после перезахода, большое окно, время сообщений, склейка повторов, исправление раскладки, вкладки, поиск, отправка длинных сообщений частями (подробно ниже) | Chat Patches |
| Книги | Цвета и стили в книге с пером, работа со страницами, вставка длинного текста, отмена, файлы; копирование при чтении (подробно ниже) | Scribble |
| Таблички | Копирование и вставка всех строк, работа мышью и Tab, палитра &-кодов, автозаполнение новой таблички (подробно ниже) | — |
| Тёмный экран загрузки | Тёмный фон вместо красной вспышки при запуске и перезагрузке ресурспаков | Dark Loading Screen |
| Схема клавиатуры | Бывший BindBoard: вся клавиатура с нампадом и мышью, бинды, конфликты, слои Ctrl/Shift/Alt, переназначение | BindBoard |

Панели «Что это», «Броня и инструменты», «Езда» и «Производительность» двигаются в редакторе HUD: перетаскивание с привязкой к краям и центру, колесо меняет размер, отдельно настраивается прозрачность фона. Позиция хранится относительно ближайшего края или центра, поэтому не съезжает при другом разрешении.

#### Чат

Раздел перенесён из Callout: увеличенная история чата и его восстановление после перезахода теперь живут здесь, а Callout оставил только оповещения и историю упоминаний.

- **История.** 100, 1 000, 4 096 или 16 384 сообщений (по умолчанию 16 384). Тот же предел у списка отправленных сообщений, который листается стрелками.
- **Возврат после перезахода.** При выходе с сервера или из мира последние 1000 сообщений сохраняются в `config/qualityessentials/chat/` отдельно для каждого адреса сервера и каждого одиночного мира. При следующем входе они встают в историю над новыми сообщениями, отделены строкой «Чат с прошлого захода» и серой полоской слева. Сохранённый чат виден только в открытом чате и не всплывает на экране.
- **Размер окна.** Ширина и высота: обычная, ×1,5, ×2 или «Максимум». В режиме «Максимум» ванильные ползунки «Ширина» и «Высота» в настройках чата растягиваются на весь экран. Окно не выходит за край экрана и перестраивается при изменении размера окна игры.
- **Время сообщений.** Нет, `12:30` или `12:30:45`. По умолчанию включён режим «только при наведении»: время не занимает места в строках, а выезжает слева, пока курсор стоит у левого края открытого чата: строки плавно сдвигаются вправо и ничего не перекрывается. Если режим выключить, время пишется перед каждым сообщением.
- **Склейка повторов.** Одинаковые сообщения подряд сливаются в одну строку с отметкой (×N).
- **Вкладки.** Над строкой ввода: «Всё»; «Разговоры» — только реплики игроков и ваши личные сообщения (системные строки сервера, смерти, задания, колонии, CoreProtect, античит и слежка за чужими ЛС сюда не попадают); «Глобальный» и «Локальный» — реплики игроков из глобального и локального чата, канал определяется по значку после `[X]` (`ꐙ` — глобальный, `ꐘ` — локальный; значки задаются в группах `global` и `local`); «Личные»; «Входы»; «Модерация» — античит (Vulcan, Grim и другие), SquadFilter, `[Мат]`, `[Защита]`, кики и баны, слежка за чужими ЛС, вывод CoreProtect. Умолчания проверены на 14 тысячах разных строк чата из логов игры: в «Разговоры» попадают все реплики игроков и ни одной служебной строки. Перед проверкой правил текст приводится к простому виду: малые капители и декоративные скобки вроде `❪ᴠᴜʟᴄᴀɴ❫` читаются как `(vulcan)`. Вкладка появляется, когда в истории есть хоть одно её сообщение; число рядом — новые сообщения с момента, когда вы на ней были. Переключение кликом или Ctrl+Tab / Ctrl+Shift+Tab. Выбранная вкладка действует и на чат на экране, когда он закрыт, и запоминается между запусками. Скрытые сообщения не теряются: они остаются в истории и видны на своей вкладке.
- **Правила вкладок** лежат в `config/qualityessentials/chat_tabs.json` (кнопка «Правила вкладок» в настройках открывает файл). Группа — набор регулярных выражений по тексту сообщения, вкладка показывает группы из `show` (`*` — все сообщения) и убирает группы из `hide`. Файл перечитывается на лету, ошибки в выражениях пишутся в лог и не ломают остальные правила. Файл старой версии правил заменяется новыми умолчаниями, старый сохраняется рядом как `chat_tabs.json.v<номер>.bak`. В выражениях используйте флаг `(?U)`, если в них есть `\W` или `\w`: без него Java считает русские буквы «не буквами».
- **Поиск.** Ctrl+F (или «Ctrl+F поиск» справа над вводом) превращает строку ввода в поиск: в чате остаются только сообщения с этим текстом в текущей вкладке, стрелки и колесо листают совпадения, Enter закрывает поиск и прокручивает чат к найденному сообщению с подсветкой, Esc закрывает поиск. Набранный до поиска текст возвращается.
- **«↓ N новых».** Когда чат прокручен вверх, над вводом видно, сколько сообщений пришло ниже; клик возвращает к последним.
- **Длинные сообщения.** Строка ввода принимает до 1280 символов. Сообщение длиннее 256 символов уходит частями по словам с паузой 1,5 с; в `/msg`, `/tell`, `/w`, `/r`, `/me` и похожих каждая часть повторяет команду и ник. Остальные команды длиннее 256 символов не отправляются, над вводом объясняется почему. Пока текст длиннее 216 символов, над вводом виден счётчик.
- **Раскладка.** Автоисправление по умолчанию выключено: включите «Исправлять раскладку в сообщениях» и «Исправлять раскладку в командах» в настройках чата (при обновлении с 1.1.3 и раньше оба переключателя выключаются один раз). Исправление срабатывает при отправке: `ghbdtn` → `привет`, `руддщ` → `hello`, `/ызфцт` и `.ызфцт` → `/spawn`. Слово меняется, только если в другой раскладке оно выглядит заметно правдоподобнее: модель букв и словари частых слов чата в `assets/qualityessentials/layout/`. Не трогаются ники игроков онлайн, ссылки и адреса, слова с цифрами и `_`, цветовые коды `&a`, слова вида `iPhone`, а также слова из списка `chatLayoutIgnore` в `config/qualityessentials.json`. Команда исправляется, только если в другой раскладке получается команда, которую знает клиент; в `/msg`, `/tell`, `/w`, `/r`, `/me` и похожих исправляется и текст сообщения, а ник после `/msg` переводится, если такой игрок онлайн. Клавиша Pause в открытом чате переводит раскладку выделенного текста или всей строки вручную.

Модель раскладки собирается скриптом `tools/layout/build_layout_model.py` из языковых файлов Minecraft 26.3 в кеше Loom и словарей `tools/layout/seed_ru.txt` и `seed_en.txt`. После правки словарей пересоберите модель и запустите тесты: `LayoutFixerTest` проверяет отложенные наборы слов из `src/test/resources/layout`.

#### Книги

- **Цвета и стили.** Панель слева от книги: 16 цветов и стили (жирный, курсив, подчёркнутый, зачёркнутый, мерцающий, обычный текст). С выделением оформление применяется к выделенному и не трогает текст после него, без выделения — к тексту дальше. Ctrl+B, Ctrl+I, Ctrl+U переключают стиль. Страница в редакторе показывает цвета так же, как в подписанной книге, а маленькие цветные засечки отмечают место каждого кода. Стрелки, Backspace и Delete перескакивают невидимые коды, поэтому курсор не застревает.
- **Почему цвета работают на ванильном сервере.** Ванильный сервер сохраняет коды `§` в страницах книги с пером и при подписании превращает страницы в текст, где эти коды видны как цвета. Плагины могут их вырезать — это зависит от сервера.
- **Страницы.** «+ Новая» вставляет пустую страницу после текущей, «Удалить» убирает текущую, «← Влево»/«Вправо →» меняют её местами с соседней, Ctrl+Enter разрывает страницу у курсора (оформление переносится), Ctrl+PageUp/PageDown — первая и последняя страница, колесо мыши листает. Книга не бывает длиннее 100 страниц и 1024 символов на странице — счётчики справа показывают, сколько осталось.
- **Вставка.** Ctrl+V (и кнопка «Вставить текст») вставляет буфер у курсора, а то, что не поместилось, продолжает на новых страницах после текущей: слова не разрываются, цвет переходит на следующую страницу. Строка `---` в тексте начинает новую страницу. Коды `§` из буфера сохраняются; `&c`, `&l` и другие превращаются в оформление, если включить «&-коды при вставке».
- **Отмена.** Ctrl+Z отменяет набор, оформление и действия со страницами, Ctrl+Y или Ctrl+Shift+Z — повтор.
- **Файлы.** «Сохранить» пишет книгу в `config/qualityessentials/books/<название>_<дата>.txt` (страницы разделены строкой `---`). «Загрузить» открывает список файлов из этой папки с предпросмотром: «Вставить у курсора» или «Заменить книгу» (Ctrl+Z в книге вернёт прежние страницы). Свои тексты можно класть туда же в UTF-8, до 512 КБ.
- **Чтение.** У подписанной книги и книги на кафедре справа кнопки «Копировать: Страницу / Всю книгу» (с Shift — без цветов), «Сохранить», «В начало», «В конец». Ctrl+C копирует страницу, Ctrl+Shift+C — книгу, Home и End переходят в начало и конец.
- **Название.** «Название до 32 символов» снимает ванильное ограничение поля в 15 символов: протокол и ванильный сервер принимают до 32. Выключено по умолчанию, потому что плагины сервера могут такое название отклонить.

#### Таблички

- **Копировать** кладёт все строки в буфер (по строке на строку) и запоминает их, **Вставить** раскладывает строки из буфера по строкам таблички, **Очистить** стирает всё. Ctrl+Shift+C и Ctrl+Shift+V делают то же с клавиатуры, а обычный Ctrl+V с несколькими строками в буфере заполняет текущую строку и следующие.
- **Мышь и клавиатура.** Клик по строке таблички выбирает её и ставит курсор, Shift+клик выделяет; Tab и Shift+Tab переходят между строками; Ctrl+Z и Ctrl+Y отменяют и повторяют.
- **&-коды.** Ванильный сервер вырезает коды `§` из табличек, поэтому палитра слева вставляет `&c`, `&l` и другие — их понимают EssentialsX и похожие плагины. Под палитрой видно, как табличка будет выглядеть, если сервер их поймёт.
- **Запомнить и повторить.** Средняя кнопка (выбор блока) по табличке запоминает текст стороны, к которой вы стоите лицом, — об этом сообщает строка над хотбаром. Новая табличка, которую вы только что поставили с табличкой в руке, сразу заполняется этим текстом; «Очистить» его уберёт, а «Вставить запомненное» вернёт. «Сразу подтверждать» закрывает такую табличку без нажатия «Готово» — удобно ставить много одинаковых.

### Управление

| Клавиша | Действие |
|---|---|
| `F10` | Открыть схему клавиатуры (также кнопка в «Настройки → Управление → Привязки клавиш») |
| `Pause` (в открытом чате) | Перевести раскладку выделенного текста или всей строки |
| не назначена | Открыть настройки QE |
| не назначена | Редактор расположения HUD |
| `Ctrl+F`, `Ctrl+Tab` / `Ctrl+Shift+Tab` (в чате) | Поиск по истории, переключение вкладок чата |

Клавиши назначаются в «Настройки → Управление», категория QualityEssentials. Сочетания книг и табличек описаны в их разделах выше.

### Настройки

Экран **Mod Menu → QualityEssentials** (или своя клавиша) собран на ванильных вкладках и строках: каждый модуль включается отдельно, там же кнопки редактора HUD и «Правила вкладок» чата. Общий файл `config/qualityessentials.json`; чат после перезахода — `config/qualityessentials/chat/`, запомненные шалкеры — `config/qualityessentials/shulkers/` (отдельно для каждого сервера и мира, до 4096 ящиков), правила вкладок — `config/qualityessentials/chat_tabs.json`, книги — `config/qualityessentials/books/`.

InfoMod открывает схему клавиатуры через `QualityEssentialsClient.createKeyboardScreen(Screen)`.

### Установка

1. **Fabric Loader** `0.19.3+` и **Fabric API**.
2. Скопируйте `quality-essentials-1.1.6.jar` из `build/libs/` в папку `mods/`.
3. **Mod Menu** — опционально, открывает экран настроек.

**Требования:** Minecraft `26.3` · Java `25` · только клиент.

### Сборка

```powershell
.\gradlew.bat clean build
```

Готовый JAR: `build/libs/quality-essentials-1.1.6.jar`.

Игровой тест:

```powershell
.\gradlew.bat runClientGameTest
```

Тест открывает книгу, вставляет длинный цветной текст, красит выделение, отменяет, сохраняет и загружает файл, проверяет чтение книги, таблички (вставку, отмену, автозаполнение) и настройки на русском, в окне 1280×720 и 854×480. Скриншоты — в `build/run/clientGameTest/screenshots`.

---

## English

### What It Is

`QualityEssentials` (mod id `qualityessentials`) is a client-side Fabric mod for Minecraft 26.3 by LTS_Server: quality-of-life features in one mod. Every module can be switched on and off in the settings and turns itself off when the original mod with the same feature is installed.

The code is written from scratch; nothing is copied from the original mods. The mod only shows what the client already knows: no mobs through walls and no ore highlighting.

### Features

| Module | What it does | Steps aside for |
|---|---|---|
| Food | Gold saturation outlines over the hunger bar, a blinking preview of the food in hand, hunger and saturation in the tooltip | AppleSkin |
| What is this | A panel at the top: the block or entity under the crosshair, growth, signal and other block properties, the right tool, breaking progress, health and armor, villager profession, horse speed and jump, source mod | Jade, WTHIT |
| Effect timers | Remaining time and level on effect icons | Status Effect Timer, Status Effect Bars |
| Armor and tools | Durability of armor and held items, arrows for bows and crossbows, how many blocks of a kind are in the inventory; three positions | Armor HUD, Armor Durability HUD |
| Riding | The player's hunger bar stays visible while riding; a mount panel: health, top speed, jump height, current speed | Better Mount HUD |
| Performance | FPS overlay (and 1% low), ping and an estimated server TPS on servers, real TPS and MSPT in singleplayer, memory, entities, coordinates (hidden with reduced debug info), time of day with the day number (blue when you can sleep) and the weather at the player | FPS Display |
| Shulkers | With Shift held, the contents of a shulker box or any container item as a grid in the tooltip (without Shift, a hint line with the item count), a stripe in the box colour, a summary mode that merges identical items. Placed boxes: look at one and hold Shift, and a window shows what it held when you last opened or placed it (the server never sends box contents to the client, so the mod remembers them itself; other players' boxes stay unknown until you open them) | Shulker Box Tooltip |
| New items | A small star on just-picked-up items in the inventory, gone on hover | Item Highlighter |
| Item details | With Shift held, the tooltip brings back lines the server hid with the `tooltip_display` component (enchantments, attributes and more) and shows a details block: ID, model (`item_model`), durability with percent, anvil repair cost, rarity, stack size, what the server hid, server data from `custom_data` (where plugins keep their item IDs) and the components the server added or removed. Without Shift, a "Shift: show hidden lines (N) and details" line appears at the bottom when something is hidden | — |
| Screenshots | Open, Folder and Copy path buttons on the saved-screenshot message | — |
| Chat | Long history restored after rejoining, a bigger window, timestamps, merged repeats, keyboard layout fix, tabs, search, long messages sent in parts (details below) | Chat Patches |
| Books | Colours and styles in the book and quill, page tools, pasting long text, undo, files; copying while reading (details below) | Scribble |
| Signs | Copy and paste all lines, mouse and Tab editing, an &-code palette, auto-filling a freshly placed sign (details below) | — |
| Dark loading screen | A dark background instead of the red flash on start and resource pack reloads | Dark Loading Screen |
| Keyboard map | Formerly BindBoard: the whole keyboard with numpad and mouse, bindings, conflicts, Ctrl/Shift/Alt layers, rebinding | BindBoard |

The What is this, Armor and tools, Riding and Performance panels move in the HUD editor: drag with snapping to edges and the centre, the wheel changes the size, and the background opacity is set separately. A position is stored relative to the nearest edge or the centre, so it stays put at another resolution.

#### Chat

This section moved here from Callout: the longer chat history and its restore after rejoining now live in QE, and Callout keeps only alerts and the mention history.

- **History.** 100, 1,000, 4,096 or 16,384 messages (16,384 by default). The list of sent messages, browsed with the arrow keys, has the same limit.
- **Restore after rejoining.** On leaving a server or a world, the last 1,000 messages are saved to `config/qualityessentials/chat/`, separately for each server address and each singleplayer world. On the next join they go into the history above the new messages, separated by a "Chat from your last visit" line and a grey stripe on the left. The restored chat is shown only in the open chat and never pops up on screen.
- **Window size.** Width and height: normal, ×1.5, ×2 or Maximum. With Maximum, the vanilla Width and Height sliders in the chat settings stretch to the whole screen. The window never leaves the screen and is rebuilt when the game window is resized.
- **Timestamps.** None, `12:30` or `12:30:45`. By default the "on hover only" mode is on: the time takes no room in the lines and slides in from the left while the cursor is at the left edge of the open chat; the lines move right smoothly and nothing overlaps. With the mode off, the time is written before every message.
- **Merged repeats.** Identical consecutive messages collapse into one line marked (×N).
- **Tabs.** Above the input line: All; Conversations — only player messages and your private messages (server system lines, deaths, quests, colonies, CoreProtect, anticheat and spying on other players' PMs stay out); Global and Local — player messages from the global and the local chat, told apart by the icon after `[X]` (`ꐙ` is global, `ꐘ` is local; the icons are set in the `global` and `local` groups); Private; Joins; Moderation — anticheat (Vulcan, Grim and others), SquadFilter, `[Мат]`, `[Защита]`, kicks and bans, PM spying, CoreProtect output. The defaults were checked against 14 thousand distinct chat lines from game logs: Conversations gets every player message and not a single service line. Before the rules run, text is normalised: small caps and decorative brackets such as `❪ᴠᴜʟᴄᴀɴ❫` read as `(vulcan)`. A tab appears once the history holds at least one of its messages; the number next to it counts messages since you last looked at it. Switch by clicking or with Ctrl+Tab / Ctrl+Shift+Tab. The selected tab also filters the on-screen chat when it is closed and is remembered between launches. Hidden messages are not lost: they stay in the history and show on their own tab.
- **Tab rules** live in `config/qualityessentials/chat_tabs.json` (the Tab rules button in the settings opens the file). A group is a set of regular expressions over the message text; a tab shows the groups from `show` (`*` means every message) and removes the groups from `hide`. The file is reloaded on the fly; errors in expressions go to the log and do not break the other rules. A file from an older rules version is replaced with the new defaults, and the old one is kept next to it as `chat_tabs.json.v<number>.bak`. Use the `(?U)` flag in expressions containing `\W` or `\w`: without it Java treats Cyrillic letters as non-letters.
- **Search.** Ctrl+F (or "Ctrl+F search" above the input on the right) turns the input line into a search: the chat keeps only the messages with that text in the current tab, the arrows and the wheel step through matches, Enter closes the search and scrolls the chat to the found message with a highlight, Esc closes the search. The text typed before the search comes back.
- **"↓ N new".** When the chat is scrolled up, the line above the input shows how many messages arrived below; a click returns to the latest.
- **Long messages.** The input line takes up to 1,280 characters. A message longer than 256 characters is sent in word-split parts with a 1.5 s pause; in `/msg`, `/tell`, `/w`, `/r`, `/me` and similar commands every part repeats the command and the name. Other commands longer than 256 characters are not sent, and the line above the input explains why. While the text is longer than 216 characters, a counter shows above the input.
- **Keyboard layout.** The automatic fix is off by default: turn on "Fix layout in messages" and "Fix layout in commands" in the chat settings (updating from 1.1.3 or earlier turns both off once). The fix runs on sending: `ghbdtn` → `привет`, `руддщ` → `hello`, `/ызфцт` and `.ызфцт` → `/spawn`. A word changes only when it looks clearly more plausible in the other layout: a letter model and dictionaries of frequent chat words in `assets/qualityessentials/layout/`. Online player names, links and addresses, words with digits and `_`, colour codes such as `&a`, words like `iPhone` and the words from the `chatLayoutIgnore` list in `config/qualityessentials.json` are left alone. A command is fixed only when the other layout gives a command the client knows; in `/msg`, `/tell`, `/w`, `/r`, `/me` and similar the message text is fixed too, and the name after `/msg` is converted when that player is online. The Pause key in the open chat converts the selected text or the whole line by hand.

The layout model is built by `tools/layout/build_layout_model.py` from the Minecraft 26.3 language files in the Loom cache and the dictionaries `tools/layout/seed_ru.txt` and `seed_en.txt`. After editing the dictionaries, rebuild the model and run the tests: `LayoutFixerTest` checks the held-out word sets in `src/test/resources/layout`.

#### Books

- **Colours and styles.** A panel left of the book: 16 colours and styles (bold, italic, underlined, strikethrough, obfuscated, plain). With a selection the formatting applies to the selected text and leaves the text after it alone; without one it applies to the text that follows. Ctrl+B, Ctrl+I and Ctrl+U toggle styles. The page in the editor shows colours the same way as a signed book, and small coloured ticks mark where each code sits. Arrows, Backspace and Delete skip invisible codes, so the cursor never gets stuck.
- **Why colours work on a vanilla server.** A vanilla server keeps `§` codes in book and quill pages and turns the pages into text on signing, where those codes show as colours. Plugins may strip them — that depends on the server.
- **Pages.** "+ New" inserts an empty page after the current one, Delete removes the current one, "← Left" / "Right →" swap it with its neighbour, Ctrl+Enter splits the page at the cursor (formatting carries over), Ctrl+PageUp/PageDown jump to the first and last page, and the mouse wheel turns pages. A book never exceeds 100 pages and 1,024 characters per page; the counters on the right show what is left.
- **Pasting.** Ctrl+V (and the Paste text button) pastes the clipboard at the cursor, and whatever does not fit continues on new pages after the current one: words are not split and the colour carries over to the next page. A `---` line in the text starts a new page. `§` codes from the clipboard are kept; `&c`, `&l` and others turn into formatting when "&-codes on paste" is on.
- **Undo.** Ctrl+Z undoes typing, formatting and page actions; Ctrl+Y or Ctrl+Shift+Z redoes.
- **Files.** Save writes the book to `config/qualityessentials/books/<title>_<date>.txt` (pages separated by a `---` line). Load opens a list of files from that folder with a preview: Insert at cursor or Replace book (Ctrl+Z in the book brings the old pages back). You can put your own UTF-8 texts there, up to 512 KB.
- **Reading.** A signed book and a book on a lectern get buttons on the right: Copy: Page / Whole book (with Shift, without colours), Save, First, Last. Ctrl+C copies the page, Ctrl+Shift+C the book; Home and End jump to the start and the end.
- **Title.** "Titles up to 32 characters" lifts the vanilla 15-character limit of the field: the protocol and a vanilla server accept up to 32. Off by default, because server plugins may reject such a title.

#### Signs

- **Copy** puts all lines on the clipboard (one per line) and remembers them, **Paste** spreads the clipboard lines over the sign lines, **Clear** wipes everything. Ctrl+Shift+C and Ctrl+Shift+V do the same from the keyboard, and a plain Ctrl+V with several lines on the clipboard fills the current line and the next ones.
- **Mouse and keyboard.** Clicking a sign line selects it and places the cursor, Shift+click selects text; Tab and Shift+Tab move between lines; Ctrl+Z and Ctrl+Y undo and redo.
- **&-codes.** A vanilla server strips `§` codes from signs, so the palette on the left inserts `&c`, `&l` and others, which EssentialsX and similar plugins understand. Below the palette you see how the sign will look if the server understands them.
- **Remember and repeat.** The middle button (pick block) on a sign remembers the text of the side you are facing, and a line above the hotbar says so. A new sign you have just placed with a sign in hand fills with that text at once; Clear removes it and Paste remembered brings it back. "Confirm at once" closes such a sign without pressing Done, which is handy for placing many identical signs.

### Controls

| Key | Action |
|---|---|
| `F10` | Open the keyboard map (also a button in Options → Controls → Key Binds) |
| `Pause` (in the open chat) | Convert the layout of the selected text or the whole line |
| unbound | Open the QE settings |
| unbound | HUD layout editor |
| `Ctrl+F`, `Ctrl+Tab` / `Ctrl+Shift+Tab` (in chat) | Search the history, switch chat tabs |

Keys are bound in Options → Controls, category QualityEssentials. Book and sign shortcuts are listed in their sections above.

### Configuration

The **Mod Menu → QualityEssentials** screen (or its own key) is built on vanilla tabs and option rows: every module is switched separately, and the HUD editor and chat Tab rules buttons are there too. Main file `config/qualityessentials.json`; restored chat in `config/qualityessentials/chat/`, remembered shulker boxes in `config/qualityessentials/shulkers/` (per server and world, up to 4,096 boxes), tab rules in `config/qualityessentials/chat_tabs.json`, books in `config/qualityessentials/books/`.

InfoMod opens the keyboard map through `QualityEssentialsClient.createKeyboardScreen(Screen)`.

### Installation

1. **Fabric Loader** `0.19.3+` and **Fabric API**.
2. Copy `quality-essentials-1.1.6.jar` from `build/libs/` into `mods/`.
3. **Mod Menu** is optional and opens the settings screen.

**Requirements:** Minecraft `26.3` · Java `25` · client-only.

### Building

```powershell
.\gradlew.bat clean build
```

Output JAR: `build/libs/quality-essentials-1.1.6.jar`.

In-game test:

```powershell
.\gradlew.bat runClientGameTest
```

The test opens a book, pastes a long coloured text, colours a selection, undoes, saves and loads a file, and checks book reading, signs (paste, undo, auto-fill) and the settings in Russian, in 1280×720 and 854×480 windows. Screenshots go to `build/run/clientGameTest/screenshots`.

## Лицензия / License

[MIT](LICENSE). Developed by LTS_Server.
