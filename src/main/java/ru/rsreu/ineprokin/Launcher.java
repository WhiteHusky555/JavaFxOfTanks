package ru.rsreu.ineprokin;

/**
 * Отдельный класс без наследования от {@code Application} — точка входа для
 * собранного {@code java -jar}. JavaFX отказывается запускать jar, чей
 * Main-Class сам расширяет {@code Application}, если модульный путь не
 * настроен явно; обходной манёвр — тонкий не-{@code Application} лаунчер.
 */
public final class Launcher {

    public static void main(String[] args) {
        TanksApplication.main(args);
    }
}
