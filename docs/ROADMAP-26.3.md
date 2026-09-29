# Krylix → Minecraft 26.3: план (версия 1.4.0)

Составлено 2026-09-28 по трём источникам: полный аудит ветки `26.1-dev`, сводка изменений 26.2/26.3 и загрузчиков (со ссылками — в конце), и локальная проверка: jar'ы 26.3 и NeoForge 26.3.0.16-beta уже лежат в кэше Gradle (от femboy-mod), поэтому пути текстур и API можно сверять напрямую, а не по слухам.

## Коротко

1. **Сначала hotfix 1.3.1 на 26.1** — пять багов, которые видны игрокам или опасны для серверов (раздел 3). Это полдня работы, и они же всплыли на скриншотах для README.
2. **Порт 1.4.0 делаем через реструктуризацию, а не правкой двух копий.** Сейчас 85–90% кода Fabric и NeoForge — построчные дубли (`LeaderboardScreen`, `MobTextures`, `HudRender`, `MobKillStatsData` — 0 отличий). В 26.2/26.3 сменился рендер текста, HUD, неймтеги и ввод — править пришлось бы всё дважды. Переносим логику в `common` (как в femboy-mod: Architectury, `api` / `common` / `fabric` / `neoforge` / `example-addon`), и порт делается один раз.
3. **Addon API** — отдельный модуль `api` с событиями и реестрами (раздел 6), плюс data-driven иконки мобов через ресурс-паки, чтобы модовые мобы работали вообще без кода.
4. **Новые фичи** — ассисты, цвета команд, редактор расположения HUD, записи в фиде для боссов, экспорт статистики (раздел 7).

## 1. Что поменялось в 26.2 / 26.3 и что это ломает

| Изменение | Версия | Что ломается в Krylix |
|---|---|---|
| Ввод перешёл с GLFW на **SDL**: `InputConstants` = SDL scancodes, модификаторы `SDL_KMOD_*` | 26.3 | `KrylixKeyBindings` на обоих загрузчиках использует `GLFW.GLFW_KEY_K/L/O/H` — не скомпилируется |
| `SubmitNodeCollector.submitNameTag()` **потерял параметр distanceSq** (culling внутри); новые атрибуты `name_tag_distance` / `below_name_distance` | 26.2 | `HealthIndicator` (2 вызова на загрузчик) |
| **OIT** (order-independent transparency): неймтеги рисуются через `oitTranslucent`, `seeThroughNameTags` слит с `seeThrough`, `textBackground*` удалены | 26.3 | табличка здоровья — проверять с опцией «improved transparency» вкл/выкл |
| **Gui/Hud split**: HUD переехал в класс `Hud` внутри `Gui`; `Minecraft.screen` → `gui.screen()`, `setScreen` → `gui.setScreen`, `hideGui` → `Hud.isHidden()` | 26.2 | `GuiMixin` (Fabric), `KrylixClient` (NeoForge), открытие лидерборда |
| **Рендер текста переписан**: `Font.drawInBatch*` удалены → `Font.prepareText()` / `PreparedText`; `ChatFormatting` урезан | 26.2 | `text/centeredText` в HUD и экранах, `ChatFormatting.WHITE` в табличке |
| `GuiGraphicsExtractor.skin()` принимает `Model.Simple` вместо `PlayerModel`; `ScissorStack.push/pop` → void | 26.2 | головы игроков в фиде, лидерборде, recap; скругления в `HudRender` |
| `TextureAtlasSprite` → `UvMapping`; `submitModel*` принимает `UvMapping` | 26.3 | `AtlasSprite`-сердца в табличке (проверить) |
| Бэкенд **Vulkan** (экспериментально), OpenGL запланирован к удалению | 26.2 | прямых GL-вызовов нет — ок, но только через Blaze3D |
| Новый моб **Sulfur Cube** (как слайм: делится при смерти) | 26.2 | нужна иконка и группировка «×N» в фиде, иначе спам |
| Мебель **Cushion** (сущность для сидения) | 26.3 | не должна попадать в статистику — проверить фильтр `MobCategory` |
| Атласы кроватей и табличек слиты в `blocks.png` | 26.2 | Krylix не затрагивает |

**Проверено локально:** все 36 путей текстур мобов из `MobTextures` (и оверлеи глаз эндермена/бриза) есть в клиенте 26.3 без изменений — таблица иконок переносится как есть.

