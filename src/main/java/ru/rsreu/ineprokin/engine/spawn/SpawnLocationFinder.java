package ru.rsreu.ineprokin.engine.spawn;

import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Position;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Способность найти свободную точку для возрождения нового вражеского танка. */
@FunctionalInterface
public interface SpawnLocationFinder {

    Optional<Position> findSpawn(GameMap map, List<Tank> tanks, Random random);
}
