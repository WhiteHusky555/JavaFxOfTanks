package ru.rsreu.ineprokin.model.entity;

/**
 * Разновидность разового бонуса, который подбирает танк игрока, проехав
 * сверху. Сам эффект (сколько лечит, насколько ускоряет перезарядку) —
 * забота {@code engine.GameConfig} и {@code engine.GameWorld}, тип здесь
 * лишь помечает, какой это бонус.
 */
public enum PickupType {
    MEDKIT,
    EXTRA_LIFE,
    RAPID_RELOAD
}
