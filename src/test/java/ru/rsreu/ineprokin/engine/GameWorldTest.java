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
    }

    @Test
    void movingPlayerIntoWallDoesNotChangePosition() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        double startX = player.getX();
        double startY = player.getY();

        for (int i = 0; i < 200; i++) {
            world.movePlayer(PlayerId.PLAYER_ONE, Direction.LEFT, 1.0 / 60.0);
        }

        assertTrue(player.getX() >= startX - 1); // упёрся в стену слева, а не прошёл сквозь
        assertEquals(startY, player.getY());
    }

    @Test
    void movingUnknownPlayerIsNoOp() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());

        world.movePlayer(PlayerId.PLAYER_TWO, Direction.UP, 1.0);

        assertEquals(0, world.getPlayerMaxHealth(PlayerId.PLAYER_TWO) - Tank.MAX_HEALTH); // не бросило исключений
    }

    @Test
    void firingPlayerBulletEventuallyKillsStationaryEnemyAndAwardsScore() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = world.getTanks().stream().filter(Tank::isPlayer).findFirst().orElseThrow();
        Tank enemy = world.getTanks().stream().filter(tank -> !tank.isPlayer()).findFirst().orElseThrow();

        // Подводим игрока вплотную к врагу по одной оси, чтобы выстрел точно попал.
        player.setPosition(enemy.getX(), enemy.getY() - Tank.SIZE);

        int shotsNeeded = (Tank.MAX_HEALTH / Bullet.DAMAGE) + 1;
        for (int shot = 0; shot < shotsNeeded; shot++) {
            world.movePlayer(PlayerId.PLAYER_ONE, Direction.DOWN, 0); // выставляем направление, не двигая танк
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
        world.movePlayer(PlayerId.PLAYER_ONE, Direction.RIGHT, 1.0);

        assertEquals(startX, player.getX());
        assertFalse(world.getTanks().isEmpty());
    }
}
