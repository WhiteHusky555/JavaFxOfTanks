package ru.rsreu.ineprokin;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.rsreu.ineprokin.navigation.SceneRouter;

/** Точка входа JavaFX. Вся сборка объектов — в {@link SceneRouter}, здесь только запуск. */
public final class TanksApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneRouter router = new SceneRouter(primaryStage);
        router.showMenu();
    }

    public static void main(String[] args) {
        Application.launch(TanksApplication.class, args);
    }
}
