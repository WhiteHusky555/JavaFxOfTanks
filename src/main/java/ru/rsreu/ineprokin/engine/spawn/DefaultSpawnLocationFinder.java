package ru.rsreu.ineprokin.engine.spawn;

import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Position;
import ru.rsreu.ineprokin.model.geometry.TileCoord;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Ищет свободную точку возрождения среди заранее заданных на карте стартов
 * врагов ({@code E} в {@code map.txt}), пропуская точки, занятые сейчас
 * другим танком.
 */
public final class DefaultSpawnLocationFinder implements SpawnLocationFinder {

    @Override
    public Optional<Position> findSpawn(GameMap map, List<Tank> tanks, Random random) {
        List<TileCoord> candidates = new ArrayList<>(map.enemyStarts());
        Collections.shuffle(candidates, random);

        for (TileCoord candidate : candidates) {
            Position position = candidate.toPixelCenter(GameMap.TILE_SIZE, Tank.SIZE);
            if (!this.isOccupied(position, tanks)) {
                return Optional.of(position);
            }
        }
        return Optional.empty();
    }

    private boolean isOccupied(Position position, List<Tank> tanks) {
        for (Tank tank : tanks) {
            if (tank.isDestroyed()) {
                continue;
            }
            boolean overlapsX = position.x() + Tank.SIZE > tank.getX() && position.x() < tank.getX() + Tank.SIZE;
            boolean overlapsY = position.y() + Tank.SIZE > tank.getY() && position.y() < tank.getY() + Tank.SIZE;
            if (overlapsX && overlapsY) {
                return true;
            }
        }
        return false;
    }
}
