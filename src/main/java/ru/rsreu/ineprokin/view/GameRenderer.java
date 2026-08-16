package ru.rsreu.ineprokin.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import ru.rsreu.ineprokin.config.ThemeConfig;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.PlayerId;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.map.GameMap;
import ru.rsreu.ineprokin.viewmodel.dto.BulletView;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;
import ru.rsreu.ineprokin.viewmodel.dto.TankView;

/**
 * Рисует один {@link GameSnapshot} на {@link GraphicsContext}. Ничего не
 * хранит между кадрами и ничего не знает о {@code GameWorld} — только
 * иммутабельный снимок на входе, значит, рендерер можно вызывать из теста
 * с рукописным снимком без запуска JavaFX-приложения.
 */
public final class GameRenderer {

    public static final double HUD_HEIGHT = 56.0;
    private static final double TANK_SIZE = Tank.SIZE;
    private static final double BULLET_RADIUS = 3.0;
    private static final double HEALTH_BAR_HEIGHT = 6.0;
    private static final double RELOAD_RING_RADIUS = 7.0;
    private static final double RELOAD_RING_LINE_WIDTH = 3.0;

    private final ThemeConfig theme;

    public GameRenderer(ThemeConfig theme) {
        this.theme = theme;
    }

    public void render(GraphicsContext gc, GameSnapshot snapshot, double width, double height) {
        this.drawBackground(gc, width, height);
        this.drawMap(gc, snapshot.map());
        for (TankView tank : snapshot.tanks()) {
            this.drawTank(gc, tank);
        }
        for (BulletView bullet : snapshot.bullets()) {
            this.drawBullet(gc, bullet);
        }
        this.drawHud(gc, snapshot, width);
        this.drawStateOverlay(gc, snapshot, width, height);
        if (snapshot.resultsVisible()) {
            this.drawResultsScreen(gc, snapshot, width, height);
        }
    }

    private void drawBackground(GraphicsContext gc, double width, double height) {
        gc.setFill(this.theme.hudBackground());
        gc.fillRect(0, 0, width, GameRenderer.HUD_HEIGHT);
        gc.setFill(this.theme.background());
        gc.fillRect(0, GameRenderer.HUD_HEIGHT, width, height - GameRenderer.HUD_HEIGHT);
    }

    private void drawMap(GraphicsContext gc, GameMap map) {
        gc.setFill(this.theme.wall());
        for (int row = 0; row < map.height(); row++) {
            for (int col = 0; col < map.width(); col++) {
                if (map.isWall(col, row)) {
                    gc.fillRect(col * GameMap.TILE_SIZE, this.mapY(row * GameMap.TILE_SIZE),
                            GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                }
            }
        }
    }

    private void drawTank(GraphicsContext gc, TankView tank) {
        double x = tank.x();
        double y = this.mapY(tank.y());
        double centerX = x + GameRenderer.TANK_SIZE / 2.0;
        double centerY = y + GameRenderer.TANK_SIZE / 2.0;
        double turretLength = GameRenderer.TANK_SIZE * 0.75;

        // Корпус и ствол рисуются в повёрнутой системе координат — курс танка
        // виден по всей его форме, а не только по отдельной черте-стволу.
        gc.save();
        gc.translate(centerX, centerY);
        gc.rotate(tank.headingDegrees());

        gc.setFill(this.hullColorFor(tank.playerId()));
        gc.fillRect(-GameRenderer.TANK_SIZE / 2.0, -GameRenderer.TANK_SIZE / 2.0, GameRenderer.TANK_SIZE, GameRenderer.TANK_SIZE);

        gc.setStroke(this.theme.turret());
        gc.setLineWidth(4);
        gc.strokeLine(0, 0, 0, -turretLength);
        gc.restore();

        if (!tank.isPlayerControlled() && tank.health() < tank.maxHealth()) {
            this.drawHealthBar(gc, x, y, tank.health(), tank.maxHealth());
        }
        // Кольцо перезарядки игрока рисует drawReloadStatus в неподвижной
        // полосе HUD, а не здесь, у самого танка на поле боя.
    }

    private Color hullColorFor(PlayerId playerId) {
        if (playerId == null) {
            return this.theme.enemyHull();
        }
        // Оба игрока — один зелёный корпус, пока второй реально не появился на поле;
        // здесь единственное место, которое нужно тронуть, чтобы различать игроков цветом.
        return this.theme.playerHull();
    }

    private void drawHealthBar(GraphicsContext gc, double tankX, double tankY, int health, int maxHealth) {
        double barY = tankY - 12.0;

        gc.setFill(this.theme.healthBarBackground());
        gc.fillRect(tankX, barY, GameRenderer.TANK_SIZE, GameRenderer.HEALTH_BAR_HEIGHT);

        double ratio = (double) health / maxHealth;
        gc.setFill(this.healthColorFor(ratio));
        gc.fillRect(tankX, barY, GameRenderer.TANK_SIZE * ratio, GameRenderer.HEALTH_BAR_HEIGHT);

        gc.setStroke(this.theme.hudText());
        gc.setLineWidth(1);
        gc.strokeRect(tankX, barY, GameRenderer.TANK_SIZE, GameRenderer.HEALTH_BAR_HEIGHT);
    }

    /**
     * Кольцо перезарядки орудия под танком игрока: тонкий фоновый круг и поверх
     * него дуга, которая заполняет круг по часовой стрелке от 12 часов, пока
     * орудие перезаряжается, и становится полной окружностью, когда оно готово.
     */
    private void drawReloadRing(GraphicsContext gc, double centerX, double centerY, double reloadProgress) {
        double diameter = GameRenderer.RELOAD_RING_RADIUS * 2;
        double left = centerX - GameRenderer.RELOAD_RING_RADIUS;
        double top = centerY - GameRenderer.RELOAD_RING_RADIUS;

        gc.setStroke(this.theme.reloadBarBackground());
        gc.setLineWidth(GameRenderer.RELOAD_RING_LINE_WIDTH);
        gc.strokeOval(left, top, diameter, diameter);

        gc.setStroke(reloadProgress >= 1.0 ? this.theme.reloadReady() : this.theme.reloadCharging());
        gc.setLineWidth(GameRenderer.RELOAD_RING_LINE_WIDTH);
        // 90° — верх циферблата; отрицательный размах дуги заполняет её по часовой стрелке.
        gc.strokeArc(left, top, diameter, diameter, 90, -360.0 * reloadProgress, ArcType.OPEN);
    }

    private Color healthColorFor(double ratio) {
        if (ratio > 0.6) {
            return this.theme.healthGood();
        }
        if (ratio > 0.3) {
            return this.theme.healthMedium();
        }
        return this.theme.healthLow();
    }

    private void drawBullet(GraphicsContext gc, BulletView bullet) {
        gc.setFill(bullet.fromPlayer() ? this.theme.bulletPlayer() : this.theme.bulletEnemy());
        double x = bullet.x();
        double y = this.mapY(bullet.y());
        gc.fillOval(x - GameRenderer.BULLET_RADIUS, y - GameRenderer.BULLET_RADIUS,
                GameRenderer.BULLET_RADIUS * 2, GameRenderer.BULLET_RADIUS * 2);
    }

    private void drawHud(GraphicsContext gc, GameSnapshot snapshot, double width) {
        gc.setFill(this.theme.hudText());
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));

