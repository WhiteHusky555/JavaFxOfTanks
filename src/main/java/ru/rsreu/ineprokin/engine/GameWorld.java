package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.engine.ai.RandomAiStrategy;
import ru.rsreu.ineprokin.engine.spawn.DefaultSpawnLocationFinder;
import ru.rsreu.ineprokin.engine.spawn.SpawnLocationFinder;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.capability.Destructible;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.PlayerId;
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

/**
 * Корень доменной модели одной партии: карта, танки, пули, счёт и фаза игры.
 * Сам не занимается ни физикой столкновений, ни ИИ, ни поиском точки
 * возрождения — эти обязанности переданы {@link CollisionService},
 * {@link EnemyAiService} и {@link SpawnLocationFinder} соответственно.
 * {@code GameWorld} — только их оркестратор.
 * <p>
 * Игроки адресуются через {@link PlayerId}, а не как единственный
 * {@code playerTank} — раунд завершается, когда уничтожены все танки
 * игроков, присутствующих на карте, что уже сегодня корректно работает
 * и для одного, и (при появлении второй точки старта) для двух игроков.
 */
public final class GameWorld implements GameWorldView {

    private final GameMap map;
    private final CollisionService collisionService;
    private final EnemyAiService enemyAiService;
    private final SpawnLocationFinder spawnLocationFinder;
    private final Random random;

    private final List<Tank> tanks = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final Map<PlayerId, Tank> playerTanks = new EnumMap<>(PlayerId.class);

    private GameState state = GameState.PLAYING;
    private int score;

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

    /** Возвращает партию в начальное состояние: танки — на стартовые позиции, счёт — на ноль. */
    public void reset() {
        this.tanks.clear();
        this.bullets.clear();
        this.playerTanks.clear();
        this.score = 0;
        this.state = GameState.PLAYING;

        for (Map.Entry<PlayerId, TileCoord> entry : this.map.playerStarts().entrySet()) {
            Position start = entry.getValue().toPixelCenter(GameMap.TILE_SIZE, Tank.SIZE);
            Tank tank = new Tank(start.x(), start.y(), Direction.UP.headingDegrees(), entry.getKey());
            this.playerTanks.put(entry.getKey(), tank);
            this.tanks.add(tank);
        }

        for (TileCoord enemyStart : this.map.enemyStarts()) {
            Position position = enemyStart.toPixelCenter(GameMap.TILE_SIZE, Tank.SIZE);
            this.tanks.add(Tank.enemy(position.x(), position.y(), this.randomDirection().headingDegrees()));
        }
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

    /** Продвигает партию на {@code deltaTimeSeconds}: перезарядка, полёт пуль, ИИ, столкновения, уборка трупов. */
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

        List<Tank> killedByPlayer = this.collisionService.resolveBulletHits(this.bullets, this.tanks, this.map);
        for (int i = 0; i < killedByPlayer.size(); i++) {
            this.score += GameConfig.SCORE_PER_KILL;
            this.spawnReplacementEnemy();
        }

        this.collisionService.separateOverlappingTanks(this.tanks, this.map);

        this.bullets.removeIf(Destructible::isDestroyed);
        this.tanks.removeIf(Destructible::isDestroyed);

        if (this.allPlayersDestroyed()) {
            this.state = GameState.GAME_OVER;
        }
    }

    private boolean allPlayersDestroyed() {
        return this.playerTanks.values().stream().allMatch(Tank::isDestroyed);
    }

    private void spawnBullet(BulletSpawnRequest request) {
        this.bullets.add(new Bullet(request.x(), request.y(), request.headingDegrees(), request.fromPlayer()));
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

    @Override
    public GameState getState() {
        return this.state;
    }

    @Override
    public int getScore() {
        return this.score;
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
