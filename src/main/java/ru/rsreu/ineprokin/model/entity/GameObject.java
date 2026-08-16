package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.capability.Destructible;
import ru.rsreu.ineprokin.model.capability.Updatable;

/**
 * Базовый класс любой сущности на игровом поле — хранит позицию в пиксельных
 * координатах. Способности, общие для {@link Tank} и {@link Bullet}, выражены
 * функциональными интерфейсами {@link Updatable} и {@link Destructible}, а не
 * одноимёнными абстрактными методами "просто потому что" — движок может
 * опираться на эти интерфейсы, вообще не зная о существовании {@code GameObject}.
 */
public abstract class GameObject implements Updatable, Destructible {

    private double x;
    private double y;

    protected GameObject(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public final double getX() {
        return x;
    }

    public final double getY() {
        return y;
    }

    public final void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }
}
