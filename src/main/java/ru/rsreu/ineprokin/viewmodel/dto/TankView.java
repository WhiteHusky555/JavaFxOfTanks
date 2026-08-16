package ru.rsreu.ineprokin.viewmodel.dto;

import ru.rsreu.ineprokin.model.entity.PlayerId;
import ru.rsreu.ineprokin.model.geometry.Direction;

/**
 * Неизменяемый снимок одного танка для отрисовки. Слой представления
 * никогда не держит в руках "живой" {@code model.entity.Tank} — только
 * такие record-снимки, сделанные потоком симуляции на момент последнего тика.
 *
 * @param playerId {@code null}, если танк управляется ИИ
 */
public record TankView(double x, double y, Direction direction, PlayerId playerId, int health, int maxHealth) {

    public boolean isPlayerControlled() {
        return this.playerId != null;
    }
}
