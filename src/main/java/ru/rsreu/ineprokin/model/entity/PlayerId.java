package ru.rsreu.ineprokin.model.entity;

/**
 * Идентификатор игрока-человека, управляющего танком. У ИИ-танков вместо
 * этого значения — {@code null} (см. {@link Tank#getPlayerId()}).
 * <p>
 * Сейчас в партии реально участвует только {@link #PLAYER_ONE} — второй
 * слот существует, чтобы добавить локального кооперативного игрока можно
 * было, не переделывая {@code Tank}, {@code GameMap} и {@code GameWorld}:
 * достаточно добавить точку старта на карте и провести управление от
 * второго набора клавиш до {@code GameWorld.movePlayer(PLAYER_TWO, ...)}.
 */
public enum PlayerId {
    PLAYER_ONE,
    PLAYER_TWO
}