**Не упомянуто в праймерах 26.2/26.3** (значит, ждём ошибок компиляции, а не документации): `CustomPacketPayload`/`StreamCodec`, `SavedDataType`, `DamageSource`/`CombatTracker`, `LivingEntity#die`.

## 2. Тулчейн для 26.3

| | Версия | Статус |
|---|---|---|
| Java | 25 | без изменений |
| NeoForge | 26.3.0.26-beta | **только beta**; последний stable — 26.2.0.88 |
| ModDevGradle | 2.0.147 | |
| Fabric Loader | 0.19.5 | |
| Loom | 1.17.x (1.18.2 — последний релиз) | |
| Fabric API | 0.161.0+26.3 | `HudElementRegistry` на месте |
| Architectury API | 22.0.2 | уже используется в femboy-mod на 26.3 |
| Cloth Config | 26.3.159 (Fabric + NeoForge) | есть |
| Mod Menu | 21.0.0 | есть |
| MinecraftForge | 66.0.6 (с 2026-09-27) | **поддерживает 26.3**, но Architectury Forge не поддерживает — отдельный модуль |

Маппинги: только официальные имена Mojang, ремаппинга нет.

Решение по Forge: модуль `forge/` сейчас на API 1.21, не собирается и в `settings.gradle.kts` закомментирован. Предлагаю **удалить его**, перенеся три полезные вещи (проверку прав, регистрацию экрана конфига, правильный raycast), и вернуться к Forge только если о нём попросят игроки: у Kill Feed-ниши на Modrinth и так мало загрузок, а третий загрузчик — это +30% работы на каждый порт.

## 3. Hotfix 1.3.1 (26.1) — ✅ сделано

Все пункты ниже исправлены в 1.3.1 (плюс здоровье мобов-убийц в фиде, дальность таблички во всех направлениях и поиск цели раз в тик); проверка — `HealthPlateClientTest` и скриншоты `DocsShotsClientTest`. Подробности — в CHANGELOG.


| # | Баг | Где | Последствие |
|---|---|---|---|
| 1 | `/krylix toggle` **без проверки прав** | `KrylixServerCommands` (Fabric и NeoForge) | любой игрок выключает килл-фид всему серверу. Проверка была только в старом forge-модуле |
| 2 | Табличка здоровья **перерисовывает чужие неймтеги** данными цели и не проверяет невидимость/спектатора/скрытие тегов командой | `HealthIndicator.onSubmitNameDisplay` / `onDoRenderNameTag` | у всех видимых игроков и именованных мобов — сердца цели; в PvP раскрываются невидимые игроки |
| 3 | Карточка Death Recap **налезает на «You Died!»** и строку причины смерти | `DeathRecapClient.render`: `cardY = height/4 - 20` | видно на скриншоте при любом масштабе GUI. Фикс: ставить карточку под кнопками (`height/4 + 124`) |
| 4 | Fabric: «Damage Dealt» **всегда 0** | `combatDamageMap` читается, но не пишется (`KrylixFabric:26,101`) | NeoForge заполняет его в `LivingDamageEvent.Post`; на Fabric нужен `ServerLivingEntityEvents.AFTER_DAMAGE` |
| 5 | Zombie Villager — **серая заглушка** вместо лица | UV есть (`MobTextures:110`), но `register(...)` нет | виден в панели статистики |
| 6 | Экран конфига **не зарегистрирован** ни на одном загрузчике | Fabric: нет entrypoint `modmenu`; NeoForge: `IConfigScreenFactory` импортирован, но не используется | настройки меняются только в файле |
| 7 | Диапазоны версий открыты: `">=26.1"` / `[26.1,)` | `fabric.mod.json`, `neoforge.mods.toml` | jar для 26.1 попытается загрузиться на 26.3 и упадёт. Нужно `~26.1` / `[26.1,26.2)` |
| 8 | Нет ключа `krylix.command.hud_count` во всех 13 языках; ещё 3 ключа (`deathrecap.title`, `deathrecap.damage_dealt`, `health_bar_style.hearts`) нет в 11 языках | lang | сырые ключи в интерфейсе |

## 4. Производительность и надёжность (в порт)

