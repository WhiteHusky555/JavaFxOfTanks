package ru.rsreu.ineprokin.engine.ai;

import ru.rsreu.ineprokin.model.geometry.Direction;

/**
 * Результат "размышления" вражеского танка за один опрос {@link AiStrategy}:
 * в какую сторону двигаться (и стоит ли вообще), а также — хочет ли танк
 * выстрелить прямо сейчас.
 */
public record AiDecision(Direction direction, boolean moving, boolean wantsToFire) {
}
