package ru.rsreu.ineprokin.viewmodel.dto;

import ru.rsreu.ineprokin.model.entity.PickupType;

/** Неизменяемый снимок одного бонуса на поле для отрисовки. */
public record PickupView(double x, double y, PickupType type) {
}