        gc.fillText("Очки: " + snapshot.score(), 16, 34);
        gc.fillText("Жизни: " + snapshot.playerHealth() + " / " + snapshot.playerMaxHealth(), 190, 34);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(String.format("FPS: %.0f", snapshot.fps()), width - 16, 34);
        gc.setTextAlign(TextAlignment.LEFT);

        this.drawReloadStatus(gc, snapshot.playerReloadProgress());
    }

    /**
     * Статус перезарядки — в неподвижной полосе HUD: в отличие от точки на
     * поле боя, эта позиция не заслоняется пулями, стенами или другими танками.
     */
    private void drawReloadStatus(GraphicsContext gc, double reloadProgress) {
        gc.setFill(this.theme.hudText());
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 14));
        gc.fillText("Орудие:", 16, 50);

        // Само кольцо цветом и заполнением показывает готовность — короткой
        // подписи слева достаточно, чтобы не гадать, к чему оно относится.
        this.drawReloadRing(gc, 78, 45, reloadProgress);
    }

    private void drawStateOverlay(GraphicsContext gc, GameSnapshot snapshot, double width, double height) {
        if (snapshot.state() == GameState.PAUSED) {
            this.drawCentered(gc, "ПАУЗА", this.theme.pauseText(), 48, width, height / 2.0);
        } else if (snapshot.state() == GameState.GAME_OVER && !snapshot.resultsVisible()) {
            this.drawCentered(gc, "ИГРА ОКОНЧЕНА", this.theme.gameOverText(), 44, width, height / 2.0 - 20);
        }
    }

    private void drawResultsScreen(GraphicsContext gc, GameSnapshot snapshot, double width, double height) {
        gc.setFill(this.theme.resultsOverlay());
        gc.fillRect(0, 0, width, height);

        this.drawCentered(gc, "ИГРА ОКОНЧЕНА", this.theme.gameOverText(), 44, width, height / 2.0 - 70);
        this.drawCentered(gc, "Счёт: " + snapshot.score(), this.theme.hudText(), 30, width, height / 2.0 - 10);

        gc.setFill(this.theme.hudText());
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 20));
        gc.fillText("Возврат в меню через: " + snapshot.resultsSecondsLeft(), width / 2.0, height / 2.0 + 50);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawCentered(GraphicsContext gc, String text, Color color, double fontSize, double width, double y) {
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, fontSize));
        gc.fillText(text, width / 2.0, y);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /** Переводит Y карты в экранные координаты — область карты начинается ниже полосы HUD. */
    private double mapY(double mapPixelY) {
        return mapPixelY + GameRenderer.HUD_HEIGHT;
    }
}
