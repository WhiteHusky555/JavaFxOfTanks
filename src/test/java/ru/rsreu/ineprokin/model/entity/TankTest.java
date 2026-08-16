package ru.rsreu.ineprokin.model.entity;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankTest {

    private static final double EPSILON = 1e-9;

    @Test
    void newTankCanFireImmediately() {
        Tank tank = new Tank(0, 0, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        assertTrue(tank.canFire());
        assertEquals(1.0, tank.getReloadProgress(), EPSILON);
    }

    @Test
    void firingResetsReloadUntilEnoughTimePasses() {
        Tank tank = new Tank(0, 0, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        Optional<BulletSpawnRequest> firstShot = tank.tryFire();
        assertTrue(firstShot.isPresent());
        assertFalse(tank.canFire());
        assertEquals(0.0, tank.getReloadProgress(), EPSILON);

        tank.update(Tank.PLAYER_RELOAD_SECONDS - 0.01);
        assertFalse(tank.canFire());
        assertTrue(tank.getReloadProgress() < 1.0);

        tank.update(0.02);
        assertTrue(tank.canFire());
        assertEquals(1.0, tank.getReloadProgress(), EPSILON);
    }

    @Test
    void cannotFireTwiceBeforeReload() {
        Tank tank = new Tank(0, 0, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        assertTrue(tank.tryFire().isPresent());
        assertTrue(tank.tryFire().isEmpty());
    }

    @Test
    void bulletSpawnsAheadOfTurretInFacingDirection() {
        Tank tank = new Tank(100, 100, Direction.RIGHT.headingDegrees(), PlayerId.PLAYER_ONE);

        BulletSpawnRequest request = tank.tryFire().orElseThrow();

        assertEquals(Direction.RIGHT.headingDegrees(), request.headingDegrees(), EPSILON);
        assertTrue(request.x() > 100 + Tank.SIZE / 2.0);
        assertTrue(request.fromPlayer());
        assertEquals(PlayerId.PLAYER_ONE, request.shooterId());
    }

    @Test
    void rotateTurnsTowardsRequestedSideOverTime() {
        Tank tank = new Tank(0, 0, 0, PlayerId.PLAYER_ONE);

        tank.rotate(1, 1.0); // по часовой стрелке в течение секунды

        assertEquals(Tank.ROTATION_SPEED_DEG_PER_SEC, tank.getHeadingDegrees(), EPSILON);
    }

    @Test
    void rotateWrapsAroundFullCircle() {
        Tank tank = new Tank(0, 0, 10, PlayerId.PLAYER_ONE);

        tank.rotate(-1, 1.0); // против часовой стрелки, должен уйти в отрицательные градусы и завернуться

        assertTrue(tank.getHeadingDegrees() >= 0 && tank.getHeadingDegrees() < 360);
    }

    @Test
    void rotateTowardsTurnsGraduallyRatherThanSnapping() {
        Tank tank = new Tank(0, 0, 0, PlayerId.PLAYER_ONE);

        tank.rotateTowards(Direction.RIGHT.headingDegrees(), 0.1); // короткий шаг, далеко от 90°

        double expected = Tank.ROTATION_SPEED_DEG_PER_SEC * 0.1;
        assertEquals(expected, tank.getHeadingDegrees(), EPSILON);
        assertTrue(tank.getHeadingDegrees() < Direction.RIGHT.headingDegrees());
    }

    @Test
    void rotateTowardsSnapsExactlyOnceCloseEnough() {
        Tank tank = new Tank(0, 0, 85, PlayerId.PLAYER_ONE);

        tank.rotateTowards(Direction.RIGHT.headingDegrees(), 1.0); // целой секунды с избытком хватает на 5°

        assertEquals(Direction.RIGHT.headingDegrees(), tank.getHeadingDegrees(), EPSILON);
    }

    @Test
    void rotateTowardsTakesShortestPathAcrossZero() {
        Tank tank = new Tank(0, 0, 350, PlayerId.PLAYER_ONE);

        // Кратчайший путь от 350° к 10° — через 0° (+20°), а не в обход через 180° (-340°).
        tank.rotateTowards(10, 1.0);

        assertEquals(10.0, tank.getHeadingDegrees(), EPSILON);
    }

    @Test
    void faceDirectionSnapsInstantlyToCardinalHeading() {
        Tank tank = new Tank(0, 0, 45, PlayerId.PLAYER_ONE);

        tank.faceDirection(Direction.LEFT);

        assertEquals(Direction.LEFT.headingDegrees(), tank.getHeadingDegrees(), EPSILON);
    }

    @Test
    void takingDamageReducesHealthAndClampsAtZero() {
        Tank tank = Tank.enemy(0, 0, Direction.DOWN.headingDegrees());

        tank.takeDamage(90);
        assertEquals(10, tank.getHealth());
        assertFalse(tank.isDestroyed());

        tank.takeDamage(50);
        assertEquals(0, tank.getHealth());
        assertTrue(tank.isDestroyed());
    }

    @Test
    void aiTankHasNoPlayerId() {
        Tank tank = Tank.enemy(0, 0, Direction.LEFT.headingDegrees());

        assertFalse(tank.isPlayer());
        assertTrue(tank.getPlayerId().isEmpty());
    }
}
