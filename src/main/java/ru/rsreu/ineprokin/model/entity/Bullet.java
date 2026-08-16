package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.capability.DamageSource;

import java.util.Optional;

/**
 * Пуля летит по прямой вдоль курса, заданного при выстреле, до тех пор пока
 * её не уничтожит {@code engine.CollisionService} (столкновение со стеной
 * или танком).
 * <p>
 * Как и у {@link Tank}, принадлежность пули игроку выражена {@code null}-able
 * {@link PlayerId}, а не булевым флагом — это позволяет засчитать очки за
 * попадание тому игроку, который действительно выстрелил, а не любому игроку вообще.
 */
public final class Bullet extends GameObject implements DamageSource {

    public static final double SPEED_PX_PER_SEC = 480.0;
    public static final double RADIUS = 3.0;
    public static final int DAMAGE = 25;

    private final double headingDegrees;
    private final PlayerId shooterId;
    private boolean destroyed;

    /**
     * @param headingDegrees курс в градусах по часовой стрелке от направления "вверх"
     * @param shooterId      {@code null}, если пулю выпустил танк под управлением ИИ
     */
    public Bullet(double startX, double startY, double headingDegrees, PlayerId shooterId) {
        super(startX, startY);
        this.headingDegrees = headingDegrees;
        this.shooterId = shooterId;
    }

    @Override
    public void update(double deltaTimeSeconds) {
        double distance = SPEED_PX_PER_SEC * deltaTimeSeconds;
        double radians = Math.toRadians(this.headingDegrees);
        this.setPosition(this.getX() + Math.sin(radians) * distance, this.getY() - Math.cos(radians) * distance);
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    public void destroy() {
        this.destroyed = true;
    }

    public boolean isFromPlayer() {
        return this.shooterId != null;
    }

    public Optional<PlayerId> getShooterId() {
        return Optional.ofNullable(this.shooterId);
    }

    public double getHeadingDegrees() {
        return this.headingDegrees;
    }

    @Override
    public int getDamage() {
        return DAMAGE;
    }
}
