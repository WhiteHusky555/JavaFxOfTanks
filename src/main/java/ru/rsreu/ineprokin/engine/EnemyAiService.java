package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.engine.ai.AiDecision;
import ru.rsreu.ineprokin.engine.ai.AiStrategy;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.entity.Tank;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Хранит "память" каждого вражеского танка (таймеры до следующего решения)
 * и на каждом тике решает, куда ему двигаться и не пора ли стрелять, делегируя
 * сам выбор направления/выстрела внедрённой {@link AiStrategy}.
 * <p>
 * У каждого танка — собственный таймер в секундах до следующего решения,
 * независимый от частоты кадров: сколько бы раз в секунду ни вызывался тик
 * симуляции, поведение противника от этого не изменится.
 */
public final class EnemyAiService {

    private final AiStrategy strategy;
    private final CollisionService collisionService;
    private final Random random;
    private final Map<Tank, Memory> memoryByTank = new IdentityHashMap<>();

    public EnemyAiService(AiStrategy strategy, CollisionService collisionService, Random random) {
        this.strategy = strategy;
        this.collisionService = collisionService;
        this.random = random;
    }

    /** @return запросы на создание пуль от танков, решивших выстрелить в этом тике */
    public List<BulletSpawnRequest> update(double deltaTimeSeconds, GameWorldView world) {
        this.memoryByTank.entrySet().removeIf(entry -> entry.getKey().isDestroyed());

        List<BulletSpawnRequest> spawnRequests = new ArrayList<>();
        for (Tank tank : world.getTanks()) {
            if (tank.isPlayer() || tank.isDestroyed()) {
                continue;
            }
            Memory memory = this.memoryByTank.computeIfAbsent(tank, ignoredKey -> new Memory(this.random));
            this.updateMovement(tank, memory, deltaTimeSeconds, world);
            this.updateFiring(tank, memory, deltaTimeSeconds, world, spawnRequests);
        }
        return spawnRequests;
    }

    private void updateMovement(Tank tank, Memory memory, double deltaTimeSeconds, GameWorldView world) {
        memory.moveCooldown -= deltaTimeSeconds;
        if (memory.moveCooldown <= 0) {
            AiDecision decision = this.strategy.decide(tank, world, this.random);
            memory.moving = decision.moving();
            if (decision.moving()) {
                tank.setDirection(decision.direction());
            }
            memory.moveCooldown = this.randomBetween(
                    GameConfig.AI_MOVE_DECISION_MIN_SECONDS, GameConfig.AI_MOVE_DECISION_MAX_SECONDS);
        }

        if (memory.moving) {
            double distance = tank.getSpeed() * deltaTimeSeconds;
            double newX = tank.getX() + tank.getDirection().dx() * distance;
            double newY = tank.getY() + tank.getDirection().dy() * distance;
            this.collisionService.tryMoveTank(tank, newX, newY, world.getMap(), world.getTanks());
        }
    }

    private void updateFiring(Tank tank, Memory memory, double deltaTimeSeconds, GameWorldView world,
                               List<BulletSpawnRequest> spawnRequests) {
        memory.fireCooldown -= deltaTimeSeconds;
        if (memory.fireCooldown <= 0) {
            memory.fireCooldown = this.randomBetween(
                    GameConfig.AI_FIRE_CHECK_MIN_SECONDS, GameConfig.AI_FIRE_CHECK_MAX_SECONDS);
            AiDecision decision = this.strategy.decide(tank, world, this.random);
            if (decision.wantsToFire()) {
                tank.tryFire().ifPresent(spawnRequests::add);
            }
        }
    }

    private double randomBetween(double min, double max) {
        return min + this.random.nextDouble() * (max - min);
    }

    /** Таймеры одного вражеского танка между опросами {@link AiStrategy}. */
    private static final class Memory {
        private double moveCooldown;
        private double fireCooldown;
        private boolean moving;

        private Memory(Random random) {
            // Разносим фазы танков во времени, чтобы все враги не "думали" синхронно.
            this.moveCooldown = random.nextDouble() * GameConfig.AI_MOVE_DECISION_MAX_SECONDS;
            this.fireCooldown = random.nextDouble() * GameConfig.AI_FIRE_CHECK_MAX_SECONDS;
        }
    }
}
