package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.capability.Damageable;
import ru.rsreu.ineprokin.model.capability.Fireable;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.util.Optional;

/**
 * Танк — игрока или противника. Модель хранит только собственное состояние
 * (здоровье, перезарядку, направление) и не знает ни о карте, ни о других
 * танках: перемещение по полю — забота {@code engine.CollisionService} /
 * {@code engine.GameWorld}, которые вызывают {@link #setPosition(double, double)}
 * только после проверки, что клетка свободна.
 * <p>
 * Принадлежность танка игроку выражена не булевым флагом, а {@code null}-able
 * {@link PlayerId}: {@code null} — танк управляется ИИ, конкретное значение —
 * танк принадлежит соответствующему игроку. Это и есть та самая точка
 * расширения под второго локального игрока — {@link PlayerId#PLAYER_TWO}
 * уже существует, просто пока никто не создаёт танк с этим значением.
 */
public final class Tank extends GameObject implements Damageable, Fireable {

    public static final int MAX_HEALTH = 100;
    public static final double SIZE = 36.0;

    public static final double PLAYER_SPEED_PX_PER_SEC = 220.0;
    public static final double ENEMY_SPEED_PX_PER_SEC = 150.0;

    public static final double PLAYER_RELOAD_SECONDS = 0.8;
    public static final double ENEMY_RELOAD_SECONDS = 1.5;

    /** Насколько дульный срез вынесен вперёд от центра танка при рождении пули. */
    private static final double MUZZLE_OFFSET = SIZE / 2.0 + 1.0;

    private final PlayerId playerId;
    private final double speed;
    private final double reloadSeconds;

    private Direction direction;
    private int health;
    private double timeSinceLastShot;

    /**
     * @param playerId {@code null} для танка под управлением ИИ, иначе — владелец-игрок
     */
    public Tank(double startX, double startY, Direction startDirection, PlayerId playerId) {
        super(startX, startY);
        this.direction = startDirection;
        this.playerId = playerId;
        this.health = MAX_HEALTH;
        this.speed = playerId != null ? PLAYER_SPEED_PX_PER_SEC : ENEMY_SPEED_PX_PER_SEC;
        this.reloadSeconds = playerId != null ? PLAYER_RELOAD_SECONDS : ENEMY_RELOAD_SECONDS;
        // Танк готов выстрелить сразу после появления на поле.
        this.timeSinceLastShot = this.reloadSeconds;
    }

    /** Удобный фабричный метод для читаемости в местах, создающих вражеские танки. */
    public static Tank enemy(double startX, double startY, Direction startDirection) {
        return new Tank(startX, startY, startDirection, null);
    }

    @Override
    public void update(double deltaTimeSeconds) {
        if (this.timeSinceLastShot < this.reloadSeconds) {
            this.timeSinceLastShot += deltaTimeSeconds;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.health <= 0;
    }

    public boolean canFire() {
        return this.timeSinceLastShot >= this.reloadSeconds;
    }

    @Override
    public Optional<BulletSpawnRequest> tryFire() {
        if (!this.canFire()) {
            return Optional.empty();
        }
        this.timeSinceLastShot = 0.0;

        double centerX = this.getX() + SIZE / 2.0;
        double centerY = this.getY() + SIZE / 2.0;
        double bulletX = centerX + this.direction.dx() * MUZZLE_OFFSET;
        double bulletY = centerY + this.direction.dy() * MUZZLE_OFFSET;

        return Optional.of(new BulletSpawnRequest(bulletX, bulletY, this.direction, this.isPlayer()));
    }

    @Override
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public Direction getDirection() {
        return this.direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /** {@code true}, если танк принадлежит любому из игроков (не ИИ). */
    public boolean isPlayer() {
        return this.playerId != null;
    }

    public Optional<PlayerId> getPlayerId() {
        return Optional.ofNullable(this.playerId);
    }

    public int getHealth() {
        return this.health;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public double getSpeed() {
        return this.speed;
    }
}
