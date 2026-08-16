package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.Destructible;

/**
 * Бонус, лежащий на карте до тех пор, пока танк игрока не подберёт его,
 * проехав сверху. {@link #isDestroyed()} (унаследован от {@link GameObject},
 * который уже реализует {@link Destructible}) здесь означает "подобран" —
 * {@code engine.GameWorld} убирает подобранные бонусы из мира той же
 * веткой {@code removeIf(Destructible::isDestroyed)}, что и уничтоженные
 * пули и танки.
 */
public final class Pickup extends GameObject {

    public static final double SIZE = 24.0;

    private final PickupType type;
    private boolean collected;

    public Pickup(double x, double y, PickupType type) {
        super(x, y);
        this.type = type;
    }

    @Override
    public void update(double deltaTimeSeconds) {
        // Бонус лежит неподвижно — реализация метода нужна только для контракта Updatable.
    }

    @Override
    public boolean isDestroyed() {
        return this.collected;
    }

    public void collect() {
        this.collected = true;
    }

    public PickupType getType() {
        return this.type;
    }
}
