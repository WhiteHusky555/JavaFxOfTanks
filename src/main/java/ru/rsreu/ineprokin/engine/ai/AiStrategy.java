package ru.rsreu.ineprokin.engine.ai;

import ru.rsreu.ineprokin.engine.GameWorldView;
import ru.rsreu.ineprokin.model.entity.Tank;

import java.util.Random;

/**
 * Тактика поведения вражеского танка. Вынесена в функциональный интерфейс,
 * а не зашита в {@code EnemyAiService}, чтобы поведение можно было подменить —
 * например, на детерминированную лямбду в тестах или на более умную тактику
 * в будущем — не трогая код, который вызывает таймеры принятия решений.
 */
@FunctionalInterface
public interface AiStrategy {

    AiDecision decide(Tank tank, GameWorldView world, Random random);
}
