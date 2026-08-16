package ru.rsreu.ineprokin.viewmodel;

import ru.rsreu.ineprokin.engine.GameWorld;
import ru.rsreu.ineprokin.model.entity.PlayerId;
import ru.rsreu.ineprokin.model.geometry.Direction;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;

/**
 * ViewModel игрового экрана. Это единственный объект, о котором знает
 * {@code view.GameView}: она не видит ни {@link GameWorld}, ни поток
 * симуляции — только команды (движение, выстрел, пауза) и текущий
 * {@link GameSnapshot}, готовый к отрисовке.
 * <p>
 * Публичный API сегодня управляет только {@link PlayerId#PLAYER_ONE} —
 * это осознанная граница текущего экрана, а не движка: {@link GameWorld}
 * и {@link GameSimulationLoop} уже умеют работать с произвольным набором
 * игроков, так что добавление второго игрока — это добавление второго
 * набора клавиш и параметра {@code PlayerId} здесь и в {@code GameView},
 * без изменений в модели или потоке симуляции.
 */
public final class GameViewModel {

    private final GameSimulationLoop loop;

    public GameViewModel(GameWorld world) {
        this.loop = new GameSimulationLoop(world);
    }

    public void start() {
        this.loop.start();
    }

    public void stop() {
        this.loop.stop();
    }

    public GameSnapshot latestSnapshot() {
        return this.loop.latestSnapshot();
    }

    public void onDirectionKeyDown(Direction direction) {
        this.loop.setHeldDirection(PlayerId.PLAYER_ONE, direction);
    }

    public void onDirectionKeyUp(Direction direction) {
        this.loop.clearHeldDirectionIfMatches(PlayerId.PLAYER_ONE, direction);
    }

    public void onFireRequested() {
        this.loop.requestFire(PlayerId.PLAYER_ONE);
    }

    public void onPauseToggleRequested() {
        this.loop.requestPauseToggle();
    }

    /** Вызывается (в FX-потоке) один раз, когда экран результатов отсчитал своё время. */
    public void setOnRoundFinished(Runnable callback) {
        this.loop.setOnRoundFinished(callback);
    }
}
