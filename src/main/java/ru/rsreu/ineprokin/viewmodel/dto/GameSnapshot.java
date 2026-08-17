package ru.rsreu.ineprokin.viewmodel.dto;

import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.List;

/**
 * Полностью неизменяемый снимок партии на момент одного тика симуляции.
 * <p>
 * Это единственное, что пересекает границу между потоком симуляции
 * ({@code GameSimulationLoop}, работает в фоне) и потоком JavaFX (рисует
 * {@code Canvas}): поток симуляции публикует новый снимок в
 * {@code AtomicReference}, поток JavaFX читает последний опубликованный —
 * без блокировок, потому что делить между потоками нечего, кроме ссылки
 * на неизменяемый объект. Компактный конструктор дополнительно копирует
 * списки в {@link List#copyOf}, чтобы вызывающий поток не мог случайно
 * продолжить их изменять после публикации снимка.
 *
 * @param playerOne итоги первого игрока для HUD
 * @param playerTwo итоги второго игрока для HUD; {@link PlayerHudInfo#available()} — {@code false},
 *                  если на этой карте для него нет точки старта
 */
public record GameSnapshot(
        GameMap map,
        List<TankView> tanks,
        List<BulletView> bullets,
        List<PickupView> pickups,
        List<BarrelView> barrels,
        List<ExplosionView> explosions,
        PlayerHudInfo playerOne,
        PlayerHudInfo playerTwo,
        GameState state,
        double fps,
        boolean resultsVisible,
        int resultsSecondsLeft
) {

    public GameSnapshot {
        tanks = List.copyOf(tanks);
        bullets = List.copyOf(bullets);
        pickups = List.copyOf(pickups);
        barrels = List.copyOf(barrels);
        explosions = List.copyOf(explosions);
    }
}
