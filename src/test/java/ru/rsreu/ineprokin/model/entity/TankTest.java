package ru.rsreu.ineprokin.model.entity;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankTest {

    @Test
    void newTankCanFireImmediately() {
        Tank tank = new Tank(0, 0, Direction.UP, PlayerId.PLAYER_ONE);

        assertTrue(tank.canFire());
    }

    @Test
    void firingResetsReloadUntilEnoughTimePasses() {
        Tank tank = new Tank(0, 0, Direction.UP, PlayerId.PLAYER_ONE);

        Optional<BulletSpawnRequest> firstShot = tank.tryFire();
        assertTrue(firstShot.isPresent());
        assertFalse(tank.canFire());

        tank.update(Tank.PLAYER_RELOAD_SECONDS - 0.01);
        assertFalse(tank.canFire());

        tank.update(0.02);
        assertTrue(tank.canFire());
    }

    @Test
    void cannotFireTwiceBeforeReload() {
        Tank tank = new Tank(0, 0, Direction.UP, PlayerId.PLAYER_ONE);

        assertTrue(tank.tryFire().isPresent());
        assertTrue(tank.tryFire().isEmpty());
    }

    @Test
    void bulletSpawnsAheadOfTurretInFacingDirection() {
        Tank tank = new Tank(100, 100, Direction.RIGHT, PlayerId.PLAYER_ONE);

        BulletSpawnRequest request = tank.tryFire().orElseThrow();

        assertEquals(Direction.RIGHT, request.direction());
        assertTrue(request.x() > 100 + Tank.SIZE / 2.0);
        assertTrue(request.fromPlayer());
    }

    @Test
    void takingDamageReducesHealthAndClampsAtZero() {
        Tank tank = Tank.enemy(0, 0, Direction.DOWN);

        tank.takeDamage(90);
        assertEquals(10, tank.getHealth());
        assertFalse(tank.isDestroyed());

        tank.takeDamage(50);
        assertEquals(0, tank.getHealth());
        assertTrue(tank.isDestroyed());
    }

    @Test
    void aiTankHasNoPlayerId() {
        Tank tank = Tank.enemy(0, 0, Direction.LEFT);

        assertFalse(tank.isPlayer());
        assertTrue(tank.getPlayerId().isEmpty());
    }
}
