package ru.rsreu.ineprokin.viewmodel;

import ru.rsreu.ineprokin.config.AboutContent;
import ru.rsreu.ineprokin.model.AboutInfo;
import ru.rsreu.ineprokin.navigation.Router;

/** ViewModel экрана "О программе" — статичные сведения плюс кнопка "Назад". */
public final class AboutViewModel {

    private final Router router;
    private final AboutInfo info;

    public AboutViewModel(Router router) {
        this.router = router;
        this.info = AboutContent.load();
    }

    public AboutInfo info() {
        return this.info;
    }

    public void onBackRequested() {
        this.router.showMenu();
    }
}
