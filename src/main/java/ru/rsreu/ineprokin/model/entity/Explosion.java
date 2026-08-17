package ru.rsreu.ineprokin.model.entity;

/**
 * Кратковременный визуальный эффект взрыва бочки: расширяющееся кольцо на
 * радиусе поражения, которое гаснет по истечении {@link #DURATION_SECONDS}.
 * Сам урон уже нанесён {@code engine.CollisionService} в момент детонации —
 * этот объект ничего не задевает, он лишь показывает игроку, докуда долетела
 * взрывная волна, и исчезает точно так же, как подобранный бонус или
 * долетевшая пуля: через {@link #isDestroyed()} и общую уборку в
 * {@code engine.GameWorld}.
 */
public final class Explosion extends GameObject {

    public static final double DURATION_SECONDS = 0.4;

    private final double radius;
    private double remainingSeconds = Explosion.DURATION_SECONDS;

    public Explosion(double x, double y, double radius) {
        super(x, y);
        this.radius = radius;
    }

    public double getRadius() {
        return this.radius;
    }

    /** Доля прожитого времени эффекта: {@code 0} — только вспыхнул, {@code 1} — вот-вот погаснет. */
    public double getProgress() {
        return 1.0 - this.remainingSeconds / Explosion.DURATION_SECONDS;
    }

    @Override
    public void update(double deltaTimeSeconds) {
        this.remainingSeconds -= deltaTimeSeconds;
    }

    @Override
    public boolean isDestroyed() {
        return this.remainingSeconds <= 0;
    }
}