- **Полная рассылка лидерборда на каждое убийство моба.** Сейчас на каждого убитого моба сервер собирает всю историческую статистику (все UUID, когда-либо заходившие) и шлёт её всем онлайн-игрокам. На сервере с мобофермой это O(история × онлайн) байт на каждого моба. Делаем: дельта-пакет с одной строкой + снимок по запросу при открытии лидерборда (C2S), top-N / постранично. Все коллекции в кодеках — с ограничением размера (`ByteBufCodecs.collection(…, max)`): NeoForge режет пакеты около 32 КиБ, см. их security advisory.
- **Raycast таблички — на каждую сущность в каждом кадре.** `updateTarget()` вызывается из `extractRenderState` для каждой видимой сущности: O(E²) за кадр. Считать цель один раз за тик. Плюс `inflate(look * 48)` сжимает AABB при отрицательных компонентах взгляда — «48 блоков» работают только в части направлений. Заменить на `expandTowards` + `ProjectileUtil.getEntityHitResult` (так было в forge-модуле).
- **Общая изменяемая Map в одиночной игре.** `MobStatsSyncPacket` несёт живую Map из SavedData; во внутреннем соединении пакеты не сериализуются, и клиент с сервером делят одну HashMap → риск `ConcurrentModificationException`. Слать `Map.copyOf`.
- **Панель мобов получает данные только у убийцы**, хотя они общие для мира — остальные видят устаревшие числа.
- **Рассинхрон часов:** `KillEntry.timestamp` — время сервера, а затухание считается по часам клиента. При расхождении больше 15 с строки не показываются вовсе. Ставить метку при получении пакета.
- **Состояние клиента не сбрасывается при выходе** — лидерборд сервера A виден на сервере B, recap живёт 300 с.
- `combatDamageMap` на NeoForge растёт без ограничений (ключи атакующий×цель для каждого удара по мобу).
- Покадровые аллокации в HUD (копии списков, `UUID.fromString`, `new ItemStack`, сортировки, `String.format`) — вынести в модель строки, собираемую один раз при добавлении записи.
- NeoForge регистрирует payload'ы без `.optional()` — ванильные клиенты не могут зайти на сервер с Krylix.
- `SavedDataType(..., DataFixTypes.LEVEL)` — для данных мода нужен `null`, иначе при апгрейде мира по ним пройдут ванильные фиксеры.

## 5. Архитектура 1.4.0

```
api/            чистые интерфейсы: события, реестры, KrylixPlugin, @KrylixAddon — отдельный артефакт
common/         вся логика: сервер (учёт убийств, статистика, пакеты), клиент (HUD, экраны, табличка)
fabric/         тонкий слой: события Fabric → common, HudElementRegistry, миксины только где нет события
neoforge/       тонкий слой: события NeoForge → common, RegisterGuiLayersEvent, RenderNameTagEvent
example-addon/  рабочий пример аддона для обоих загрузчиков
```

- Платформенные различия — через Architectury (сеть, регистрация клавиш, HUD-слой, конфиг-экран) или маленький `Services`-интерфейс там, где Architectury нет.
- `handleOnClient()` убрать из record-ов пакетов в клиентские обработчики (сейчас сервер переживает это только благодаря ленивой загрузке классов).
- `KillEntry` → неизменяемый `record` с `EntityType`-id жертвы и убийцы, `Set<Identifier>` тегов вместо булевых флагов (crit/smash/longshot/…) и местом для данных аддонов.
- Одинаковые корни команд на обоих загрузчиках (сейчас `/krylixclient` на Fabric и `/krylix` на NeoForge, где клиентский корень ещё и конфликтует с серверным).
- Удалить: `forge/`, `neoforge/bin`, `System.out.println`, неиспользуемые импорты, мёртвый `HealthBarStyle` (или вернуть ему смысл — стили таблички), старый `updates.json` с `{githubUser}`.

## 6. Addon API — ✅ API 1.0.0 сделан

Сделано в 1.4.0 (руководство — [`docs/api/README.md`](api/README.md), пример — `example-addon/`): `KrylixAddon` +
entrypoint `krylix` / `@RegisterKrylixAddon`, шина событий (`KillCreditEvent`, `KillEvent`, `StatRecordedEvent`,
`FeedEntryEvent`), чтение статистики, головы мобов из JSON в ресурс-паках и из кода, провайдеры здоровья, артефакт
`com.eliasnvx:krylix-api`. Проверка — юнит-тесты в `common` и `AddonApiClientTest` в настоящей игре.
Ещё не сделано из плана ниже: вкладки лидерборда (`LeaderboardTabs` + `StatProvider`), иконки оружия/причин смерти
через реестр, цвета команд — это следующие минорные версии API.


