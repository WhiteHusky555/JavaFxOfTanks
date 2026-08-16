package ru.rsreu.ineprokin.viewmodel.dto;

import ru.rsreu.ineprokin.model.entity.PlayerId;

/**
 * Неизменяемый снимок одного танка для отрисовки. Слой представления
 * никогда не держит в руках "живой" {@code model.entity.Tank} — только
 * такие record-снимки, сделанные потоком симуляции на момент последнего тика.
 *
 * @param headingDegrees курс танка в градусах по часовой стрелке от направления "вверх"
 * @param playerId       {@code null}, если танк управляется ИИ
 * @param reloadProgress доля перезарядки орудия: {@code 1.0} — готово стрелять
 */
public record TankView(double x, double y, double headingDegrees, PlayerId playerId,
                        int health, int maxHealth, double reloadProgress) {

    public boolean isPlayerControlled() {
        return this.playerId != null;
    }
}
