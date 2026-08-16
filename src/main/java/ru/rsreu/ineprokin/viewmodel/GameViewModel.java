package ru.rsreu.ineprokin.viewmodel;

import ru.rsreu.ineprokin.config.SteeringInput;
import ru.rsreu.ineprokin.engine.GameWorld;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;

/**
 * ViewModel игрового экрана. Это единственный объект, о котором знает
 * {@code view.GameView}: она не видит ни {@link GameWorld}, ни поток
 * симуляции — только команды (руление, выстрел, пауза, подключение
 * игрока) и текущий {@link GameSnapshot}, готовый к отрисовке.
 * <p>
 * Все команды адресуются {@link PlayerId} — {@code view.GameView} сама
 * решает, какому игроку принадлежит нажатая клавиша, эта ViewModel лишь
 * передаёт команду дальше, в {@link GameSimulationLoop}.
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

    /** Клавиша, отвечающая за {@code input}, нажата или отпущена. */
    public void onSteeringInputChanged(PlayerId playerId, SteeringInput input, boolean active) {
        this.loop.setSteering(playerId, input, active);
    }

    public void onFireRequested(PlayerId playerId) {
        this.loop.requestFire(playerId);
    }

    /** Нажата клавиша из раскладки игрока {@code playerId} — если он ещё не в партии, самое время подключиться. */
    public void onPlayerActivationRequested(PlayerId playerId) {
        this.loop.requestActivation(playerId);
    }

    public void onPauseToggleRequested() {
        this.loop.requestPauseToggle();
    }

    /** Вызывается (в FX-потоке) один раз, когда экран результатов отсчитал своё время. */
    public void setOnRoundFinished(Runnable callback) {
        this.loop.setOnRoundFinished(callback);
    }
}
