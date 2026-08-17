package ru.rsreu.ineprokin.engine;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.ExplosiveBarrel;
import ru.rsreu.ineprokin.model.entity.Pickup;
import ru.rsreu.ineprokin.model.entity.PickupType;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Direction;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionServiceTest {

    private static GameMap smallMap() {
        String content = String.join("\n",
                "######",
                "#P...#",
                "#....#",
                "#....#",
                "######");
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return GameMap.load(input);
    }

    /** Пошире, с длинным открытым коридором — удобно расставлять танки друг за другом для проверки толкания. */
    private static GameMap corridorMap() {
        String content = String.join("\n",
                "########",
                "#P.....#",
                "#......#",
                "########");
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return GameMap.load(input);
    }

    @Test
    void detectsWallAndOutOfBoundsCollision() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();

        assertTrue(collisionService.collidesWithWall(map, -1, 40, Tank.SIZE));
        assertTrue(collisionService.collidesWithWall(map, 0, 0, Tank.SIZE)); // верхняя стена (ряд 0)
        assertFalse(collisionService.collidesWithWall(map, 40, 40, Tank.SIZE));
    }

    @Test
    void tankCannotMoveThroughWall() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Tank tank = new Tank(40, 40, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        boolean moved = collisionService.tryMoveTank(tank, 40, 0, map, List.of(tank));

        assertFalse(moved);
        assertEquals(40, tank.getX());
        assertEquals(40, tank.getY());
    }

    @Test
    void movingTankPushesBlockerThatHasRoomToGive() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.corridorMap();
        Tank mover = new Tank(80, 40, Direction.RIGHT.headingDegrees(), PlayerId.PLAYER_ONE);
        Tank blocker = Tank.enemy(80 + Tank.SIZE, 40, Direction.LEFT.headingDegrees());
        List<Tank> tanks = List.of(mover, blocker);

        boolean moved = collisionService.tryMoveTank(mover, 85, 40, map, tanks);
        double expectedPush = (85 - 80) * GameConfig.PUSH_TRANSFER_FACTOR; // толчок гасится сопротивлением

        assertTrue(moved);
        assertEquals(80 + expectedPush, mover.getX(), 1e-9); // сам толкающий тоже продвинулся лишь на эту долю
        assertEquals(80 + Tank.SIZE + expectedPush, blocker.getX(), 1e-9);
    }

    @Test
    void movingTankCannotPushBlockerIntoAWall() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.corridorMap();
        double wallX = map.widthInPixels() - GameMap.TILE_SIZE;
        double blockerX = wallX - Tank.SIZE; // блокер уже вплотную к правой стене
        double moverX = blockerX - Tank.SIZE; // толкающий вплотную к блокеру
        Tank mover = new Tank(moverX, 40, Direction.RIGHT.headingDegrees(), PlayerId.PLAYER_ONE);
        Tank blocker = Tank.enemy(blockerX, 40, Direction.LEFT.headingDegrees());
        List<Tank> tanks = List.of(mover, blocker);

        boolean moved = collisionService.tryMoveTank(mover, moverX + 5, 40, map, tanks);

        assertFalse(moved);
        assertEquals(moverX, mover.getX()); // толкающий тоже остаётся на месте
        assertEquals(blockerX, blocker.getX());
    }

    @Test
    void bulletHittingWallIsDestroyedWithoutHurtingAnyone() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Bullet bullet = new Bullet(2, 2, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE); // почти в стене (ряд 0)

        List<PlayerId> scorers = collisionService.resolveBulletHits(List.of(bullet), List.of(), map);

        assertTrue(bullet.isDestroyed());
        assertTrue(scorers.isEmpty());
    }

    @Test
    void playerBulletDamagesEnemyTankAndReportsScorerOnDeath() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Tank enemy = Tank.enemy(80, 80, Direction.DOWN.headingDegrees());
        Bullet bullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), PlayerId.PLAYER_TWO);

        List<PlayerId> scorers = collisionService.resolveBulletHits(List.of(bullet), List.of(enemy), map);

        assertTrue(bullet.isDestroyed());
        assertEquals(Tank.MAX_HEALTH - Bullet.DAMAGE, enemy.getHealth());
        assertTrue(scorers.isEmpty()); // одного попадания недостаточно, чтобы уничтожить танк

        for (int i = 0; i < 2; i++) {
            Bullet nextBullet = new Bullet(
                    80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), PlayerId.PLAYER_TWO);
            collisionService.resolveBulletHits(List.of(nextBullet), List.of(enemy), map);
        }
        Bullet killingBlow = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), PlayerId.PLAYER_TWO);
        List<PlayerId> finalScorers = collisionService.resolveBulletHits(List.of(killingBlow), List.of(enemy), map);

        assertTrue(enemy.isDestroyed());
        // Именно тот игрок, чья пуля добила танк, получает очко — не любой игрок вообще.
        assertEquals(List.of(PlayerId.PLAYER_TWO), finalScorers);
    }

    @Test
    void friendlyFireIsIgnoredRegardlessOfWhichPlayerShot() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Tank playerOneTank = new Tank(80, 80, Direction.DOWN.headingDegrees(), PlayerId.PLAYER_ONE);
        Bullet playerTwoBullet = new Bullet(
                80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), PlayerId.PLAYER_TWO);

        collisionService.resolveBulletHits(List.of(playerTwoBullet), List.of(playerOneTank), map);

        assertEquals(Tank.MAX_HEALTH, playerOneTank.getHealth());
        assertFalse(playerTwoBullet.isDestroyed());
    }

    @Test
    void bulletDestroysBarrelAndBlastHitsEveryoneInRadiusIncludingTheShooter() {
        CollisionService collisionService = new CollisionService();
        ExplosiveBarrel barrel = new ExplosiveBarrel(80, 80);
        Tank shooter = new Tank(80, 80, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE); // сам в радиусе взрыва
        Tank bystander = Tank.enemy(80 + 20, 80, Direction.UP.headingDegrees());
        Bullet bullet = new Bullet(80 + ExplosiveBarrel.SIZE / 2.0, 80 + ExplosiveBarrel.SIZE / 2.0,
                Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        CollisionService.BarrelBlastResult result = collisionService.resolveBulletBarrelHits(
                List.of(bullet), List.of(barrel), List.of(shooter, bystander));

        assertTrue(bullet.isDestroyed());
        assertTrue(barrel.isDestroyed());
        assertEquals(Tank.MAX_HEALTH - ExplosiveBarrel.EXPLOSION_DAMAGE, shooter.getHealth()); // взрыв не разбирает своих
        assertEquals(Tank.MAX_HEALTH - ExplosiveBarrel.EXPLOSION_DAMAGE, bystander.getHealth());
        assertTrue(result.scorers().isEmpty()); // одного взрыва недостаточно, чтобы убить
        assertEquals(1, result.detonations().size());
        assertEquals(ExplosiveBarrel.EXPLOSION_RADIUS, result.detonations().get(0).radius(), 1e-9);
    }

    @Test
    void barrelExplosionCreditsTheShooterWhenItKillsAnEnemy() {
        CollisionService collisionService = new CollisionService();
        ExplosiveBarrel barrel = new ExplosiveBarrel(80, 80);
        Tank enemy = Tank.enemy(80, 80, Direction.UP.headingDegrees());
        enemy.takeDamage(Tank.MAX_HEALTH - ExplosiveBarrel.EXPLOSION_DAMAGE + 1); // взрыв как раз добьёт
        Bullet bullet = new Bullet(80 + ExplosiveBarrel.SIZE / 2.0, 80 + ExplosiveBarrel.SIZE / 2.0,
                Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        CollisionService.BarrelBlastResult result =
                collisionService.resolveBulletBarrelHits(List.of(bullet), List.of(barrel), List.of(enemy));

        assertTrue(enemy.isDestroyed());
        assertEquals(List.of(PlayerId.PLAYER_ONE), result.scorers());
    }

    @Test
    void tanksOutsideBlastRadiusAreUnaffectedByExplosion() {
        CollisionService collisionService = new CollisionService();
        ExplosiveBarrel barrel = new ExplosiveBarrel(80, 80);
        Tank farAway = Tank.enemy(80 + ExplosiveBarrel.EXPLOSION_RADIUS * 3, 80, Direction.UP.headingDegrees());
        Bullet bullet = new Bullet(80 + ExplosiveBarrel.SIZE / 2.0, 80 + ExplosiveBarrel.SIZE / 2.0,
                Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        collisionService.resolveBulletBarrelHits(List.of(bullet), List.of(barrel), List.of(farAway));

        assertEquals(Tank.MAX_HEALTH, farAway.getHealth());
    }

    @Test
    void playerTankCollectsPickupOnOverlap() {
        CollisionService collisionService = new CollisionService();
        Pickup medkit = new Pickup(80, 80, PickupType.MEDKIT);
        Tank player = new Tank(80, 80, Direction.UP.headingDegrees(), PlayerId.PLAYER_ONE);

        List<CollisionService.PickupCollection> collections =
                collisionService.resolvePickupCollisions(List.of(medkit), List.of(player));

        assertTrue(medkit.isDestroyed()); // "уничтожен" здесь означает "подобран"
        assertEquals(1, collections.size());
        assertEquals(PickupType.MEDKIT, collections.get(0).type());
        assertEquals(player, collections.get(0).tank());
    }

    @Test
    void enemyTanksCannotCollectPickups() {
        CollisionService collisionService = new CollisionService();
        Pickup medkit = new Pickup(80, 80, PickupType.MEDKIT);
        Tank enemy = Tank.enemy(80, 80, Direction.UP.headingDegrees());

        List<CollisionService.PickupCollection> collections =
                collisionService.resolvePickupCollisions(List.of(medkit), List.of(enemy));

        assertFalse(medkit.isDestroyed());
        assertTrue(collections.isEmpty());
    }
}
