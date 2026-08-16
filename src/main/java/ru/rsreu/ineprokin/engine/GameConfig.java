package ru.rsreu.ineprokin.engine;

/**
 * Числовые параметры баланса игры, которые не являются неотъемлемым свойством
 * одной сущности (в отличие, например, от скорости танка — она хранится
 * в {@code Tank}). Собраны в одном месте, чтобы настройку сложности не
 * пришлось искать по всему движку.
 */
public final class GameConfig {

    /** Очков за уничтожение вражеского танка. */
    public static final int SCORE_PER_KILL = 100;

    /** Сколько секунд показывается экран результатов перед возвратом в меню. */
    public static final double RESULTS_SCREEN_SECONDS = 3.0;

    /** Как часто (в среднем, в секундах) вражеский танк принимает новое решение о движении. */
    public static final double AI_MOVE_DECISION_MIN_SECONDS = 0.4;
    public static final double AI_MOVE_DECISION_MAX_SECONDS = 1.2;

    /** Как часто вражеский танк проверяет, не стоит ли выстрелить. */
    public static final double AI_FIRE_CHECK_MIN_SECONDS = 0.5;
    public static final double AI_FIRE_CHECK_MAX_SECONDS = 1.5;

    /** Вероятность того, что принятое решение — действительно двигаться, а не постоять на месте. */
    public static final double AI_MOVE_CHANCE = 0.7;

    /** Вероятность выстрела при срабатывании таймера проверки (при готовом орудии). */
    public static final double AI_FIRE_CHANCE = 0.5;

    /** Вероятность того, что при движении танк выберет направление в сторону игрока, а не случайное. */
    public static final double AI_CHASE_BIAS = 0.35;

    private GameConfig() {
    }
}
