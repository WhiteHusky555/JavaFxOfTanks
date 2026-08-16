package ru.rsreu.ineprokin.model.capability;

/** Способность объекта продвигать своё состояние во времени. */
@FunctionalInterface
public interface Updatable {

    /**
     * @param deltaTimeSeconds время, прошедшее с предыдущего тика, в секундах
     */
    void update(double deltaTimeSeconds);
}
