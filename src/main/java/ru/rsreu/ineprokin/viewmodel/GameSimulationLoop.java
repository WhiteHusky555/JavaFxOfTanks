package ru.rsreu.ineprokin.viewmodel;

import javafx.application.Platform;
import ru.rsreu.ineprokin.config.SteeringInput;
import ru.rsreu.ineprokin.engine.GameConfig;
import ru.rsreu.ineprokin.engine.GameWorld;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.viewmodel.dto.BarrelView;
import ru.rsreu.ineprokin.viewmodel.dto.BulletView;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;
import ru.rsreu.ineprokin.viewmodel.dto.PickupView;
import ru.rsreu.ineprokin.viewmodel.dto.PlayerHudInfo;
import ru.rsreu.ineprokin.viewmodel.dto.TankView;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Продвигает {@link GameWorld} на отдельном потоке-демоне с постоянным шагом
 * ~60 Гц, не завися от частоты кадров рендера.
 * <p>
 * Поток JavaFX (поток отрисовки {@code Canvas}) и поток симуляции обмениваются
 * данными без единого {@code synchronized}: команды игроков публикуются
 * в потокобезопасных множествах ({@link ConcurrentHashMap#newKeySet()}) и
 * {@link AtomicBoolean} (пишет поток JavaFX, читает поток симуляции), а
 * состояние мира — в виде неизменяемого {@link GameSnapshot}, который поток
 * симуляции кладёт в {@link AtomicReference}, а поток JavaFX оттуда читает.
 * Делить нечего — разделяются только ссылки на неизменяемые объекты, поэтому
 * блокировки не нужны в принципе, а не просто "пока не понадобились".
 * <p>
 * Все входные команды адресуются по {@link PlayerId}, включая запрос на
 * подключение второго игрока ({@link #requestActivation}) — сама партия
 * ({@link GameWorld}) уже умеет держать произвольный состав игроков, цикл
 * симуляции лишь передаёт ей команды под правильным идентификатором.
 * <p>
 * Единственный момент, где поток симуляции обязан передать управление
 * обратно в JavaFX — переход в главное меню после экрана результатов —
 * оформлен через {@link Platform#runLater(Runnable)}, как того требует
 * правило JavaFX "трогать сцену можно только из FX-потока".
 */
public final class GameSimulationLoop implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(GameSimulationLoop.class.getName());
    private static final long TICK_PERIOD_MILLIS = 16L; // ≈ 60 обновлений в секунду

    private final GameWorld world;
    private final ScheduledExecutorService executor;
    private final AtomicReference<GameSnapshot> latestSnapshot;

    private final Set<PlayerId> turnLeftPlayers = ConcurrentHashMap.newKeySet();
    private final Set<PlayerId> turnRightPlayers = ConcurrentHashMap.newKeySet();
    private final Set<PlayerId> moveForwardPlayers = ConcurrentHashMap.newKeySet();
    private final Set<PlayerId> moveBackwardPlayers = ConcurrentHashMap.newKeySet();
    private final Set<PlayerId> firePendingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<PlayerId> activationPendingPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean pauseToggleRequested = new AtomicBoolean(false);

    private volatile Runnable onRoundFinished;
    private ScheduledFuture<?> scheduledTask;

    // Поля ниже читает и пишет исключительно сам поток симуляции (внутри run()),
    // поэтому синхронизация им не требуется — доступ строго последовательный.
    private long lastTickNanos = -1L;
    private double resultsElapsedSeconds;
    private boolean resultsAnnounced;
    private double smoothedFps;

    public GameSimulationLoop(GameWorld world) {
        this.world = world;
        this.executor = Executors.newSingleThreadScheduledExecutor(this::newDaemonThread);
        this.latestSnapshot = new AtomicReference<>(this.buildSnapshot(0.0));
    }

    private Thread newDaemonThread(Runnable task) {
        Thread thread = new Thread(task, "tanks-simulation");
        thread.setDaemon(true);
        return thread;
    }

    public void start() {
        if (this.scheduledTask == null) {
            this.scheduledTask = this.executor.scheduleAtFixedRate(this, 0, TICK_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(false);
        }
        this.executor.shutdown();
    }

    /** Единица работы, которую исполнитель периодически прогоняет в фоновом потоке. */
    @Override
    public void run() {
        try {
            double deltaTimeSeconds = this.computeDeltaSeconds(System.nanoTime());
            this.applyPendingInput(deltaTimeSeconds);
            this.world.tick(deltaTimeSeconds);
            this.advanceResultsCountdown(deltaTimeSeconds);
            this.latestSnapshot.set(this.buildSnapshot(deltaTimeSeconds));
        } catch (RuntimeException e) {
            // scheduleAtFixedRate молча "хоронит" задачу при необработанном исключении —
            // логируем и отдаём следующему тику шанс продолжить, а не роняем весь цикл.
            LOGGER.log(Level.SEVERE, "Необработанная ошибка в цикле симуляции", e);
        }
    }

    private double computeDeltaSeconds(long nowNanos) {
        if (this.lastTickNanos < 0) {
            this.lastTickNanos = nowNanos;
            return 0.0;
        }
        double deltaTimeSeconds = (nowNanos - this.lastTickNanos) / 1_000_000_000.0;
        this.lastTickNanos = nowNanos;
        // Ограничиваем скачок дельты (после паузы отладчика, сна ОС и т.п.),
        // чтобы танк не "телепортировался" сквозь стену за один огромный тик.
        return Math.min(deltaTimeSeconds, 0.05);
    }

    private void applyPendingInput(double deltaTimeSeconds) {
        // Подключение — раньше руления и стрельбы: если это первое нажатие
        // клавиши вторым игроком, тот же кадр должен и создать ему танк,
        // и сразу сдвинуть/выстрелить им, а не потребовать вторую попытку.
        for (PlayerId playerId : PlayerId.values()) {
            if (this.activationPendingPlayers.remove(playerId)) {
                this.world.activatePlayer(playerId);
            }
        }

        for (PlayerId playerId : PlayerId.values()) {
            double turnDirection = this.axisValue(playerId, this.turnLeftPlayers, this.turnRightPlayers);
            double throttle = this.axisValue(playerId, this.moveBackwardPlayers, this.moveForwardPlayers);
            if (turnDirection != 0 || throttle != 0) {
                this.world.steerPlayer(playerId, turnDirection, throttle, deltaTimeSeconds);
            }
        }

        for (PlayerId playerId : PlayerId.values()) {
            if (this.firePendingPlayers.remove(playerId)) {
                this.world.firePlayer(playerId);
            }
        }
        if (this.pauseToggleRequested.getAndSet(false)) {
            this.world.togglePause();
        }
    }

    /** {@code -1}, если активна только "отрицательная" клавиша, {@code +1} — только "положительная", иначе {@code 0}. */
    private double axisValue(PlayerId playerId, Set<PlayerId> negative, Set<PlayerId> positive) {
        double value = 0;
        if (negative.contains(playerId)) {
            value -= 1;
        }
        if (positive.contains(playerId)) {
            value += 1;
        }
        return value;
    }

    private void advanceResultsCountdown(double deltaTimeSeconds) {
        if (this.world.getState() != GameState.GAME_OVER) {
            this.resultsElapsedSeconds = 0.0;
            this.resultsAnnounced = false;
            return;
        }
        if (this.resultsAnnounced) {
            return;
        }
        this.resultsElapsedSeconds += deltaTimeSeconds;
        if (this.resultsElapsedSeconds >= GameConfig.RESULTS_SCREEN_SECONDS) {
            this.resultsAnnounced = true;
            Runnable callback = this.onRoundFinished;
            if (callback != null) {
                Platform.runLater(callback);
            }
        }
    }

    private GameSnapshot buildSnapshot(double deltaTimeSeconds) {
        double instantFps = deltaTimeSeconds > 0 ? 1.0 / deltaTimeSeconds : this.smoothedFps;
        this.smoothedFps = this.smoothedFps <= 0 ? instantFps : (this.smoothedFps * 0.9 + instantFps * 0.1);

        List<TankView> tankViews = this.world.getTanks().stream()
                .map(tank -> new TankView(
                        tank.getX(), tank.getY(), tank.getHeadingDegrees(), tank.getPlayerId().orElse(null),
                        tank.getHealth(), tank.getMaxHealth(), tank.getReloadProgress(), tank.isInvulnerable()))
                .toList();
        List<BulletView> bulletViews = this.world.getBullets().stream()
                .map(bullet -> new BulletView(bullet.getX(), bullet.getY(), bullet.isFromPlayer()))
                .toList();
        List<PickupView> pickupViews = this.world.getPickups().stream()
                .map(pickup -> new PickupView(pickup.getX(), pickup.getY(), pickup.getType()))
                .toList();
        List<BarrelView> barrelViews = this.world.getBarrels().stream()
                .map(barrel -> new BarrelView(barrel.getX(), barrel.getY()))
                .toList();

        int secondsLeft = (int) Math.max(1, Math.ceil(GameConfig.RESULTS_SCREEN_SECONDS - this.resultsElapsedSeconds));

        return new GameSnapshot(
                this.world.getMap(), tankViews, bulletViews, pickupViews, barrelViews,
                this.playerHudInfo(PlayerId.PLAYER_ONE), this.playerHudInfo(PlayerId.PLAYER_TWO),
                this.world.getState(), this.smoothedFps,
                this.world.getState() == GameState.GAME_OVER, secondsLeft);
    }

    private PlayerHudInfo playerHudInfo(PlayerId playerId) {
        if (!this.world.isPlayerAvailable(playerId)) {
            return PlayerHudInfo.unavailable();
        }
        return new PlayerHudInfo(
                true, this.world.isPlayerActive(playerId),
                this.world.getScore(playerId),
                this.world.getPlayerHealth(playerId), this.world.getPlayerMaxHealth(playerId),
                this.world.getPlayerReloadProgress(playerId), this.world.getExtraLives(playerId));
    }

    public GameSnapshot latestSnapshot() {
        return this.latestSnapshot.get();
    }

    /** Клавиша действия {@code input} нажата ({@code active=true}) или отпущена ({@code active=false}). */
    public void setSteering(PlayerId playerId, SteeringInput input, boolean active) {
        Set<PlayerId> target = this.playersFor(input);
        if (active) {
            target.add(playerId);
        } else {
            target.remove(playerId);
        }
    }

    private Set<PlayerId> playersFor(SteeringInput input) {
        return switch (input) {
            case TURN_LEFT -> this.turnLeftPlayers;
            case TURN_RIGHT -> this.turnRightPlayers;
            case MOVE_FORWARD -> this.moveForwardPlayers;
            case MOVE_BACKWARD -> this.moveBackwardPlayers;
        };
    }

    public void requestFire(PlayerId playerId) {
        this.firePendingPlayers.add(playerId);
    }

    /** Просит подключить {@code playerId} к партии — не-op, если он уже играет или карта его не поддерживает. */
    public void requestActivation(PlayerId playerId) {
        this.activationPendingPlayers.add(playerId);
    }

    public void requestPauseToggle() {
        this.pauseToggleRequested.set(true);
    }

    public void setOnRoundFinished(Runnable callback) {
        this.onRoundFinished = callback;
    }
}
