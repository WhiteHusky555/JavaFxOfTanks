package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.DamageSource;
import ru.rsreu.ineprokin.model.geometry.Direction;

/**
 * Пуля летит по прямой в направлении, заданном при выстреле, до тех пор пока
 * её не уничтожит {@code engine.CollisionService} (столкновение со стеной
 * или танком).
 */
public final class Bullet extends GameObject implements DamageSource {

    public static final double SPEED_PX_PER_SEC = 480.0;
    public static final double RADIUS = 3.0;
    public static final int DAMAGE = 25;

    private final Direction direction;
    private final boolean fromPlayer;
    private boolean destroyed;

    public Bullet(double startX, double startY, Direction direction, boolean fromPlayer) {
        super(startX, startY);
        this.direction = direction;
        this.fromPlayer = fromPlayer;
    }

    @Override
    public void update(double deltaTimeSeconds) {
        double distance = SPEED_PX_PER_SEC * deltaTimeSeconds;
        this.setPosition(this.getX() + this.direction.dx() * distance, this.getY() + this.direction.dy() * distance);
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isFromPlayer() {
        return fromPlayer;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public int getDamage() {
        return DAMAGE;
    }
}
