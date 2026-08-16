package ru.rsreu.ineprokin.model.capability;

import ru.rsreu.ineprokin.model.geometry.Direction;

/**
 * Запрос на создание пули, возвращаемый {@link Fireable#tryFire()}. Инкапсулирует
 * расчёт точки вылета пули (у дульного среза, а не из центра танка), который
 * в исходной C++-версии был продублирован в {@code GameModel::playerFire} и
 * {@code GameModel::updateEnemies} дословно.
 */
public record BulletSpawnRequest(double x, double y, Direction direction, boolean fromPlayer) {
}
