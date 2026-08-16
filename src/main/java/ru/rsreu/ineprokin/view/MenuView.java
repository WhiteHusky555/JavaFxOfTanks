package ru.rsreu.ineprokin.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import ru.rsreu.ineprokin.viewmodel.MenuViewModel;

/** Главное меню: три кнопки, три команды {@link MenuViewModel}. */
public final class MenuView {

    private static final double WIDTH = 360;
    private static final double HEIGHT = 320;
    private static final double BUTTON_WIDTH = 220;

    private final MenuViewModel viewModel;

    public MenuView(MenuViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public Scene createScene() {
        Button newGameButton = this.button("Новая игра", this.viewModel::onNewGameRequested);
        Button aboutButton = this.button("О программе", this.viewModel::onAboutRequested);
        Button exitButton = this.button("Выход", this.viewModel::onExitRequested);

        VBox root = new VBox(16, newGameButton, aboutButton, exitButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #121216;");

        return new Scene(root, MenuView.WIDTH, MenuView.HEIGHT);
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(MenuView.BUTTON_WIDTH);
        button.setOnAction(event -> action.run());
        return button;
    }
}
