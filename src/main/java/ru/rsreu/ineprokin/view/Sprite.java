package ru.rsreu.ineprokin.view;

import javafx.scene.canvas.GraphicsContext;

/**
 * Единица отрисовки одного визуального элемента карты — бонуса, бочки,
 * взрыва. Реализация сама решает, чем элемент рисуется на самом деле:
 * готовой растровой текстурой ({@link TextureSprite}) или последовательностью
 * примитивов {@link GraphicsContext}, оформленной лямбдой прямо в месте
 * использования. {@link GameRenderer} об этом не знает и работает с любой
 * реализацией одинаково — так же, как остальные "способности" в проекте
 * выражены функциональными интерфейсами, а не иерархией классов с
 * {@code instanceof}.
 */
@FunctionalInterface
public interface Sprite {

    /**
     * @param x       левый верхний угол квадратной области отрисовки
     * @param y       левый верхний угол квадратной области отрисовки
     * @param size    сторона квадратной области отрисовки
     * @param opacity непрозрачность элемента: {@code 1.0} — сплошной, {@code 0.0} — невидим
     */
    void draw(GraphicsContext gc, double x, double y, double size, double opacity);
}
