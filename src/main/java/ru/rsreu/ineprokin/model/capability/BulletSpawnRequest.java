package ru.rsreu.ineprokin.model.capability;

import ru.rsreu.ineprokin.model.geometry.Direction;

/**
 * Запрос на создание пули, возвращаемый {@link Fireable#tryFire()}. Несёт уже
 * готовую точку вылета — у дульного среза, а не из центра танка, — так что
 * этот расчёт не нужно повторять отдельно для игрока и для каждого врага.
 */
public record BulletSpawnRequest(double x, double y, Direction direction, boolean fromPlayer) {
}
