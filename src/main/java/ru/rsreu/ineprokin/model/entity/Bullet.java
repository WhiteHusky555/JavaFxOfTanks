package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.DamageSource;

/**
 * Пуля летит по прямой вдоль курса, заданного при выстреле, до тех пор пока
 * её не уничтожит {@code engine.CollisionService} (столкновение со стеной
 * или танком).
 */
public final class Bullet extends GameObject implements DamageSource {

    public static final double SPEED_PX_PER_SEC = 480.0;
    public static final double RADIUS = 3.0;
    public static final int DAMAGE = 25;

    private final double headingDegrees;
    private final boolean fromPlayer;
    private boolean destroyed;

    /** @param headingDegrees курс в градусах по часовой стрелке от направления "вверх" */
    public Bullet(double startX, double startY, double headingDegrees, boolean fromPlayer) {
        super(startX, startY);
        this.headingDegrees = headingDegrees;
        this.fromPlayer = fromPlayer;
    }

    @Override
    public void update(double deltaTimeSeconds) {
        double distance = SPEED_PX_PER_SEC * deltaTimeSeconds;
        double radians = Math.toRadians(this.headingDegrees);
        this.setPosition(this.getX() + Math.sin(radians) * distance, this.getY() - Math.cos(radians) * distance);
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

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    @Override
    public int getDamage() {
        return DAMAGE;
    }
}
