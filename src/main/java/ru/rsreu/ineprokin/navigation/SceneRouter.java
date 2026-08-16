package ru.rsreu.ineprokin.navigation;

import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.rsreu.ineprokin.config.ControlsConfig;
import ru.rsreu.ineprokin.engine.GameWorld;
import ru.rsreu.ineprokin.model.map.GameMap;
import ru.rsreu.ineprokin.view.AboutView;
import ru.rsreu.ineprokin.view.GameView;
import ru.rsreu.ineprokin.view.MenuView;
import ru.rsreu.ineprokin.viewmodel.AboutViewModel;
import ru.rsreu.ineprokin.viewmodel.GameViewModel;
import ru.rsreu.ineprokin.viewmodel.MenuViewModel;

import java.io.InputStream;

/**
 * Composition root: единственное место, которое знает про {@link Stage}
 * и умеет собрать пару view/viewmodel для каждого из трёх экранов.
 * Заменяет собой {@code ApplicationController} исходной версии.
 */
public final class SceneRouter implements Router {

    private static final String MAP_RESOURCE = "/ru/rsreu/ineprokin/map.txt";

    private final Stage stage;
    private final ControlsConfig controlsConfig;

    private GameViewModel activeGameViewModel;

    public SceneRouter(Stage stage) {
        this.stage = stage;
        this.controlsConfig = ControlsConfig.loadDefault();
    }

    @Override
    public void showMenu() {
        this.stopActiveGame();
        MenuViewModel viewModel = new MenuViewModel(this);
        MenuView view = new MenuView(viewModel);
        this.present("JavaFX of Tanks — Меню", view.createScene());
    }

    @Override
    public void showGame() {
        this.stopActiveGame();

        GameMap map = GameMap.load(this.openMapResource());
        GameWorld world = GameWorld.createDefault(map);
        GameViewModel viewModel = new GameViewModel(world);
        this.activeGameViewModel = viewModel;

        GameView view = new GameView(viewModel, this.controlsConfig, this::showMenu);
        this.present("JavaFX of Tanks", view.createScene());
        viewModel.start();
    }

    @Override
    public void showAbout() {
        this.stopActiveGame();
        AboutViewModel viewModel = new AboutViewModel(this);
        AboutView view = new AboutView(viewModel);
        this.present("JavaFX of Tanks — О программе", view.createScene());
    }

    private void present(String title, Scene scene) {
        this.stage.setTitle(title);
        this.stage.setScene(scene);
        this.stage.setResizable(false);
        this.stage.sizeToScene();
        this.stage.centerOnScreen();
        this.stage.show();
    }

    private void stopActiveGame() {
        if (this.activeGameViewModel != null) {
            this.activeGameViewModel.stop();
            this.activeGameViewModel = null;
        }
    }

    private InputStream openMapResource() {
        InputStream input = SceneRouter.class.getResourceAsStream(SceneRouter.MAP_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Файл карты не найден: " + SceneRouter.MAP_RESOURCE);
        }
        return input;
    }
}
