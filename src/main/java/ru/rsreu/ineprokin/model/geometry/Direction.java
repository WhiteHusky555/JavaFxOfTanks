package ru.rsreu.ineprokin.model.geometry;

/**
 * Одно из четырёх сторон света, которыми думает простой ИИ, выбирая, куда
 * пойти. Игрок в них не заперт — его танк поворачивается на произвольный
 * угол (см. {@code Tank.rotate}), а {@link Direction} лишь даёт врагам
 * готовый вектор смещения и угол, куда развернуться, без {@code switch}
 * на четыре кейса в каждом месте, где это нужно.
 */
public enum Direction {

    UP(0, -1, 0),
    DOWN(0, 1, 180),
    LEFT(-1, 0, 270),
    RIGHT(1, 0, 90);

    private final int dx;
    private final int dy;
    private final double headingDegrees;

    Direction(int dx, int dy, double headingDegrees) {
        this.dx = dx;
        this.dy = dy;
        this.headingDegrees = headingDegrees;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    /** Угол в градусах в той же системе отсчёта, что и {@code Tank.getHeadingDegrees()}: 0° — вверх, по часовой стрелке. */
    public double headingDegrees() {
        return headingDegrees;
    }
}
