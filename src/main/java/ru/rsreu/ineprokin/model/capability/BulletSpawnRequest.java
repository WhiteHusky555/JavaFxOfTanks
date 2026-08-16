package ru.rsreu.ineprokin.model.capability;

import ru.rsreu.ineprokin.model.PlayerId;

/**
 * Запрос на создание пули, возвращаемый {@link Fireable#tryFire()}. Несёт уже
 * готовую точку вылета — у дульного среза, а не из центра танка, — так что
 * этот расчёт не нужно повторять отдельно для игрока и для каждого врага.
 *
 * @param headingDegrees курс пули в градусах по часовой стрелке от направления "вверх"
 * @param shooterId      {@code null}, если пулю выпустил танк под управлением ИИ
 */
public record BulletSpawnRequest(double x, double y, double headingDegrees, PlayerId shooterId) {

    public boolean fromPlayer() {
        return this.shooterId != null;
    }
}
