package ru.rsreu.ineprokin.viewmodel.dto;

/**
 * Итоги партии для одного игрока, которые показывает HUD.
 *
 * @param available возможен ли этот игрок вообще на данной карте (есть ли для него точка старта)
 * @param active    подключился ли этот игрок к партии
 */
public record PlayerHudInfo(boolean available, boolean active, int score, int health, int maxHealth, double reloadProgress) {

    public static PlayerHudInfo unavailable() {
        return new PlayerHudInfo(false, false, 0, 0, 0, 0.0);
    }
}
