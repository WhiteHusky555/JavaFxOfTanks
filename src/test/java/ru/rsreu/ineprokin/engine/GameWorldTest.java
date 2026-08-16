package ru.rsreu.ineprokin.engine;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.engine.ai.AiDecision;
import ru.rsreu.ineprokin.engine.ai.AiStrategy;
import ru.rsreu.ineprokin.engine.spawn.DefaultSpawnLocationFinder;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.ExplosiveBarrel;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.Pickup;
import ru.rsreu.ineprokin.model.entity.PickupType;
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

    /** У этой карты, в отличие от {@link #mapWithOneEnemy()}, есть точка старта и для второго игрока. */
    private static GameMap mapWithBothPlayers() {
        String content = String.join("\n",
                "########",
                "#P.....#",
                "#......#",
                "#2.....#",
                "########");
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return GameMap.load(input);
    }

    /** Несёт по одному экземпляру каждого бонуса и одну бочку — координаты не важны, тесты находят их через геттеры. */
    private static GameMap mapWithPickupsAndBarrel() {
        String content = String.join("\n",
                "##########",
                "#P.......#",
                "#..M..L..#",
                "#..R..B..#",
                "#........#",
                "##########");
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return GameMap.load(input);
    }

    private static GameWorld newWorldWithDormantAi(GameMap map) {
        Random random = new Random(42);
        CollisionService collisionService = new CollisionService();
        EnemyAiService enemyAiService = new EnemyAiService(GameWorldTest.DORMANT_AI, collisionService, random);
        return new GameWorld(map, collisionService, enemyAiService, new DefaultSpawnLocationFinder(), random);
    }

    private static Tank playerTank(GameWorld world, PlayerId playerId) {
        return world.getTanks().stream()
                .filter(tank -> tank.getPlayerId().orElse(null) == playerId)
                .findFirst()
                .orElseThrow();
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
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
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
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        double startX = player.getX();
        double startY = player.getY();

        world.steerPlayer(PlayerId.PLAYER_ONE, 1, 0, 1.0);

        assertEquals(Tank.ROTATION_SPEED_DEG_PER_SEC, player.getHeadingDegrees());
        assertEquals(startX, player.getX());
        assertEquals(startY, player.getY());
    }

    @Test
    void steeringInactivePlayerIsNoOp() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());

        world.steerPlayer(PlayerId.PLAYER_TWO, 1, 1, 1.0); // не должно бросить исключение

        assertFalse(world.isPlayerAvailable(PlayerId.PLAYER_TWO)); // на этой карте для него нет точки старта
        assertEquals(Tank.MAX_HEALTH, world.getPlayerMaxHealth(PlayerId.PLAYER_TWO));
        assertEquals(1.0, world.getPlayerReloadProgress(PlayerId.PLAYER_TWO)); // считается "готовым" по умолчанию
    }

    @Test
    void firingPlayerBulletEventuallyKillsStationaryEnemyAndCreditsTheShooter() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
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

        assertTrue(world.getScore(PlayerId.PLAYER_ONE) >= GameConfig.SCORE_PER_KILL);
        assertEquals(0, world.getScore(PlayerId.PLAYER_TWO)); // второй игрок не стрелял — и не подключался
    }

    @Test
    void pauseStopsSimulationUpdates() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithOneEnemy());
        world.togglePause();
        assertEquals(GameState.PAUSED, world.getState());

        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        double startX = player.getX();
        double startY = player.getY();
        world.steerPlayer(PlayerId.PLAYER_ONE, 1, 1, 1.0);

        assertEquals(startX, player.getX());
        assertEquals(startY, player.getY());
        assertFalse(world.getTanks().isEmpty());
    }

    @Test
    void playerTwoStaysInactiveUntilExplicitlyActivated() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithBothPlayers());

        assertTrue(world.isPlayerAvailable(PlayerId.PLAYER_TWO));
        assertFalse(world.isPlayerActive(PlayerId.PLAYER_TWO));
        assertEquals(1, world.getTanks().size()); // только первый игрок, второй ещё не сыграл ни одной клавиши
    }

    @Test
    void activatingPlayerTwoSpawnsThemAtTheirMapStart() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithBothPlayers());

        world.activatePlayer(PlayerId.PLAYER_TWO);

        assertTrue(world.isPlayerActive(PlayerId.PLAYER_TWO));
        assertEquals(2, world.getTanks().size());
        assertEquals(Tank.MAX_HEALTH, world.getPlayerHealth(PlayerId.PLAYER_TWO));
    }

    @Test
    void activatingAlreadyActivePlayerDoesNotSpawnASecondTank() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithBothPlayers());
        world.activatePlayer(PlayerId.PLAYER_TWO);

        world.activatePlayer(PlayerId.PLAYER_TWO);

        assertEquals(2, world.getTanks().size());
    }

    @Test
    void roundEndsOnlyOnceBothActivePlayersAreDestroyed() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithBothPlayers());
        world.activatePlayer(PlayerId.PLAYER_TWO);

        GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE).takeDamage(Tank.MAX_HEALTH);
        world.tick(1.0 / 60.0);
        assertEquals(GameState.PLAYING, world.getState()); // второй игрок ещё жив

        GameWorldTest.playerTank(world, PlayerId.PLAYER_TWO).takeDamage(Tank.MAX_HEALTH);
        world.tick(1.0 / 60.0);
        assertEquals(GameState.GAME_OVER, world.getState());
    }

    @Test
    void cannotActivatePlayerAfterTheRoundIsOver() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithBothPlayers());
        GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE).takeDamage(Tank.MAX_HEALTH);
        world.tick(1.0 / 60.0);
        assertEquals(GameState.GAME_OVER, world.getState());

        world.activatePlayer(PlayerId.PLAYER_TWO);

        assertFalse(world.isPlayerActive(PlayerId.PLAYER_TWO));
    }

    @Test
    void collectingMedkitHealsPlayerButNeverAboveMax() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithPickupsAndBarrel());
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        player.takeDamage(70);
        Pickup medkit = world.getPickups().stream()
                .filter(pickup -> pickup.getType() == PickupType.MEDKIT).findFirst().orElseThrow();
        player.setPosition(medkit.getX(), medkit.getY());

        world.tick(1.0 / 60.0);

        assertEquals(Math.min(Tank.MAX_HEALTH, 30 + GameConfig.MEDKIT_HEAL_AMOUNT), player.getHealth());
        assertTrue(world.getPickups().stream().noneMatch(pickup -> pickup.getType() == PickupType.MEDKIT));
    }

    @Test
    void collectingExtraLifeAllowsPlayerToRespawnInsteadOfEndingTheRound() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithPickupsAndBarrel());
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        Pickup extraLife = world.getPickups().stream()
                .filter(pickup -> pickup.getType() == PickupType.EXTRA_LIFE).findFirst().orElseThrow();
        player.setPosition(extraLife.getX(), extraLife.getY());
        world.tick(1.0 / 60.0);
        assertEquals(1, world.getExtraLives(PlayerId.PLAYER_ONE));

        player.takeDamage(Tank.MAX_HEALTH);
        world.tick(1.0 / 60.0);

        assertEquals(GameState.PLAYING, world.getState()); // запасная жизнь потрачена вместо конца раунда
        assertEquals(0, world.getExtraLives(PlayerId.PLAYER_ONE));
        Tank revivedPlayer = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        assertEquals(Tank.MAX_HEALTH, revivedPlayer.getHealth());
        assertTrue(revivedPlayer.isInvulnerable());
    }

    @Test
    void playerCanDetonateBarrelWithGunfireAndTakeSplashDamage() {
        GameWorld world = GameWorldTest.newWorldWithDormantAi(GameWorldTest.mapWithPickupsAndBarrel());
        Tank player = GameWorldTest.playerTank(world, PlayerId.PLAYER_ONE);
        ExplosiveBarrel barrel = world.getBarrels().get(0);

        // Игрок вплотную к бочке, целится в неё и стреляет.
        player.setPosition(barrel.getX(), barrel.getY() - Tank.SIZE);
        player.faceDirection(Direction.DOWN);
        world.firePlayer(PlayerId.PLAYER_ONE);

        for (int frame = 0; frame < 30; frame++) {
            world.tick(1.0 / 60.0);
        }

        assertTrue(world.getBarrels().isEmpty());
        assertTrue(player.getHealth() < Tank.MAX_HEALTH); // задело взрывом
    }
}
