# JavaFX of Tanks

[![Java CI with Maven](https://github.com/WhiteHusky555/JavaFxOfTanks/actions/workflows/maven.yml/badge.svg)](https://github.com/WhiteHusky555/JavaFxOfTanks/actions/workflows/maven.yml)

Аркадная игра «Танки» на **Java 21 / JavaFX**: слоистая архитектура
**MVVM**, доменная логика без единой зависимости от UI-фреймворка,
симуляция партии в отдельном потоке и вся редактируемая часть игры
(карта, управление, тема, тексты) вынесена в текстовые конфиги.

## Управление

Поворот на месте (влево/вправо) плюс газ вперёд/назад вдоль курса —
без мгновенного переключения между четырьмя направлениями.

| Игрок    | Вперёд | Назад | Влево | Вправо | Выстрел |
|----------|--------|-------|-------|--------|---------|
| Игрок 1  | W      | S     | A     | D      | Пробел  |
| Игрок 2  | ↑      | ↓     | ←     | →      | Enter   |

Общие: **P** — пауза, **Esc** — выход в меню.

Раскладка не зашита в код — она читается из
[`controls.properties`](src/main/resources/ru/rsreu/ineprokin/controls.properties)
при каждом запуске.

Второй игрок необязателен и подключается посреди партии, а не только со
старта: первое же нажатие клавиши из его раскладки создаёт ему танк и
заменяет приглашение в HUD на блок статистики (очки, здоровье,
перезарядка). Раунд заканчивается, только когда уничтожены **все**
подключившиеся игроки — отсутствие второго или его более ранняя гибель
не влияют на условие поражения для первого. Очки считаются по
`PlayerId` стрелка, а не общим счётчиком на двоих.

Танки толкают друг друга при столкновении на ходу: толкаемый уступает,
если ему есть куда сдвинуться (не мешают стена или третий танк), иначе
толчок не проходит вовсе. За один тик оба продвигаются на долю попытки
хода — `GameConfig.PUSH_TRANSFER_FACTOR`, а не на всю дистанцию.
Готовность орудия — кольцо в HUD, заполняющееся по часовой стрелке при
перезарядке; вынесено в неподвижную полосу сверху, а не к танку на поле.

## Бонусы и опасности

На карте, помимо стен и точек старта, размечены подбираемые бонусы и
взрывоопасные бочки — подбирать бонусы может только танк игрока, ИИ их
игнорирует:

| Значок на `map.txt` | Что это         | Эффект                                                          |
|----------------------|------------------|------------------------------------------------------------------|
| `M`                  | Медпакет        | Восстанавливает `GameConfig.MEDKIT_HEAL_AMOUNT` очков здоровья, не выше максимума |
| `L`                  | Доп. жизнь      | Не расходуется сразу: копится в счётчике игрока и спасает от гибели в раунде — при уничтожении танк вместо выбывания тут же возрождается на стартовой позиции |
| `R`                  | Ускоренная перезарядка | На `GameConfig.RAPID_RELOAD_DURATION_SECONDS` секунд перезарядка идёт в `GameConfig.RAPID_RELOAD_MULTIPLIER` раз быстрее |
| `B`                  | Взрывная бочка  | Не действует сама по себе — но любое попадание пули (хоть игрока, хоть ИИ) детонирует её и наносит `ExplosiveBarrel.EXPLOSION_DAMAGE` урона всем танкам в радиусе `ExplosiveBarrel.EXPLOSION_RADIUS`, без разбора команд, включая того, кто стрелял |

Возрождение по доп. жизни сопровождается неуязвимостью на
`GameConfig.RESPAWN_INVULNERABILITY_SECONDS` (танк мигает, урон
игнорируется) — см. `Tank.grantInvulnerability`/`isInvulnerable`.

Детонация бочки рисует расширяющееся кольцо до границы
`ExplosiveBarrel.EXPLOSION_RADIUS` — `model.entity.Explosion`, живёт
`Explosion.DURATION_SECONDS` и убирается той же уборкой
`isDestroyed()`, что и пули с бонусами.

### Источники текстур

Значки бонусов и бочки — не нарисованный код, а готовые изображения из
свободных наборов:

| Элемент                  | Файл                                                                            | Источник | Автор | Лицензия |
|---------------------------|----------------------------------------------------------------------------------|----------|-------|----------|
| Медпакет                 | [`textures/pickup_medkit.png`](src/main/resources/ru/rsreu/ineprokin/textures/pickup_medkit.png) | [Generic Items — Kenney](https://opengameart.org/content/generic-items) | Kenney (kenney.nl) | CC0 1.0 |
| Взрывная бочка           | [`textures/barrel.png`](src/main/resources/ru/rsreu/ineprokin/textures/barrel.png) | [Topdown Tanks — Kenney](https://opengameart.org/content/topdown-tanks) | Kenney (kenney.nl) | CC0 1.0 |
| Доп. жизнь (сердце)      | [`textures/pickup_extra_life.png`](src/main/resources/ru/rsreu/ineprokin/textures/pickup_extra_life.png) | [Game icons (heart, diamond, star and lightning bolt)](https://opengameart.org/content/game-icons-heart-diamond-star-and-lightning-bolt) | PiXeRaT | CC BY-SA 4.0 |
| Ускоренная перезарядка (молния) | [`textures/pickup_rapid_reload.png`](src/main/resources/ru/rsreu/ineprokin/textures/pickup_rapid_reload.png) | [Game icons (heart, diamond, star and lightning bolt)](https://opengameart.org/content/game-icons-heart-diamond-star-and-lightning-bolt) | PiXeRaT | CC BY-SA 4.0 |

Оригиналы сердца и молнии вырезаны из анимированного спрайт-листа автора и
обрезаны по границе непрозрачных пикселей — сам рисунок не менялся.

## Запуск

Нужен только JDK 21+ — Maven устанавливать не обязательно: в проекте есть
Maven Wrapper (`mvnw`/`mvnw.cmd`), который сам скачает нужную версию Maven
при первом запуске. Если Maven уже установлен, `mvnw` можно заменить на `mvn`.

```powershell
# Windows (PowerShell/cmd)
.\mvnw.cmd javafx:run

# либо собрать один исполняемый jar со всем нужным внутри
.\mvnw.cmd package
java -jar target\javafx-of-tanks.jar
```

```bash
# Linux/macOS
./mvnw javafx:run
./mvnw package
java -jar target/javafx-of-tanks.jar
```

```bash
./mvnw test
```

## Архитектура

Игра построена по **MVVM** — границы между слоями
проведены так, чтобы `model`/`engine` можно было тестировать без единого
JavaFX-класса на classpath, а `view` не знал ни о чём, кроме своей
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
model/                 PlayerId, AboutInfo — вынесены из entity в корень, иначе capability↔entity дают цикл пакетов
model.capability/       Updatable, Destructible, Damageable, DamageSource, Fireable, BulletSpawnRequest
model.geometry/         Direction, Position, TileCoord
model.entity/           GameObject, Tank, Bullet, Pickup, PickupType, ExplosiveBarrel, Explosion, GameState
model.map/               GameMap, TileType, MapLoadException

engine/                CollisionService, EnemyAiService, GameWorld, GameWorldView, GameConfig
engine.ai/              AiStrategy, AiDecision, RandomAiStrategy
engine.spawn/           SpawnLocationFinder, DefaultSpawnLocationFinder

viewmodel/              GameViewModel, GameSimulationLoop, MenuViewModel, AboutViewModel
viewmodel.dto/           GameSnapshot, TankView, BulletView, PickupView, BarrelView, ExplosionView, PlayerHudInfo

view/                   GameView, GameRenderer, Sprite, TextureSprite, MenuView, AboutView
navigation/              Router, SceneRouter
config/                  ControlsConfig, PlayerControlScheme, SteeringInput, ThemeConfig, AboutContent
```

`GameWorld` оркестрирует три отдельных сервиса — `CollisionService`
(геометрия столкновений), `EnemyAiService` (тайминги решений ИИ),
`DefaultSpawnLocationFinder` (поиск точки возрождения) — каждый
тестируется отдельно от остальных.

### Функциональные интерфейсы вместо флагов и `instanceof`

Способности объектов выражены однометодными интерфейсами, а не булевыми
полями или цепочками проверок типа:

- `Damageable`/`DamageSource` — кто может получать урон, кто его наносит;
- `Fireable` — `Tank.tryFire()` сам считает точку вылета пули и
  возвращает `Optional<BulletSpawnRequest>`, так что этот расчёт не
  разбросан по местам, откуда стреляют;
- `AiStrategy` — тактика вражеского танка — это стратегия, а не набор
  условий внутри движка; в тестах ей на замену подставляется лямбда,
  которая никогда не двигается и не стреляет, без единого мока;
- `view.Sprite` — та же идея на слое отрисовки: `GameRenderer` вызывает
  `draw(...)`, не зная и не проверяя, текстура это (`TextureSprite`) или
  код (лямбда — так нарисовано кольцо взрыва, статичным изображением
  его не изобразить).

### Records как DTO между слоями

`GameMap`, `Position`, `TileCoord`, `BulletSpawnRequest`, `AiDecision`,
`AboutInfo` и, главное, `GameSnapshot`/`TankView`/`BulletView` — records.
`GameSnapshot` — это единственное, что видит `view`: он никогда не
держит в руках «живой» изменяемый `Tank` из движка, только его
неизменяемый снимок на момент последнего тика.

### Второй игрок подключается посреди партии

И `Tank`, и `GameMap`, и `GameWorld` адресуют участников через `PlayerId`
(`PLAYER_ONE` / `PLAYER_TWO`), а не единственным полем на танк игрока —
это и позволяет второму игроку появиться не с начала раунда, а в любой
момент, первым же нажатием своей клавиши:

- `GameMap` понимает второй маркер старта на карте — `2`;
- `GameWorld.isPlayerAvailable(id)` — есть ли для игрока точка старта на
  карте; `isPlayerActive(id)` — подключился ли уже; `activatePlayer(id)` —
  лениво создаёт танк на стартовой позиции (не-op, если игрок уже
  активен, карта его не поддерживает или раунд окончен);
- `GameView` шлёт запрос на подключение вместе с самой командой на любое
  нажатие клавиши ещё не подключившегося игрока — оба долетают до
  `GameWorld` в одном тике, поэтому танк реагирует на то же нажатие,
  которым появился;
- раунд заканчивается, только когда уничтожены **все** подключившиеся
  игроки — отсутствие второго или его более ранняя гибель не мешают
  первому доиграть в одиночку;
- очки/здоровье/перезарядка считаются по каждому игроку отдельно
  (`Bullet`/`BulletSpawnRequest` несут `PlayerId` стрелка) и показываются
  в HUD отдельным блоком — до подключения второго игрока на его месте
  приглашение.

## Конкурентность

Партия считается в фоновом потоке-демоне независимо от кадров экрана:

```
GameSimulationLoop implements Runnable   — тикает партию на ~60 Гц
        │  ScheduledExecutorService.scheduleAtFixedRate(this, …)
        ▼
   GameWorld.tick(deltaSeconds)          — реальное время, а не «кадры»
        │
        ▼  публикует AtomicReference<GameSnapshot>
   GameView (AnimationTimer, FX-поток)   — читает снимок каждый pulse
```

- **Шаг симуляции меряется реальным временем** (`System.nanoTime()`),
  не числом кадров — не завязан на FPS рендера.
- **Рендер синхронизирован с vsync через `AnimationTimer.handle(...)`**,
  который JavaFX сам вызывает раз за такт пайплайна — троттлинг кадров
  в прикладном коде не нужен.
- **Между потоками — ни одного `synchronized`.** Команды игрока живут
  в `ConcurrentHashMap.newKeySet()`/`AtomicBoolean` (пишет FX-поток,
  читает поток симуляции), состояние партии — в единственном
  `AtomicReference<GameSnapshot>` (record, неизменяем, `List.copyOf` в
  компактном конструкторе). Возврат в меню после экрана результатов —
  единственный переход на FX-поток, через `Platform.runLater(...)`.
- Поля, которые трогает только сам поток симуляции (таймер результатов,
  сглаженный FPS), — обычные `double`/`boolean` без `Atomic*`: делить
  их не с кем.

## Конфигурация без пересборки

Всё, что может поменять человек без Java-компилятора под рукой,
вынесено в текстовые ресурсы:

| Файл                  | Что описывает                                  |
|------------------------|------------------------------------------------|
| `map.txt`             | Карта: `#` стена, `P`/`2` старт игроков, `E` враг, `M`/`L`/`R` бонусы, `B` бочка |
| `controls.properties` | Раскладка клавиш (имена `javafx.scene.input.KeyCode`) |
| `theme.properties`    | Цветовая палитра экрана (`#RRGGBB[AA]`)         |
| `about.properties`    | Текст экрана «О программе»                      |

## Тесты

`model`/`engine` не содержат импортов JavaFX — юнит-тесты (JUnit 5) не
запускают графическое приложение:

- `GameMapTest` — разбор карты и её ошибок, бонусы и бочки на клетках;
- `TankTest` — перезарядка, урон, плавный поворот, лечение, ускоренная
  перезарядка, неуязвимость;
- `CollisionServiceTest` — стены, толкание танков, дружественный огонь,
  взрыв бочки (урон по радиусу, начисление убийства), сбор бонусов;
- `GameWorldTest` — подключение и гибель второго игрока, очки верному
  стрелку, возрождение по доп. жизни вместо конца раунда; собирает
  `GameWorld` с ИИ-стратегией-заглушкой (лямбдой) вместо рандомизированной.

```bash
mvn test
```

## CI/CD

Два workflow в [`.github/workflows`](.github/workflows):

| Файл | Когда запускается | Что делает |
|------|--------------------|------------|
| `maven.yml` | Любой `push`, любой pull request в `main` | `mvnw clean test` (JDK 21) — все 50 тестов из раздела «Тесты» выше; отчёт покрытия JaCoCo (`target/site/jacoco/`) прикладывается к запуску как скачиваемый артефакт |
| `release.yml` | `push` тега вида `v*` (например, `v1.0.0`) | `mvnw clean package` и публикация `javafx-of-tanks.jar` в GitHub Releases с автоматически сгенерированными release notes |

Статус последнего прогона — значок в самом верху этого файла.

## Автор

Непрокин Иван
