package ru.rsreu.ineprokin.viewmodel.dto;

/** Неизменяемый снимок одной пули для отрисовки. */
public record BulletView(double x, double y, boolean fromPlayer) {
}
