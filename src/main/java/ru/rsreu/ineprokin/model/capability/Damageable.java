package ru.rsreu.ineprokin.model.capability;

/** Способность объекта получать урон. Сейчас её несёт только танк. */
@FunctionalInterface
public interface Damageable {

    void takeDamage(int amount);
}
