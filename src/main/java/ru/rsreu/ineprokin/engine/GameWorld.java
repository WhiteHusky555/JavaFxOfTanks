package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.engine.ai.RandomAiStrategy;
import ru.rsreu.ineprokin.engine.spawn.DefaultSpawnLocationFinder;
import ru.rsreu.ineprokin.engine.spawn.SpawnLocationFinder;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.capability.Destructible;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.ExplosiveBarrel;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.Pickup;
import ru.rsreu.ineprokin.model.entity.PickupType;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Direction;
import ru.rsreu.ineprokin.model.geometry.Position;
import ru.rsreu.ineprokin.model.geometry.TileCoord;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Корень доменной модели одной партии: карта, танки, пули, бонусы, бочки,
 * счёт и фаза игры. Сам не занимается ни физикой столкновений, ни ИИ, ни
 * поиском точки возрождения — эти обязанности переданы {@link CollisionService},
 * {@link EnemyAiService} и {@link SpawnLocationFinder} соответственно.
 * {@code GameWorld} — только их оркестратор.
 * <p>
 * Игроки адресуются через {@link PlayerId}, а не единственным полем танка
 * игрока. {@link PlayerId#PLAYER_ONE} активен с первого кадра партии, если
 * карта вообще определяет для него точку старта; {@link PlayerId#PLAYER_TWO}
 * присоединяется позже, посреди партии, через {@link #activatePlayer}.
 * Раунд заканчивается, когда уничтожены все танки игроков, которые
 * действительно подключились — если второй игрок так и не присоединился,
 * его отсутствие никак не влияет на условие поражения.
 */
public final class GameWorld implements GameWorldView {

    private final GameMap map;
    private final CollisionService collisionService;
    private final EnemyAiService enemyAiService;
    private final SpawnLocationFinder spawnLocationFinder;
    private final Random random;

    private final List<Tank> tanks = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<ExplosiveBarrel> barrels = new ArrayList<>();
    private final Map<PlayerId, Tank> playerTanks = new EnumMap<>(PlayerId.class);
    private final Map<PlayerId, Integer> scores = new EnumMap<>(PlayerId.class);
    private final Map<PlayerId, Integer> extraLives = new EnumMap<>(PlayerId.class);

    private GameState state = GameState.PLAYING;

    public GameWorld(GameMap map, CollisionService collisionService, EnemyAiService enemyAiService,
                      SpawnLocationFinder spawnLocationFinder, Random random) {
        this.map = map;
        this.collisionService = collisionService;
        this.enemyAiService = enemyAiService;
        this.spawnLocationFinder = spawnLocationFinder;
        this.random = random;
        this.reset();
    }

    /** Собирает мир с типовыми коллаборантами — удобно для приложения и для тестов, не проверяющих ИИ. */
    public static GameWorld createDefault(GameMap map) {
        Random random = new Random();
        CollisionService collisionService = new CollisionService();
        EnemyAiService enemyAiService = new EnemyAiService(new RandomAiStrategy(), collisionService, random);
        return new GameWorld(map, collisionService, enemyAiService, new DefaultSpawnLocationFinder(), random);
    }

    /** Возвращает партию в начальное состояние: первый игрок на стартовой позиции, счёт — на ноль. */
    public void reset() {
        this.tanks.clear();
        this.bullets.clear();
        this.pickups.clear();
        this.barrels.clear();
        this.playerTanks.clear();
        this.scores.clear();
        this.extraLives.clear();
        for (PlayerId playerId : PlayerId.values()) {
            this.scores.put(playerId, 0);
        }
        this.state = GameState.PLAYING;

        this.spawnPlayerTank(PlayerId.PLAYER_ONE);

        for (TileCoord enemyStart : this.map.enemyStarts()) {
            Position position = enemyStart.toPixelCenter(GameMap.TILE_SIZE, Tank.SIZE);
            this.tanks.add(Tank.enemy(position.x(), position.y(), this.randomDirection().headingDegrees()));
        }
        for (PickupType type : PickupType.values()) {
            for (TileCoord coord : this.map.pickupStarts(type)) {
                Position position = coord.toPixelCenter(GameMap.TILE_SIZE, Pickup.SIZE);
                this.pickups.add(new Pickup(position.x(), position.y(), type));
            }
        }
        for (TileCoord coord : this.map.barrelStarts()) {
            Position position = coord.toPixelCenter(GameMap.TILE_SIZE, ExplosiveBarrel.SIZE);
            this.barrels.add(new ExplosiveBarrel(position.x(), position.y()));
        }
    }

    /** Есть ли на карте вообще точка старта для этого игрока — не зависит от того, подключился ли он. */
    public boolean isPlayerAvailable(PlayerId playerId) {
        return this.map.playerStarts().containsKey(playerId);
    }

    /** Уже управляет ли этим игроком чей-то танк на поле. */
    public boolean isPlayerActive(PlayerId playerId) {
        return this.playerTanks.containsKey(playerId);
    }

    /**
     * Подключает игрока {@code playerId} посреди партии: создаёт ему танк на
     * стартовой позиции с карты. Не делает ничего, если для этого игрока нет
     * точки старта, он уже подключён или партия уже окончена.
     */
    public void activatePlayer(PlayerId playerId) {
        if (this.isPlayerActive(playerId) || this.state == GameState.GAME_OVER) {
            return;
        }
        this.spawnPlayerTank(playerId);
    }

    private void spawnPlayerTank(PlayerId playerId) {
        TileCoord start = this.map.playerStarts().get(playerId);
        if (start == null) {
            return;
        }
        Position position = start.toPixelCenter(GameMap.TILE_SIZE, Tank.SIZE);
        Tank tank = new Tank(position.x(), position.y(), Direction.UP.headingDegrees(), playerId);
        this.playerTanks.put(playerId, tank);
        this.tanks.add(tank);
    }

    /**
     * Управляет танком игрока {@code playerId} — вызывается каждый кадр, пока зажата
     * хоть одна из клавиш управления. Танк не прыгает мгновенно в сторону: он
     * поворачивается на месте ({@code turnDirection}, знак задаёт сторону) и
     * едет вперёд/назад вдоль своего курса ({@code throttle}: {@code >0} — вперёд,
     * {@code <0} — назад, задним ходом медленнее).
     */
    public void steerPlayer(PlayerId playerId, double turnDirection, double throttle, double deltaTimeSeconds) {
        Tank tank = this.playerTanks.get(playerId);
        if (tank == null || this.state != GameState.PLAYING || tank.isDestroyed()) {
            return;
        }
        if (turnDirection != 0) {
            tank.rotate(turnDirection, deltaTimeSeconds);
        }
        if (throttle != 0) {
            double speedFactor = throttle < 0 ? GameConfig.REVERSE_SPEED_FACTOR : 1.0;
            double distance = tank.getSpeed() * speedFactor * deltaTimeSeconds * Math.signum(throttle);
            double newX = tank.getX() + tank.forwardX() * distance;
            double newY = tank.getY() + tank.forwardY() * distance;
            this.collisionService.tryMoveTank(tank, newX, newY, this.map, this.tanks);
        }
    }

    public void firePlayer(PlayerId playerId) {
        Tank tank = this.playerTanks.get(playerId);
        if (tank == null || this.state != GameState.PLAYING) {
            return;
        }
        tank.tryFire().ifPresent(this::spawnBullet);
    }

    public void togglePause() {
        if (this.state == GameState.PLAYING) {
            this.state = GameState.PAUSED;
        } else if (this.state == GameState.PAUSED) {
            this.state = GameState.PLAYING;
        }
    }

    /** Продвигает партию на {@code deltaTimeSeconds}: перезарядка, полёт пуль, ИИ, столкновения, бонусы, уборка трупов. */
    public void tick(double deltaTimeSeconds) {
        if (this.state != GameState.PLAYING) {
            return;
        }

        for (Tank tank : this.tanks) {
            tank.update(deltaTimeSeconds);
        }
        for (Bullet bullet : this.bullets) {
            bullet.update(deltaTimeSeconds);
        }

        for (BulletSpawnRequest request : this.enemyAiService.update(deltaTimeSeconds, this)) {
            this.spawnBullet(request);
        }

        for (PlayerId scorer : this.collisionService.resolveBulletHits(this.bullets, this.tanks, this.map)) {
            this.awardKill(scorer);
        }
        for (PlayerId scorer : this.collisionService.resolveBulletBarrelHits(this.bullets, this.barrels, this.tanks)) {
            this.awardKill(scorer);
        }
        for (CollisionService.PickupCollection collection : this.collisionService.resolvePickupCollisions(this.pickups, this.tanks)) {
            this.applyPickupEffect(collection.tank(), collection.type());
        }

        this.collisionService.separateOverlappingTanks(this.tanks, this.map);

        this.reviveDestroyedPlayers();

        this.bullets.removeIf(Destructible::isDestroyed);
        this.tanks.removeIf(Destructible::isDestroyed);
        this.pickups.removeIf(Destructible::isDestroyed);
        this.barrels.removeIf(Destructible::isDestroyed);

        if (this.allPlayersDestroyed()) {
            this.state = GameState.GAME_OVER;
        }
    }

    private void awardKill(PlayerId scorer) {
        this.scores.merge(scorer, GameConfig.SCORE_PER_KILL, Integer::sum);
        this.spawnReplacementEnemy();
    }

    private void applyPickupEffect(Tank tank, PickupType type) {
        switch (type) {
            case MEDKIT -> tank.heal(GameConfig.MEDKIT_HEAL_AMOUNT);
            case RAPID_RELOAD -> tank.applyRapidReload(GameConfig.RAPID_RELOAD_DURATION_SECONDS, GameConfig.RAPID_RELOAD_MULTIPLIER);
            case EXTRA_LIFE -> tank.getPlayerId().ifPresent(id -> this.extraLives.merge(id, 1, Integer::sum));
        }
    }

    /**
     * Игрок с погибшим танком и запасной жизнью в кармане возрождается на стартовой
     * позиции — а не выбывает. Возрождённый танк получает кратковременную
     * неуязвимость, чтобы не погибнуть от того же выстрела, что настиг его на
     * старом месте.
     */
    private void reviveDestroyedPlayers() {
        for (PlayerId playerId : Set.copyOf(this.playerTanks.keySet())) {
            Tank tank = this.playerTanks.get(playerId);
            if (tank.isDestroyed() && this.consumeExtraLife(playerId)) {
                this.spawnPlayerTank(playerId);
                this.playerTanks.get(playerId).grantInvulnerability(GameConfig.RESPAWN_INVULNERABILITY_SECONDS);
            }
        }
    }

    private boolean consumeExtraLife(PlayerId playerId) {
        int lives = this.extraLives.getOrDefault(playerId, 0);
        if (lives <= 0) {
            return false;
        }
        this.extraLives.put(playerId, lives - 1);
        return true;
    }

    private boolean allPlayersDestroyed() {
        return !this.playerTanks.isEmpty() && this.playerTanks.values().stream().allMatch(Tank::isDestroyed);
    }

    private void spawnBullet(BulletSpawnRequest request) {
        this.bullets.add(new Bullet(request.x(), request.y(), request.headingDegrees(), request.shooterId()));
    }

    private void spawnReplacementEnemy() {
        this.spawnLocationFinder.findSpawn(this.map, this.tanks, this.random)
                .ifPresent(position -> this.tanks.add(
                        Tank.enemy(position.x(), position.y(), this.randomDirection().headingDegrees())));
    }

    private Direction randomDirection() {
        Direction[] values = Direction.values();
        return values[this.random.nextInt(values.length)];
    }

    @Override
    public GameMap getMap() {
        return this.map;
    }

    @Override
    public List<Tank> getTanks() {
        return Collections.unmodifiableList(this.tanks);
    }

    public List<Bullet> getBullets() {
        return Collections.unmodifiableList(this.bullets);
    }

    public List<Pickup> getPickups() {
        return Collections.unmodifiableList(this.pickups);
    }

    public List<ExplosiveBarrel> getBarrels() {
        return Collections.unmodifiableList(this.barrels);
    }

    @Override
    public GameState getState() {
        return this.state;
    }

    /** Суммарный счёт всех игроков — например, для итогового экрана. */
    @Override
    public int getScore() {
        return this.scores.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getScore(PlayerId playerId) {
        return this.scores.getOrDefault(playerId, 0);
    }

    public int getExtraLives(PlayerId playerId) {
        return this.extraLives.getOrDefault(playerId, 0);
    }

    public int getPlayerHealth(PlayerId playerId) {
        if (this.state == GameState.GAME_OVER) {
            return 0;
        }
        Tank tank = this.playerTanks.get(playerId);
        return tank == null ? 0 : tank.getHealth();
    }

    public int getPlayerMaxHealth(PlayerId playerId) {
        Tank tank = this.playerTanks.get(playerId);
        return tank == null ? Tank.MAX_HEALTH : tank.getMaxHealth();
    }

    /** Доля перезарядки орудия игрока: {@code 1.0} — готово стрелять, {@code 0.0} — только что выстрелил. */
    public double getPlayerReloadProgress(PlayerId playerId) {
        Tank tank = this.playerTanks.get(playerId);
        return tank == null ? 1.0 : tank.getReloadProgress();
    }
}
