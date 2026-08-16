package ru.rsreu.ineprokin.viewmodel;

import javafx.application.Platform;
import ru.rsreu.ineprokin.navigation.Router;

/** ViewModel главного меню — три пункта, три команды. */
public final class MenuViewModel {

    private final Router router;

    public MenuViewModel(Router router) {
        this.router = router;
    }

    public void onNewGameRequested() {
        this.router.showGame();
    }

    public void onAboutRequested() {
        this.router.showAbout();
    }

    public void onExitRequested() {
        Platform.exit();
    }
}
