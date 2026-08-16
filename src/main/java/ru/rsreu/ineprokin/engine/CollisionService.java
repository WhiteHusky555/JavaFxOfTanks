package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.Bullet;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Вся геометрия столкновений в одном месте: танк со стеной, пуля со стеной,
 * пуля с танком, танк с танком.
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
            boolean overlapsX = x + Tank.SIZE > other.getX() && x < other.getX() + Tank.SIZE;
            boolean overlapsY = y + Tank.SIZE > other.getY() && y < other.getY() + Tank.SIZE;
            if (overlapsX && overlapsY) {
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
                if (this.bulletHitsTank(bullet, tank)) {
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

    private boolean bulletHitsTank(Bullet bullet, Tank tank) {
        boolean overlapsX = bullet.getX() + Bullet.RADIUS > tank.getX() && bullet.getX() - Bullet.RADIUS < tank.getX() + Tank.SIZE;
        boolean overlapsY = bullet.getY() + Bullet.RADIUS > tank.getY() && bullet.getY() - Bullet.RADIUS < tank.getY() + Tank.SIZE;
        return overlapsX && overlapsY;
    }
}
