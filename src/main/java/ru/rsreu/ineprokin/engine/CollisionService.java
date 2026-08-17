package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.ExplosiveBarrel;
import ru.rsreu.ineprokin.model.entity.Pickup;
import ru.rsreu.ineprokin.model.entity.PickupType;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Вся геометрия столкновений в одном месте: танк со стеной, пуля со стеной,
 * пуля с танком, танк с танком, пуля с бочкой, взрыв бочки с танками,
 * танк с бонусом.
 */
public final class CollisionService {

    public boolean collidesWithWall(GameMap map, double x, double y, double size) {
        if (x < 0 || y < 0 || x + size > map.widthInPixels() || y + size > map.heightInPixels()) {
            return true;
        }

        int startCol = (int) (x / GameMap.TILE_SIZE);
        int startRow = (int) (y / GameMap.TILE_SIZE);
        int endCol = (int) ((x + size - 0.001) / GameMap.TILE_SIZE);
        int endRow = (int) ((y + size - 0.001) / GameMap.TILE_SIZE);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (map.isWall(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean collidesWithOtherTank(Tank moving, double x, double y, List<Tank> tanks) {
        return this.findBlockingTank(moving, x, y, tanks) != null;
    }

    private Tank findBlockingTank(Tank moving, double x, double y, List<Tank> tanks) {
        for (Tank other : tanks) {
            if (other == moving || other.isDestroyed()) {
                continue;
            }
            if (this.boxesOverlap(x, y, Tank.SIZE, other.getX(), other.getY(), Tank.SIZE)) {
                return other;
            }
        }
        return null;
    }

    /**
     * Пытается передвинуть танк в новую точку. Стену объехать нельзя, а вот
     * другой танк, оказавшийся на пути, можно толкнуть — если только у него
     * самого есть куда сдвинуться. Толкается только один танк за раз: если он
     * упирается в стену или в третий танк, толчок не проходит и мы сами тоже
     * остаёмся на месте.
     * <p>
     * Толчок не бесплатный: и толкающий, и толкаемый продвигаются не на всю
     * попытку хода, а только на {@link GameConfig#PUSH_TRANSFER_FACTOR} от
     * неё — иначе танк противника или игрока сдвигался бы с места так же
     * легко, как пустая клетка, за один кадр.
     */
    public boolean tryMoveTank(Tank tank, double newX, double newY, GameMap map, List<Tank> tanks) {
        if (this.collidesWithWall(map, newX, newY, Tank.SIZE)) {
            return false;
        }
        Tank blocker = this.findBlockingTank(tank, newX, newY, tanks);
        if (blocker == null) {
            tank.setPosition(newX, newY);
            return true;
        }

        double pushDx = (newX - tank.getX()) * GameConfig.PUSH_TRANSFER_FACTOR;
        double pushDy = (newY - tank.getY()) * GameConfig.PUSH_TRANSFER_FACTOR;
        if (!this.tryPush(blocker, pushDx, pushDy, map, tanks)) {
            return false;
        }
        tank.setPosition(tank.getX() + pushDx, tank.getY() + pushDy);
        return true;
    }

    /** Сдвигает {@code tank} на вектор {@code (dx, dy)}, если это не заводит его в стену или в третий танк. */
    private boolean tryPush(Tank tank, double dx, double dy, GameMap map, List<Tank> tanks) {
        double newX = tank.getX() + dx;
        double newY = tank.getY() + dy;
        if (this.collidesWithWall(map, newX, newY, Tank.SIZE) || this.findBlockingTank(tank, newX, newY, tanks) != null) {
            return false;
        }
        tank.setPosition(newX, newY);
        return true;
    }

    /** Мягко разводит перекрывающиеся танки в стороны, не пропуская их сквозь стены. */
    public void separateOverlappingTanks(List<Tank> tanks, GameMap map) {
        for (int i = 0; i < tanks.size(); i++) {
            Tank first = tanks.get(i);
            if (first.isDestroyed()) {
                continue;
            }
            for (int j = i + 1; j < tanks.size(); j++) {
                Tank second = tanks.get(j);
                if (second.isDestroyed()) {
                    continue;
                }
                this.separatePair(first, second, map, tanks);
            }
        }
    }

    private void separatePair(Tank first, Tank second, GameMap map, List<Tank> tanks) {
        double centerFirstX = first.getX() + Tank.SIZE / 2.0;
        double centerFirstY = first.getY() + Tank.SIZE / 2.0;
        double centerSecondX = second.getX() + Tank.SIZE / 2.0;
        double centerSecondY = second.getY() + Tank.SIZE / 2.0;

        double dx = centerFirstX - centerSecondX;
        double dy = centerFirstY - centerSecondY;
        double distance = Math.hypot(dx, dy);

        if (distance >= Tank.SIZE) {
            return; // не перекрываются
        }
        if (distance < 0.001) {
            // Танки оказались точно друг на друге — расталкиваем по оси X произвольно.
            this.tryMoveTank(first, first.getX() + Tank.SIZE / 4.0, first.getY(), map, tanks);
            this.tryMoveTank(second, second.getX() - Tank.SIZE / 4.0, second.getY(), map, tanks);
            return;
        }

        double overlap = Tank.SIZE - distance;
        double pushX = (dx / distance) * overlap / 2.0;
        double pushY = (dy / distance) * overlap / 2.0;

        this.tryMoveTank(first, first.getX() + pushX, first.getY() + pushY, map, tanks);
        this.tryMoveTank(second, second.getX() - pushX, second.getY() - pushY, map, tanks);
    }

    /**
     * Обрабатывает столкновения пуль со стенами и танками.
     *
     * @return {@link PlayerId} стрелка за каждый вражеский танк, уничтоженный
     *         в этом тике, — по этому списку {@code GameWorld} начислит очки
     *         тому игроку, который выстрелил, и закажет возрождение замены
     */
    public List<PlayerId> resolveBulletHits(List<Bullet> bullets, List<Tank> tanks, GameMap map) {
        List<PlayerId> scorers = new ArrayList<>();

        for (Bullet bullet : bullets) {
            if (bullet.isDestroyed()) {
                continue;
            }
            int col = (int) Math.floor(bullet.getX() / GameMap.TILE_SIZE);
            int row = (int) Math.floor(bullet.getY() / GameMap.TILE_SIZE);
            if (map.isWall(col, row)) {
                bullet.destroy();
                continue;
            }

            for (Tank tank : tanks) {
                if (tank.isDestroyed() || tank.isPlayer() == bullet.isFromPlayer()) {
                    continue; // без дружественного огня
                }
                if (this.bulletHitsBox(bullet, tank.getX(), tank.getY(), Tank.SIZE)) {
                    tank.takeDamage(bullet.getDamage());
                    bullet.destroy();
                    if (tank.isDestroyed()) {
                        bullet.getShooterId().ifPresent(scorers::add);
                    }
                    break;
                }
            }
        }
        return scorers;
    }

    /**
     * Геометрия одной детонации бочки — центр и радиус поражения. Не несёт
     * ничего игрового, только то, что нужно, чтобы {@code engine.GameWorld}
     * завёл на этом месте кратковременный визуальный эффект взрыва.
     */
    public record BarrelDetonation(double x, double y, double radius) {
    }

    /** Результат обработки попаданий по бочкам: кто получил очко и где именно прогремел взрыв. */
    public record BarrelBlastResult(List<PlayerId> scorers, List<BarrelDetonation> detonations) {
    }

    /**
     * Обрабатывает попадания пуль по бочкам: разносит бочку от любого попадания
     * и задевает взрывом всех танков в радиусе — не разбирая, свои они или
     * чужие, даже того, кто стрелял.
     *
     * @return очки за танков, убитых взрывом (тому, чья пуля подорвала бочку),
     *         и геометрию каждой детонации этого тика
     */
    public BarrelBlastResult resolveBulletBarrelHits(List<Bullet> bullets, List<ExplosiveBarrel> barrels, List<Tank> tanks) {
        List<PlayerId> scorers = new ArrayList<>();
        List<BarrelDetonation> detonations = new ArrayList<>();

        for (Bullet bullet : bullets) {
            if (bullet.isDestroyed()) {
                continue;
            }
            for (ExplosiveBarrel barrel : barrels) {
                if (barrel.isDestroyed()) {
                    continue;
                }
                if (this.bulletHitsBox(bullet, barrel.getX(), barrel.getY(), ExplosiveBarrel.SIZE)) {
                    bullet.destroy();
                    barrel.takeDamage(1);
                    detonations.add(new BarrelDetonation(
                            barrel.getX() + ExplosiveBarrel.SIZE / 2.0,
                            barrel.getY() + ExplosiveBarrel.SIZE / 2.0,
                            ExplosiveBarrel.EXPLOSION_RADIUS));
                    this.applyExplosionDamage(barrel, bullet, tanks, scorers);
                    break;
                }
            }
        }
        return new BarrelBlastResult(scorers, detonations);
    }

    private void applyExplosionDamage(ExplosiveBarrel barrel, Bullet triggeringBullet, List<Tank> tanks, List<PlayerId> scorers) {
        double centerX = barrel.getX() + ExplosiveBarrel.SIZE / 2.0;
        double centerY = barrel.getY() + ExplosiveBarrel.SIZE / 2.0;

        for (Tank tank : tanks) {
            if (tank.isDestroyed()) {
                continue;
            }
            double tankCenterX = tank.getX() + Tank.SIZE / 2.0;
            double tankCenterY = tank.getY() + Tank.SIZE / 2.0;
            if (Math.hypot(tankCenterX - centerX, tankCenterY - centerY) > ExplosiveBarrel.EXPLOSION_RADIUS) {
                continue;
            }
            tank.takeDamage(ExplosiveBarrel.EXPLOSION_DAMAGE);
            if (triggeringBullet.isFromPlayer() && !tank.isPlayer() && tank.isDestroyed()) {
                triggeringBullet.getShooterId().ifPresent(scorers::add);
            }
        }
    }

    /**
     * Собирает бонусы, до которых доехал танк игрока — ИИ-танки бонусы не
     * подбирают. Сам эффект бонуса эта запись не несёт, только кто и что
     * подобрал; применяет его {@code GameWorld}, зная про очки и жизни.
     */
    public record PickupCollection(Tank tank, PickupType type) {
    }

    public List<PickupCollection> resolvePickupCollisions(List<Pickup> pickups, List<Tank> tanks) {
        List<PickupCollection> collections = new ArrayList<>();

        for (Pickup pickup : pickups) {
            if (pickup.isDestroyed()) {
                continue;
            }
            for (Tank tank : tanks) {
                if (tank.isDestroyed() || !tank.isPlayer()) {
                    continue;
                }
                if (this.boxesOverlap(tank.getX(), tank.getY(), Tank.SIZE, pickup.getX(), pickup.getY(), Pickup.SIZE)) {
                    pickup.collect();
                    collections.add(new PickupCollection(tank, pickup.getType()));
                    break;
                }
            }
        }
        return collections;
    }

    private boolean bulletHitsBox(Bullet bullet, double boxX, double boxY, double boxSize) {
        boolean overlapsX = bullet.getX() + Bullet.RADIUS > boxX && bullet.getX() - Bullet.RADIUS < boxX + boxSize;
        boolean overlapsY = bullet.getY() + Bullet.RADIUS > boxY && bullet.getY() - Bullet.RADIUS < boxY + boxSize;
        return overlapsX && overlapsY;
    }

    private boolean boxesOverlap(double firstX, double firstY, double firstSize, double secondX, double secondY, double secondSize) {
        boolean overlapsX = firstX + firstSize > secondX && firstX < secondX + secondSize;
        boolean overlapsY = firstY + firstSize > secondY && firstY < secondY + secondSize;
        return overlapsX && overlapsY;
    }
}
