package ru.rsreu.ineprokin.model.capability;

/** Способность объекта наносить урон при попадании. Несёт её пуля. */
@FunctionalInterface
public interface DamageSource {

    int getDamage();
}
