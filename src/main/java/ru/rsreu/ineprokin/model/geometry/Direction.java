package ru.rsreu.ineprokin.model.geometry;

/**
 * Одно из четырёх направлений движения танка и полёта пули.
 * <p>
 * Направление само знает свой единичный вектор смещения, поэтому движковому
 * коду (движение, стрельба, ИИ) не приходится писать {@code switch} по всем
 * четырём кейсам в каждом месте, где нужно сместить объект.
 */
public enum Direction {

    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }
}