**Подключение аддона** — как у Jade/JEI:

```java
@KrylixAddon                                   // NeoForge: поиск по аннотации в scan data
public final class MyAddon implements KrylixPlugin {   // Fabric: entrypoint "krylix" в fabric.mod.json
    public Identifier id() { return Identifier.fromNamespaceAndPath("mymod", "krylix"); }
    public void registerCommon(KrylixCommonRegistrar r) { ... }
    public void registerClient(KrylixClientRegistrar r) { ... }   // вызывается только на клиенте
}
```

Плагины сортируются по id, дубликаты отбрасываются, исключение одного плагина не роняет остальные (в лог — с id мода-владельца), после загрузки реестры замораживаются.

**События** (свой маленький `Event<T>` в духе Fabric, одинаковый на всех загрузчиках):

| Событие | Сторона | Зачем |
|---|---|---|
| `KILL` → `PASS / CANCEL` | сервер | отменить или изменить запись: не учитывать в статистике, не рассылать, добавить теги |
| `KILL_CREDIT` | сервер | кому засчитать: питомец → хозяин, TNT → поджигатель, снаряд → владелец |
| `STAT_RECORDED` | сервер | внешняя статистика, вебхуки |
| `FEED_ENTRY` | клиент | поменять текст, цвета, иконки строки или скрыть её |
| `LEADERBOARD_OPEN` | клиент | добавить вкладки |

**Реестры** (ключ — `Identifier`, приоритет, побеждает первый ненулевой):

- `KillIconProviders` — иконка по `EntityType` / тегу / предикату: спрайт, предмет, голова модели или свой рендер. Это же закрывает Sulfur Cube и все модовые мобы.
- `WeaponIconProviders` — `ItemStack` → иконка (пушки, заклинания, модовые снаряды).
- `DeathCauseIcons` — по `ResourceKey<DamageType>` или тегу (сейчас это `msgId.contains("fire")`, что ловит и `inFire`, и `onFire`).
- `HealthProviders` — `Entity` → текущее/макс HP, поглощение, щит, подпись (боссы, многочастные мобы, модовые системы здоровья).
- `LeaderboardTabs` + `StatProvider<T>` со `StreamCodec` (коллекции только ограниченные).
- `TeamColorProviders` — FTB Teams, Open Parties and Claims, ванильные команды.

**Без кода, через ресурс-пак:** `assets/<ns>/krylix/heads/<entity>.json` с текстурой и UV лица. Модпакеры добавляют иконки для любых мобов без Java; аддоны кладут JSON к себе в jar. Это же лечит хрупкость таблицы UV при следующих обновлениях.

**Публикация:** `api` — отдельный артефакт `com.eliasnvx:krylix-api:<api>+26.3` с sources и javadoc, со своей SemVer и `KrylixApi.API_VERSION`. Внутри основного jar API тоже есть, игроку ставить ничего дополнительно не нужно. Аннотации `@ApiStatus.Experimental / Internal / NonExtendable`; методы добавляем только `default`, удаляем после одной минорной версии в deprecated. Документация — `docs/api/README.md`, как в femboy-mod.

## 7. Новые фичи — по ценности

1. **Ассисты**: все, кто нанёс урон цели за последние ~10 с, — в строке фида («A + B → C») и в статистике.
2. **Цвета команд**: имена в фиде и лидерборде — цветом `PlayerTeam`; свои не подсвечиваются как враги.
3. **Редактор расположения HUD** — перетаскивание фида и панели мышью, якоря к углам, прилипание (у Cloth есть только числовые смещения).
4. **Фид для важных мобов**: убийства боссов (Визер, Дракон, Варден) и именованных мобов — опционально в общий фид.
5. **Табличка как «unit frame»**: у Neat нет версии для 26.x (последняя — 1.21.8), а таблички у Krylix — самая сильная фича. Добавить поглощение/броню, эффекты, стиль «полоска» и «числа урона» (опция).
6. **Recap по `CombatTracker`**: последние N ударов — кто, чем, сколько урона.
7. **Статистика по оружию и типу урона**, личные рекорды (самый дальний выстрел, больше всего за жизнь) — без громких анонсов в чат.
8. **Лидерборд**: сортировка по колонке, поиск игрока, экспорт JSON/CSV командой; формат «37K / 12D» читается как «37 тысяч» — сделать отдельные колонки.
9. **Sulfur Cube, слаймы, магма-кубы**: иконка + группировка делений в «×N».
10. **Доступность**: палитра для дальтоников (не полагаться только на красный/зелёный), размер текста HUD отдельно от масштаба GUI, нарратор для экранов (новая `NarrationTrigger` в 26.3).
11. **Jade**: в подсказке моба — «убито тобой: N».

