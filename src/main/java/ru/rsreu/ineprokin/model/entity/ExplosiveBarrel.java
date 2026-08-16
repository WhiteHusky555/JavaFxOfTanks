package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.Damageable;

/**
 * Бочка стоит на карте, пока в неё не попадёт пуля — любая, хоть игрока,
 * хоть противника. От одного попадания она разносится и задевает взрывом
 * всех, кто окажется в радиусе, не разбирая своих и чужих: подробности
 * взрыва — забота {@code engine.CollisionService}, сама бочка лишь несёт
 * геометрию (размер, радиус, урон) и умеет ломаться.
 */
public final class ExplosiveBarrel extends GameObject implements Damageable {

    public static final double SIZE = 30.0;
    public static final double EXPLOSION_RADIUS = 70.0;
    public static final int EXPLOSION_DAMAGE = 60;

    private boolean destroyed;

    public ExplosiveBarrel(double x, double y) {
        super(x, y);
    }

    @Override
    public void update(double deltaTimeSeconds) {
        // Бочка неподвижна — реализация метода нужна только для контракта Updatable.
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** Бочку разносит любое попадание — величина урона не имеет значения. */
    @Override
    public void takeDamage(int amount) {
        this.destroyed = true;
    }
}
