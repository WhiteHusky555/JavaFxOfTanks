package ru.rsreu.ineprokin.model.capability;

import java.util.Optional;

/** Способность объекта производить выстрел, если это позволяет перезарядка. */
@FunctionalInterface
public interface Fireable {

    /**
     * Пытается выстрелить. При успехе сбрасывает перезарядку и возвращает
     * параметры пули, которую должен создать движок; иначе — {@link Optional#empty()}.
     */
    Optional<BulletSpawnRequest> tryFire();
}