## 8. Тесты, скриншоты, релизы

- **Скриншоты документации** уже автоматизированы: `KRYLIX_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest` строит сцены в настоящем мире (реальные убийства мобов, табличка, фид, экран смерти, лидерборд) и снимает 1920×1080 за ~30 с; `tools/docs/make_readme_images.py` собирает из них баннер и галереи. После фиксов и порта картинки обновляются одной командой.
- **Серверные GameTests** (как в femboy-mod): учёт убийства и начисление статистики, фильтр категорий, права на команды, сохранение и загрузка SavedData, кодеки пакетов (включая лимиты).
- **CI**: сборка обоих загрузчиков + GameTests; релиз проверяет, что тег совпадает с `coreVersion`; публикация на Modrinth и CurseForge из CI; `softprops/action-gh-release@v1` → v2; исправить извлечение секции CHANGELOG (`sed '$d'` съедает последнюю строку, если секция последняя).

## 9. Порядок работ

| Фаза | Что | Результат |
|---|---|---|
| 0 | Hotfix 1.3.1 на 26.1 (раздел 3) — ✅ | релиз для текущих игроков |
| 1 | Новая структура модулей на 26.1: `common` с Minecraft в classpath, перенос дублей, Services/Architectury — **без изменения поведения** | один код вместо двух, GameTests зелёные |
| 2 | Переход на 26.3: версии, SDL-клавиши, `submitNameTag`, Gui/Hud, текст, OIT | 1.4.0-beta (NeoForge пока beta) |
| 3 | Производительность и надёжность (раздел 4) | готовность к большим серверам |
| 4 | `api` + `example-addon` + JSON-иконки + документация | публичный API |
| 5 | Фичи из раздела 7 по порядку | 1.4.x |

Фаза 1 на 26.1 до смены версии — сознательно: так рефакторинг и порт не смешиваются, и любую регрессию видно сразу.

## 10. Решения за тобой

1. ~~Делаем hotfix 1.3.1 на 26.1 перед портом?~~ Да, сделан.
2. Architectury, как в femboy-mod, или свой минимальный `Services` без зависимости? (Architectury — ещё одна обязательная библиотека у игрока.)
3. Forge 66 для 26.3: удаляем модуль и не поддерживаем — или нужен?
4. Cloth Config оставляем (он есть для 26.3) или переходим на свой конфиг / YACL?
5. ~~Иконка~~ — `draft_b`, стоит в 1.3.1.

## Источники

- Minecraft Wiki: [Java Edition 26.2](https://minecraft.wiki/w/Java_Edition_26.2), [Java Edition 26.3](https://minecraft.wiki/w/Java_Edition_26.3)
- NeoForged primers: [26.2](https://docs.neoforged.net/primer/docs/26.2/), [26.3](https://docs.neoforged.net/primer/docs/26.3/); [сетевой advisory](https://neoforged.net/news/mitigating-vulnerabilities-network/)
- Fabric: [26.2](https://fabricmc.net/2026/06/15/262.html), [26.3](https://fabricmc.net/2026/09/15/263.html), [отказ от обфускации](https://fabricmc.net/2025/10/31/obfuscation.html)
- Версии: [NeoForge maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml), [Fabric meta](https://meta.fabricmc.net/v2/versions/loader), [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/), [Cloth Config на Modrinth](https://api.modrinth.com/v2/project/cloth-config/version?game_versions=%5B%2226.3%22%5D)
- Паттерны API: [JEI plugins](https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins), [Jade plugins](https://jademc.readthedocs.io/en/latest/plugins19/getting-started/), [FancyModLoader ModFileScanData](https://github.com/neoforged/FancyModLoader/blob/main/loader/src/main/java/net/neoforged/neoforgespi/language/ModFileScanData.java)
