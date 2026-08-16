# JavaFX of Tanks

Аркадная игра «Танки» — переписана с нуля на **Java 21 / JavaFX** из
прототипа [ArcadeGame](https://github.com/WhiteHusky555/ArcadeGame)
(C++ / FLTK). Игровые правила и ощущение от игры сохранены, но код
спроектирован заново: слоистая архитектура **MVVM**, доменная логика без
единой зависимости от UI-фреймворка, симуляция партии в отдельном потоке
и вся редактируемая часть игры (карта, управление, тема, тексты) вынесена
в текстовые конфиги.

## Управление

| Клавиши          | Действие          |
|-------------------|-------------------|
| ↑ ↓ ← → / W A S D | Движение          |
| Пробел            | Выстрел           |
| P                 | Пауза             |
| Esc               | Выход в меню      |

Раскладка не зашита в код — она читается из
[`controls.properties`](src/main/resources/ru/rsreu/ineprokin/controls.properties)
при каждом запуске.

## Запуск

Нужен JDK 21+ (зависимости, включая сам JavaFX, Maven скачает сам).

```bash
# через плагин org.openjfx — не требует ручной настройки module-path
mvn javafx:run

# либо собрать один исполняемый jar со всем нужным внутри
mvn package
java -jar target/javafx-of-tanks.jar
```

```bash
mvn test
```

## Архитектура

Игра построена по **MVVM**, но не «для галочки» — границы между слоями
проведены так, чтобы `model`/`engine` можно было тестировать без единого
JavaFX-класса на classpath, а `view` — не знал ни о чём, кроме своей
ViewModel и record-снимков.

```
model     — чистый домен: Tank, Bullet, GameMap… Ни одного import javafx.*
engine    — правила партии: столкновения, ИИ, спавн. Знает про model, не про UI.
viewmodel — граница с UI: JavaFX-свойства, поток симуляции, DTO-снимки для рендера.
view      — Canvas/Scene/Button. Знает только про ViewModel, никогда — про GameWorld.
navigation— composition root: единственное место, которое видит Stage.
config    — парсинг текстовых ресурсов (controls/theme/about) в типизированные объекты.
```

```
model.capability/   Updatable, Destructible, Damageable, DamageSource, Fireable
model.geometry/      Direction, Position, TileCoord
model.entity/         GameObject, Tank, Bullet, PlayerId, GameState
model.map/             GameMap, TileType, MapLoadException

engine/                CollisionService, EnemyAiService, GameWorld, GameWorldView, GameConfig
engine.ai/              AiStrategy, AiDecision, RandomAiStrategy
engine.spawn/           SpawnLocationFinder, DefaultSpawnLocationFinder

viewmodel/              GameViewModel, GameSimulationLoop, MenuViewModel, AboutViewModel
viewmodel.dto/           GameSnapshot, TankView, BulletView

view/                   GameView, GameRenderer, MenuView, AboutView
navigation/              Router, SceneRouter
config/                  ControlsConfig, ThemeConfig, AboutContent
```

### Функциональные интерфейсы вместо флагов и `instanceof`

Способности объектов выражены однометодными интерфейсами, а не булевыми
полями или чередой `dynamic_cast` (как было в C++-версии):

- `Damageable`/`DamageSource` — кто может получать урон, кто его наносит;
- `Fireable` — `Tank.tryFire()` инкапсулирует расчёт точки вылета пули
  (в оригинале этот код был дословно продублирован в двух местах) и
  возвращает `Optional<BulletSpawnRequest>`;
- `AiStrategy` — тактика вражеского танка — стратегия, а не набор
  `rand() % 100 < 5` внутри движка; в тестах ей на замену подставляется
  лямбда, которая никогда не двигается и не стреляет, без единого мока.

### Records как DTO между слоями

`GameMap`, `Position`, `TileCoord`, `BulletSpawnRequest`, `AiDecision`,
`AboutInfo` и, главное, `GameSnapshot`/`TankView`/`BulletView` — records.
`GameSnapshot` — это единственное, что видит `view`: `view` никогда не
держит в руках «живой» изменяемый `Tank` из движка, только его
неизменяемый снимок на момент последнего тика.

### Масштабируемость на второго игрока

Второй игрок физически не подключён, но и `Tank`, и `GameMap`, и
`GameWorld` уже адресуют участников через `PlayerId` (`PLAYER_ONE` /
`PLAYER_TWO`), а не единственным полем `playerTank`:

- на карте уже понимается второй маркер старта — `2` (сейчас в
  [`map.txt`](src/main/resources/ru/rsreu/ineprokin/map.txt) есть только `P`);
- раунд заканчивается, когда уничтожены **все** танки игроков, а не
  когда погиб единственный — это уже сегодня корректно работает и для
  одного, и для двух игроков;
- `GameWorld.movePlayer/firePlayer/getPlayerHealth` принимают `PlayerId`.

Добавить второго живого игрока — значит поставить `2` на карте и
провести вторую раскладку клавиш от `GameView` до
`GameViewModel`/`GameSimulationLoop`; ни модель, ни движок, ни поток
симуляции трогать не придётся.

## Конкурентность

Партия считается в фоновом потоке-демоне независимо от кадров экрана:

```
GameSimulationLoop implements Runnable   — переоткрывает деталь на ~60 Гц
        │  ScheduledExecutorService.scheduleAtFixedRate(this, …)
        ▼
   GameWorld.tick(deltaSeconds)          — реальное время, а не «кадры»
        │
        ▼  публикует AtomicReference<GameSnapshot>
   GameView (AnimationTimer, FX-поток)   — читает снимок каждый pulse
```

- **Симуляция не зависит от FPS.** Шаг считается по настоящему
  прошедшему времени (`System.nanoTime()`), а не по числу вызовов
  кадра — на слабом ПК танк не притормозит, на мощном — не полетит
  вдвое быстрее.
- **Рендер сам подстраивается под vsync.** `AnimationTimer.handle(...)`
  вызывается ровно раз за такт рендер-пайплайна JavaFX, который сам
  пайплайн синхронизирует с вертикальной разверткой монитора — ручной
  троттлинг кадров в прикладном коде не нужен и был бы лишним.
- **Обмен данными между потоками — без единого `synchronized`.**
  Делить есть что: команды игрока (`ConcurrentHashMap`, `AtomicBoolean`,
  `ConcurrentHashMap.newKeySet()`) и один и только один разделяемый
  объект — `GameSnapshot` — в `AtomicReference`. Каждый снимок целиком
  неизменяем (record с `List.copyOf` в компактном конструкторе), поэтому
  делить, по сути, нечего — только ссылки. Единственный переход
  управления обратно в FX-поток (возврат в меню после экрана
  результатов) оформлен через `Platform.runLater(...)`, как того требует
  правило JavaFX «сцену можно трогать только из FX-потока».
- Поля, которые читает и пишет исключительно сам поток симуляции
  (таймер перезарядки экрана результатов, сглаженный FPS), намеренно
  остались обычными `double`/`boolean` — оборачивать их в `Atomic*`
  было бы накладными расходами без единой причины: разделять их не с кем.

## Конфигурация без пересборки

Всё, что может поменять человек без Java-компилятора под рукой,
вынесено в текстовые ресурсы:

| Файл                  | Что описывает                                  |
|------------------------|------------------------------------------------|
| `map.txt`             | Карта: `#` стена, `P`/`2` старт игроков, `E` враг |
| `controls.properties` | Раскладка клавиш (имена `javafx.scene.input.KeyCode`) |
| `theme.properties`    | Цветовая палитра экрана (`#RRGGBB[AA]`)         |
| `about.properties`    | Текст экрана «О программе»                      |

## Тесты

`model`/`engine` не содержат ни одного импорта JavaFX, поэтому
покрываются юнит-тестами (JUnit 5) без запуска графического
приложения: разбор карты и её ошибки, перезарядка и урон танка,
столкновения со стеной/танком/пулей, дружественный огонь, пауза,
уничтожение врага и начисление очков — `GameWorldTest` собирает
`GameWorld` с ИИ-стратегией-заглушкой (лямбдой), не трогая настоящую
рандомизированную тактику.

```bash
mvn test
```

## Чем это отличается от оригинала

Помимо смены языка/фреймворка (C++/FLTK → Java/JavaFX) и переноса
рисования из немедленного режима на `Canvas`:

- **Убран весь мёртвый код.** `GameMap.setTile`/`resetToInitialState`
  в оригинале не вызывались никем (стены никогда не разрушались) — здесь
  карта — простой неизменяемый value-объект.
- **Один `GameModel`-god-class разделён на пять с одной обязанностью
  каждый**: `GameWorld` (оркестрация), `CollisionService` (геометрия),
  `EnemyAiService` (тайминги ИИ), `RandomAiStrategy` (собственно тактика),
  `DefaultSpawnLocationFinder` (поиск точки возрождения).
- **Ошибки карты — исключение с сообщением**, а не молчаливый `return false`.
- **Движение — по реальному времени**, а не по числу нажатий клавиши
  (в оригинале зависело от повтора клавиши операционной системой).
- **ИИ считает время в секундах через собственный таймер на танк**,
  а не «`rand() % 150 < 5` при предполагаемых 60 кадрах в секунду».
- **HUD не наезжает на игровое поле** — под него зарезервирована
  отдельная полоса сверху, а не наложение поверх верхнего ряда тайлов.

## Автор

Непрокин Иван
