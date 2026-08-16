package ru.rsreu.ineprokin.engine;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.engine.ai.AiDecision;
import ru.rsreu.ineprokin.engine.ai.AiStrategy;
import ru.rsreu.ineprokin.engine.spawn.DefaultSpawnLocationFinder;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.PlayerId;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Direction;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWorldTest {

    /**
     * ИИ-стратегия, которая никогда не двигается и не стреляет — потому что
     * {@code AiStrategy} является функциональным интерфейсом, для теста
     * достаточно лямбды, без единого мока или запуска реального ИИ.
     */
    private static final AiStrategy DORMANT_AI =
            (tank, world, random) -> new AiDecision(Direction.UP, false, false);

    private static GameMap mapWithOneEnemy() {
        String content = String.join("\n",
                "########",
                "#P.....#",
                "#......#",
                "#.....E#",
                "########");
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return GameMap.load(input);
    }

    private static GameWorld newWorldWithDormantAi(GameMap map) {
        Random random = new Random(42);
        CollisionService collisionService = new CollisionService();
        EnemyAiService enemyAiService = new EnemyAiService(GameWorldTest.DORMANT_AI, collisionService, random);
        return new GameWorld(map, collisionService, enemyAiService, new DefaultSpawnLocationFinder(), random);
    }

    @Test
    void resetSpawnsPlayerAndEnemiesFromMap() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());

        assertEquals(2, world.getTanks().size());
        assertEquals(GameState.PLAYING, world.getState());
        assertEquals(Tank.MAX_HEALTH, world.getPlayerHealth(PlayerId.PLAYER_ONE));
        assertEquals(1.0, world.getPlayerReloadProgress(PlayerId.PLAYER_ONE));
    }

    @Test
    void drivingForwardIntoWallStopsAtTheWallWithoutTurning() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        double startX = player.getX();
        // Подальше от стены, а не вплотную к ней: одного тика на скорости игрока
        // хватает, чтобы проехать несколько пикселей, — из точки в паре пикселей
        // от стены танк не сдвинулся бы вовсе, упёршись уже на первой попытке.
        // 120 — всё ещё безопасно внутри карты: с учётом размера танка (36)
        // его нижний край (156) не задевает нижнюю стену (начинается с 160).
        player.setPosition(startX, 120);
        double startY = player.getY();

        // Игрок стартует лицом вверх (курс 0°) — едем вперёд, никуда не поворачивая.
        for (int i = 0; i < 200; i++) {
            world.steerPlayer(PlayerId.PLAYER_ONE, 0, 1, 1.0 / 60.0);
        }

        assertEquals(startX, player.getX()); // без поворота вбок не сместился
        assertTrue(player.getY() < startY); // проехал вперёд...
        assertTrue(player.getY() >= 39); // ...и упёрся в верхнюю стену, а не прошёл сквозь неё
    }

    @Test
    void rotatingPlayerChangesHeadingWithoutMoving() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        double startX = player.getX();
        double startY = player.getY();

        world.steerPlayer(PlayerId.PLAYER_ONE, 1, 0, 1.0);

        assertEquals(Tank.ROTATION_SPEED_DEG_PER_SEC, player.getHeadingDegrees());
        assertEquals(startX, player.getX());
        assertEquals(startY, player.getY());
    }

    @Test
    void steeringUnknownPlayerIsNoOp() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());

        world.steerPlayer(PlayerId.PLAYER_TWO, 1, 1, 1.0); // не должно бросить исключение

        assertEquals(Tank.MAX_HEALTH, world.getPlayerMaxHealth(PlayerId.PLAYER_TWO));
        assertEquals(1.0, world.getPlayerReloadProgress(PlayerId.PLAYER_TWO)); // считается "готовым" по умолчанию
    }

    @Test
    void firingPlayerBulletEventuallyKillsStationaryEnemyAndAwardsScore() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        Tank enemy = world.getTanks().stream().filter(tank -> !tank.isPlayer()).findFirst().orElseThrow();

        // Подводим игрока вплотную к врагу по одной оси и разворачиваем в его сторону, чтобы выстрел точно попал.
        player.setPosition(enemy.getX(), enemy.getY() - Tank.SIZE);
        player.faceDirection(Direction.DOWN);

        int shotsNeeded = (Tank.MAX_HEALTH / Bullet.DAMAGE) + 1;
        for (int shot = 0; shot < shotsNeeded; shot++) {
            world.firePlayer(PlayerId.PLAYER_ONE);
            // Прогоняем достаточно тиков, чтобы пуля долетела и перезарядка сбросилась.
            for (int frame = 0; frame < 90; frame++) {
                world.tick(1.0 / 60.0);
            }
        }

        assertTrue(world.getScore() >= GameConfig.SCORE_PER_KILL);
    }

    @Test
    void pauseStopsSimulationUpdates() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        world.togglePause();
        assertEquals(GameState.PAUSED, world.getState());

        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        double startX = player.getX();
        double startY = player.getY();
        world.steerPlayer(PlayerId.PLAYER_ONE, 1, 1, 1.0);

        assertEquals(startX, player.getX());
        assertEquals(startY, player.getY());
        assertFalse(world.getTanks().isEmpty());
    }
}
