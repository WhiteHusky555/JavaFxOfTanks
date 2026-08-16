package ru.rsreu.ineprokin.view;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import ru.rsreu.ineprokin.config.ControlsConfig;
import ru.rsreu.ineprokin.config.ThemeConfig;
import ru.rsreu.ineprokin.viewmodel.GameViewModel;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;

/**
 * Игровой экран: {@link Canvas} плюс перевод клавиатурных событий в команды
 * {@link GameViewModel}.
 * <p>
 * Отрисовка и симуляция партии сознательно разведены по двум разным
 * тактам. Симуляция ({@code viewmodel.GameSimulationLoop}) крутится в
 * фоновом потоке с постоянным шагом ~60 Гц и не имеет понятия о частоте
 * кадров — двигаясь на {@code скорость × реальное время}, а не на
 * фиксированное число пикселей за кадр. А {@link AnimationTimer#handle(long)}
 * ниже вызывается ровно раз за такт рендер-пайплайна JavaFX — этот такт сам
 * пайплайн синхронизирует с вертикальной разверткой монитора (vsync), если
 * она включена драйвером, поэтому дополнительный ручной троттлинг кадров
 * в прикладном коде не нужен и был бы лишним. Каждый вызов {@code handle}
 * просто читает последний готовый {@link GameSnapshot} и рисует его —
 * даже если частота кадров вдруг просядет или, наоборот, вырастет,
 * танки не ускорятся и не замедлятся.
 */
public final class GameView {

    private final GameViewModel viewModel;
    private final ControlsConfig controls;
    private final Runnable onExitToMenu;
    private final GameRenderer renderer;

    private AnimationTimer animationTimer;

    public GameView(GameViewModel viewModel, ControlsConfig controls, Runnable onExitToMenu) {
        this.viewModel = viewModel;
        this.controls = controls;
        this.onExitToMenu = onExitToMenu;
        this.renderer = new GameRenderer(ThemeConfig.loadDefault());
        this.viewModel.setOnRoundFinished(this::exitToMenu);
    }

    public Scene createScene() {
        GameSnapshot initial = this.viewModel.latestSnapshot();
        double width = initial.map().widthInPixels();
        double height = initial.map().heightInPixels() + GameRenderer.HUD_HEIGHT;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        Scene scene = new Scene(new StackPane(canvas), width, height);
        scene.setOnKeyPressed(event -> this.handleKeyPressed(event.getCode()));
        scene.setOnKeyReleased(event -> this.handleKeyReleased(event.getCode()));

        this.animationTimer = new FrameTick(canvas, graphics);
        this.animationTimer.start();

        return scene;
    }

    private void handleKeyPressed(KeyCode code) {
        if (this.controls.isBackKey(code)) {
            this.exitToMenu();
            return;
        }
        if (this.controls.isPauseKey(code)) {
            this.viewModel.onPauseToggleRequested();
            return;
        }
        if (this.controls.isFireKey(code)) {
            this.viewModel.onFireRequested();
            return;
        }
        this.controls.directionFor(code).ifPresent(this.viewModel::onDirectionKeyDown);
    }

    private void handleKeyReleased(KeyCode code) {
        this.controls.directionFor(code).ifPresent(this.viewModel::onDirectionKeyUp);
    }

    /** Выход с игрового экрана — по Esc или по завершении отсчёта на экране результатов. */
    private void exitToMenu() {
        if (this.animationTimer != null) {
            this.animationTimer.stop();
        }
        this.viewModel.stop();
        this.onExitToMenu.run();
    }

    /** Один такт рендер-пайплайна JavaFX: прочитать снимок партии и нарисовать его. */
    private final class FrameTick extends AnimationTimer {

        private final Canvas canvas;
        private final GraphicsContext graphics;

        private FrameTick(Canvas canvas, GraphicsContext graphics) {
            this.canvas = canvas;
            this.graphics = graphics;
        }

        @Override
        public void handle(long now) {
            GameSnapshot snapshot = GameView.this.viewModel.latestSnapshot();
            GameView.this.renderer.render(this.graphics, snapshot, this.canvas.getWidth(), this.canvas.getHeight());
        }
    }
}
