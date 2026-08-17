package ru.rsreu.ineprokin.config;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Цветовая схема игрового экрана, вычитанная из {@code theme.properties}
 * при старте — вместо {@code Color}-констант, зашитых в код отрисовки.
 * Цвета разбираются один раз при загрузке (а не на каждом кадре), поэтому
 * рендерер получает уже готовые {@link Color}.
 */
public final class ThemeConfig {

    private static final String RESOURCE_PATH = "/ru/rsreu/ineprokin/theme.properties";

    private final Color background;
    private final Color hudBackground;
    private final Color wall;
    private final Color playerHull;
    private final Color playerTwoHull;
    private final Color enemyHull;
    private final Color turret;
    private final Color bulletPlayer;
    private final Color bulletEnemy;
    private final Color healthBarBackground;
    private final Color healthGood;
    private final Color healthMedium;
    private final Color healthLow;
    private final Color reloadBarBackground;
    private final Color reloadReady;
    private final Color reloadCharging;
    private final Color pickupExtraLifeBody;
    private final Color explosionRing;
    private final Color hudText;
    private final Color pauseText;
    private final Color gameOverText;
    private final Color resultsOverlay;
    private final Color joinHintText;

    private ThemeConfig(Properties properties) {
        this.background = ThemeConfig.colorOf(properties, "background");
        this.hudBackground = ThemeConfig.colorOf(properties, "hud.background");
        this.wall = ThemeConfig.colorOf(properties, "wall");
        this.playerHull = ThemeConfig.colorOf(properties, "player.hull");
        this.playerTwoHull = ThemeConfig.colorOf(properties, "player.two.hull");
        this.enemyHull = ThemeConfig.colorOf(properties, "enemy.hull");
        this.turret = ThemeConfig.colorOf(properties, "turret");
        this.bulletPlayer = ThemeConfig.colorOf(properties, "bullet.player");
        this.bulletEnemy = ThemeConfig.colorOf(properties, "bullet.enemy");
        this.healthBarBackground = ThemeConfig.colorOf(properties, "health.bar.background");
        this.healthGood = ThemeConfig.colorOf(properties, "health.good");
        this.healthMedium = ThemeConfig.colorOf(properties, "health.medium");
        this.healthLow = ThemeConfig.colorOf(properties, "health.low");
        this.reloadBarBackground = ThemeConfig.colorOf(properties, "reload.bar.background");
        this.reloadReady = ThemeConfig.colorOf(properties, "reload.ready");
        this.reloadCharging = ThemeConfig.colorOf(properties, "reload.charging");
        this.pickupExtraLifeBody = ThemeConfig.colorOf(properties, "pickup.extra.life.body");
        this.explosionRing = ThemeConfig.colorOf(properties, "explosion.ring");
        this.hudText = ThemeConfig.colorOf(properties, "hud.text");
        this.pauseText = ThemeConfig.colorOf(properties, "pause.text");
        this.gameOverText = ThemeConfig.colorOf(properties, "game.over.text");
        this.resultsOverlay = ThemeConfig.colorOf(properties, "results.overlay");
        this.joinHintText = ThemeConfig.colorOf(properties, "join.hint.text");
    }

    public static ThemeConfig loadDefault() {
        try (InputStream input = ThemeConfig.class.getResourceAsStream(ThemeConfig.RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Файл темы не найден: " + ThemeConfig.RESOURCE_PATH);
            }
            Properties properties = new Properties();
            properties.load(input);
            return new ThemeConfig(properties);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл темы", e);
        }
    }

    private static Color colorOf(Properties properties, String key) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("В файле темы не задан цвет '" + key + "'");
        }
        return Color.web(raw.trim());
    }

    public Color background() {
        return this.background;
    }

    public Color hudBackground() {
        return this.hudBackground;
    }

    public Color wall() {
        return this.wall;
    }

    public Color playerHull() {
        return this.playerHull;
    }

    public Color playerTwoHull() {
        return this.playerTwoHull;
    }

    public Color enemyHull() {
        return this.enemyHull;
    }

    public Color turret() {
        return this.turret;
    }

    public Color bulletPlayer() {
        return this.bulletPlayer;
    }

    public Color bulletEnemy() {
        return this.bulletEnemy;
    }

    public Color healthBarBackground() {
        return this.healthBarBackground;
    }

    public Color healthGood() {
        return this.healthGood;
    }

    public Color healthMedium() {
        return this.healthMedium;
    }

    public Color healthLow() {
        return this.healthLow;
    }

    public Color reloadBarBackground() {
        return this.reloadBarBackground;
    }

    public Color reloadReady() {
        return this.reloadReady;
    }

    public Color reloadCharging() {
        return this.reloadCharging;
    }

    public Color pickupExtraLifeBody() {
        return this.pickupExtraLifeBody;
    }

    public Color explosionRing() {
        return this.explosionRing;
    }

    public Color hudText() {
        return this.hudText;
    }

    public Color pauseText() {
        return this.pauseText;
    }

    public Color gameOverText() {
        return this.gameOverText;
    }

    public Color resultsOverlay() {
        return this.resultsOverlay;
    }

    public Color joinHintText() {
        return this.joinHintText;
    }
}
