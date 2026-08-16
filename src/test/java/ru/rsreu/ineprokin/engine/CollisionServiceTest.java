package ru.rsreu.ineprokin.engine;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.PlayerId;
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

        assertTrue(moved);
        assertEquals(85, mover.getX());
        assertEquals(80 + Tank.SIZE + 5, blocker.getX()); // толкнулся ровно на ту же дельту
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
        Bullet bullet = new Bullet(2, 2, Direction.UP.headingDegrees(), true); // почти в стене (ряд 0)

        List<Tank> killed = collisionService.resolveBulletHits(List.of(bullet), List.of(), map);

        assertTrue(bullet.isDestroyed());
        assertTrue(killed.isEmpty());
    }

    @Test
    void playerBulletDamagesEnemyTankAndReportsKillOnDeath() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Tank enemy = Tank.enemy(80, 80, Direction.DOWN.headingDegrees());
        Bullet bullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), true);

        List<Tank> killed = collisionService.resolveBulletHits(List.of(bullet), List.of(enemy), map);

        assertTrue(bullet.isDestroyed());
        assertEquals(Tank.MAX_HEALTH - Bullet.DAMAGE, enemy.getHealth());
        assertTrue(killed.isEmpty()); // одного попадания недостаточно, чтобы уничтожить танк

        Bullet secondBullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), true);
        Bullet thirdBullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), true);
        Bullet fourthBullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), true);
        collisionService.resolveBulletHits(List.of(secondBullet), List.of(enemy), map);
        collisionService.resolveBulletHits(List.of(thirdBullet), List.of(enemy), map);
        List<Tank> finalKill = collisionService.resolveBulletHits(List.of(fourthBullet), List.of(enemy), map);

        assertTrue(enemy.isDestroyed());
        assertEquals(List.of(enemy), finalKill);
    }

    @Test
    void friendlyFireIsIgnored() {
        CollisionService collisionService = new CollisionService();
        GameMap map = CollisionServiceTest.smallMap();
        Tank playerTank = new Tank(80, 80, Direction.DOWN.headingDegrees(), PlayerId.PLAYER_ONE);
        Bullet playerBullet = new Bullet(80 + Tank.SIZE / 2.0, 80 + Tank.SIZE / 2.0, Direction.UP.headingDegrees(), true);

        collisionService.resolveBulletHits(List.of(playerBullet), List.of(playerTank), map);

        assertEquals(Tank.MAX_HEALTH, playerTank.getHealth());
        assertFalse(playerBullet.isDestroyed());
    }
}
