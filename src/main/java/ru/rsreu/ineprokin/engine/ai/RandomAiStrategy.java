package ru.rsreu.ineprokin.engine.ai;

import ru.rsreu.ineprokin.engine.GameConfig;
import ru.rsreu.ineprokin.engine.GameWorldView;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.util.List;
import java.util.Random;

/**
 * Базовая тактика ИИ: в основном случайное блуждание и случайная стрельба,
 * но с небольшим шансом сместиться в сторону игрока — иначе противники
 * бродят по карте абсолютно бесцельно, как в исходной C++-версии.
 */
public final class RandomAiStrategy implements AiStrategy {

    @Override
    public AiDecision decide(Tank tank, GameWorldView world, Random random) {
        boolean moving = random.nextDouble() < GameConfig.AI_MOVE_CHANCE;
        Direction direction = this.chooseDirection(tank, world, random);
        boolean wantsToFire = tank.canFire() && random.nextDouble() < GameConfig.AI_FIRE_CHANCE;
        return new AiDecision(direction, moving, wantsToFire);
    }

    private Direction chooseDirection(Tank tank, GameWorldView world, Random random) {
        Tank player = this.findAlivePlayer(world.getTanks());
        if (player != null && random.nextDouble() < GameConfig.AI_CHASE_BIAS) {
            return this.directionTowards(tank, player);
        }
        Direction[] directions = Direction.values();
        return directions[random.nextInt(directions.length)];
    }

    private Tank findAlivePlayer(List<Tank> tanks) {
        for (Tank candidate : tanks) {
            if (candidate.isPlayer() && !candidate.isDestroyed()) {
                return candidate;
            }
        }
        return null;
    }

    private Direction directionTowards(Tank from, Tank to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy > 0 ? Direction.DOWN : Direction.UP;
    }
}
