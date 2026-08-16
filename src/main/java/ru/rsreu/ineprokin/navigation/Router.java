package ru.rsreu.ineprokin.navigation;

/**
 * Переключение между тремя экранами приложения. {@code viewmodel}-классы
 * знают только этот интерфейс, а не {@link SceneRouter} с его подробностями
 * про {@code Stage} — благодаря этому их можно тестировать с любой заглушкой.
 */
public interface Router {

    void showMenu();

    void showGame();

    void showAbout();
}
