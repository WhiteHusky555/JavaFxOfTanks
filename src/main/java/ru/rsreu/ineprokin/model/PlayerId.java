package ru.rsreu.ineprokin.model;

/**
 * Идентификатор игрока-человека, управляющего танком. У ИИ-танков вместо
 * этого значения — {@code null} (см. {@code Tank.getPlayerId()}).
 * <p>
 * Живёт в корне пакета {@code model}, а не в {@code model.entity}: и
 * {@code model.entity} (танк, пуля), и {@code model.capability} (запрос на
 * создание пули) ссылаются на этот идентификатор, а сам он не зависит ни
 * от того, ни от другого — иначе получился бы цикл между пакетами.
 */
public enum PlayerId {
    PLAYER_ONE,
    PLAYER_TWO
}
