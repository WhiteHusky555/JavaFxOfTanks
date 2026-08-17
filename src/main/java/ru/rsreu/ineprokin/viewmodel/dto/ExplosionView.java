package ru.rsreu.ineprokin.viewmodel.dto;

/**
 * Неизменяемый снимок одного взрыва бочки для отрисовки: центр, радиус
 * поражения и доля прожитого времени эффекта ({@code progress}, от 0 до 1) —
 * по ней рендерер решает, насколько кольцо уже расширилось и выцвело.
 */
public record ExplosionView(double x, double y, double radius, double progress) {
}
