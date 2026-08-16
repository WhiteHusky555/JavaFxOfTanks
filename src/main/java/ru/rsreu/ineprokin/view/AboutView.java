package ru.rsreu.ineprokin.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import ru.rsreu.ineprokin.model.AboutInfo;
import ru.rsreu.ineprokin.viewmodel.AboutViewModel;

/** Экран "О программе": статичные сведения из {@link AboutInfo} плюс кнопка "Назад". */
public final class AboutView {

    private static final double WIDTH = 460;
    private static final double HEIGHT = 360;

    private final AboutViewModel viewModel;

    public AboutView(AboutViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public Scene createScene() {
        AboutInfo info = this.viewModel.info();

        Label title = new Label(info.appName() + "  v" + info.version());
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#E6E6EB"));

        Label author = new Label("Автор: " + info.author());
        Label controls = new Label(info.controls());
        Label objective = new Label(info.objective());
        for (Label label : new Label[]{author, controls, objective}) {
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.CENTER);
            label.setTextFill(Color.web("#C7C7CF"));
        }

        Button backButton = new Button("Назад");
        backButton.setPrefWidth(160);
        backButton.setOnAction(event -> this.viewModel.onBackRequested());

        VBox root = new VBox(14, title, author, controls, objective, backButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setMaxWidth(AboutView.WIDTH - 48);
        root.setStyle("-fx-background-color: #121216;");

        VBox.setMargin(backButton, new Insets(10, 0, 0, 0));

        return new Scene(root, AboutView.WIDTH, AboutView.HEIGHT);
    }
}
