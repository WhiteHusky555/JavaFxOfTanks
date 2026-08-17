package ru.rsreu.ineprokin.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import ru.rsreu.ineprokin.config.ThemeConfig;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.ExplosiveBarrel;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.Pickup;
import ru.rsreu.ineprokin.model.entity.PickupType;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.map.GameMap;
import ru.rsreu.ineprokin.viewmodel.dto.BarrelView;
import ru.rsreu.ineprokin.viewmodel.dto.BulletView;
import ru.rsreu.ineprokin.viewmodel.dto.ExplosionView;
import ru.rsreu.ineprokin.viewmodel.dto.GameSnapshot;
import ru.rsreu.ineprokin.viewmodel.dto.PickupView;
import ru.rsreu.ineprokin.viewmodel.dto.PlayerHudInfo;
import ru.rsreu.ineprokin.viewmodel.dto.TankView;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Рисует один {@link GameSnapshot} на {@link GraphicsContext}. Ничего не
 * хранит между кадрами и ничего не знает о {@code GameWorld} — только
 * иммутабельный снимок на входе, значит, рендерер можно вызывать из теста
 * с рукописным снимком без запуска JavaFX-приложения.
 * <p>
 * Высота полосы HUD зависит от карты, а не постоянна: если на карте есть
 * точка старта второго игрока, под неё резервируется вторая строка —
 * будущий блок статистики или, пока игрок не подключился, приглашение
 * присоединиться. Карта не меняется в течение партии, поэтому высоту
 * можно посчитать один раз при создании сцены ({@link #hudHeightFor}) —
 * не подгонять раскладку на лету.
 */
public final class GameRenderer {

    /** Высота одного блока статистики игрока — строка с очками/здоровьем плюс строка перезарядки. */
    private static final double PLAYER_BLOCK_HEIGHT = 56.0;

    private static final double TANK_SIZE = Tank.SIZE;
    private static final double BULLET_RADIUS = 3.0;
    private static final double HEALTH_BAR_HEIGHT = 6.0;
    private static final double RELOAD_RING_RADIUS = 7.0;
    private static final double RELOAD_RING_LINE_WIDTH = 3.0;
    private static final String TEXTURE_ROOT = "/ru/rsreu/ineprokin/textures/";

    private final ThemeConfig theme;
    private final Map<PickupType, Sprite> pickupSprites;
    private final Sprite barrelSprite;
    private final Sprite explosionSprite;

    public GameRenderer(ThemeConfig theme) {
        this.theme = theme;
        this.pickupSprites = new EnumMap<>(PickupType.class);
        this.pickupSprites.put(PickupType.MEDKIT, GameRenderer.textureSprite("pickup_medkit.png", false));
        this.pickupSprites.put(PickupType.EXTRA_LIFE, GameRenderer.textureSprite("pickup_extra_life.png", true));
        this.pickupSprites.put(PickupType.RAPID_RELOAD, GameRenderer.textureSprite("pickup_rapid_reload.png", true));
        this.barrelSprite = GameRenderer.textureSprite("barrel.png", false);
        // Взрыв — не готовая текстура, а расширяющееся кольцо, которое рисуется кодом
        // прямо здесь: тот же интерфейс Sprite, другая реализация — TextureSprite
        // рендереру не нужно об этом знать.
        this.explosionSprite = (gc, x, y, size, opacity) -> {
            gc.setGlobalAlpha(opacity);
            gc.setStroke(this.theme.explosionRing());
            gc.setLineWidth(4);
            gc.strokeOval(x, y, size, size);
            gc.setGlobalAlpha(1.0);
        };
    }

    /**
     * Загружает готовую текстуру бонуса/бочки из ресурсов приложения и оборачивает
     * её в {@link Sprite}. Источники и лицензии всех текстур перечислены в README,
     * в разделе «Бонусы и опасности».
     */
    private static Sprite textureSprite(String fileName, boolean pixelArt) {
        String path = GameRenderer.TEXTURE_ROOT + fileName;
        try (InputStream input = GameRenderer.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Текстура не найдена: " + path);
            }
            return new TextureSprite(new Image(input), pixelArt);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить текстуру " + path, e);
        }
    }

    /** Сколько места нужно под HUD: одна строка для одного игрока, две — если на карте есть второй. */
    public static double hudHeightFor(boolean playerTwoAvailable) {
        return playerTwoAvailable ? PLAYER_BLOCK_HEIGHT * 2 : PLAYER_BLOCK_HEIGHT;
    }

    public void render(GraphicsContext gc, GameSnapshot snapshot, double width, double height) {
        double hudHeight = GameRenderer.hudHeightFor(snapshot.playerTwo().available());

        this.drawBackground(gc, width, height, hudHeight);
        this.drawMap(gc, snapshot.map(), hudHeight);
        for (BarrelView barrel : snapshot.barrels()) {
            this.drawBarrel(gc, barrel, hudHeight);
        }
        for (PickupView pickup : snapshot.pickups()) {
            this.drawPickup(gc, pickup, hudHeight);
        }
        for (TankView tank : snapshot.tanks()) {
            this.drawTank(gc, tank, hudHeight);
        }
        for (BulletView bullet : snapshot.bullets()) {
            this.drawBullet(gc, bullet, hudHeight);
        }
        for (ExplosionView explosion : snapshot.explosions()) {
            this.drawExplosion(gc, explosion, hudHeight);
        }
        this.drawHud(gc, snapshot, width, hudHeight);
        this.drawStateOverlay(gc, snapshot, width, height);
        if (snapshot.resultsVisible()) {
            this.drawResultsScreen(gc, snapshot, width, height);
        }
    }

    private void drawBackground(GraphicsContext gc, double width, double height, double hudHeight) {
        gc.setFill(this.theme.hudBackground());
        gc.fillRect(0, 0, width, hudHeight);
        gc.setFill(this.theme.background());
        gc.fillRect(0, hudHeight, width, height - hudHeight);
    }

    private void drawMap(GraphicsContext gc, GameMap map, double hudHeight) {
        gc.setFill(this.theme.wall());
        for (int row = 0; row < map.height(); row++) {
            for (int col = 0; col < map.width(); col++) {
                if (map.isWall(col, row)) {
                    gc.fillRect(col * GameMap.TILE_SIZE, this.mapY(row * GameMap.TILE_SIZE, hudHeight),
                            GameMap.TILE_SIZE, GameMap.TILE_SIZE);
                }
            }
        }
    }

    private void drawTank(GraphicsContext gc, TankView tank, double hudHeight) {
        // Пока танк неуязвим (сразу после появления/возрождения), корпус мигает —
        // видно по настенным часам, а не по состоянию партии, поэтому не нужно
        // тащить время тика через снимок: достаточно самого факта неуязвимости.
        if (tank.invulnerable() && (System.nanoTime() / 120_000_000L) % 2 != 0) {
            return;
        }

        double x = tank.x();
        double y = this.mapY(tank.y(), hudHeight);
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
        // Кольцо перезарядки игрока рисует drawHud в неподвижной полосе HUD,
        // а не здесь, у самого танка на поле боя.
    }

    /** Бочка: стоит на карте как есть, пока в неё не попадёт пуля — тогда взрывается. */
    private void drawBarrel(GraphicsContext gc, BarrelView barrel, double hudHeight) {
        double y = this.mapY(barrel.y(), hudHeight);
        this.barrelSprite.draw(gc, barrel.x(), y, ExplosiveBarrel.SIZE, 1.0);
    }

    /** Бонус: значок на карте зависит от типа — какой именно, определяет {@link PickupView#type()}. */
    private void drawPickup(GraphicsContext gc, PickupView pickup, double hudHeight) {
        double y = this.mapY(pickup.y(), hudHeight);
        this.pickupSprites.get(pickup.type()).draw(gc, pickup.x(), y, Pickup.SIZE, 1.0);
    }

    private Color hullColorFor(PlayerId playerId) {
        if (playerId == null) {
            return this.theme.enemyHull();
        }
        return playerId == PlayerId.PLAYER_TWO ? this.theme.playerTwoHull() : this.theme.playerHull();
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
     * Кольцо перезарядки орудия: тонкий фоновый круг и поверх него дуга,
     * которая заполняет круг по часовой стрелке от 12 часов, пока орудие
     * перезаряжается, и становится полной окружностью, когда оно готово.
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

    private void drawBullet(GraphicsContext gc, BulletView bullet, double hudHeight) {
        gc.setFill(bullet.fromPlayer() ? this.theme.bulletPlayer() : this.theme.bulletEnemy());
        double x = bullet.x();
        double y = this.mapY(bullet.y(), hudHeight);
        gc.fillOval(x - GameRenderer.BULLET_RADIUS, y - GameRenderer.BULLET_RADIUS,
                GameRenderer.BULLET_RADIUS * 2, GameRenderer.BULLET_RADIUS * 2);
    }

    /**
     * Кольцо ударной волны на радиусе поражения бочки: расширяется от центра
     * взрыва до {@link ExplosionView#radius()} и одновременно выцветает —
     * оба эффекта завязаны на {@link ExplosionView#progress()}, а не на
     * отдельный таймер рендерера, поэтому кадр не зависит от FPS.
     */
    private void drawExplosion(GraphicsContext gc, ExplosionView explosion, double hudHeight) {
        double centerX = explosion.x();
        double centerY = this.mapY(explosion.y(), hudHeight);
        double currentRadius = explosion.radius() * explosion.progress();
        double alpha = 1.0 - explosion.progress();
        double diameter = currentRadius * 2;

        this.explosionSprite.draw(gc, centerX - currentRadius, centerY - currentRadius, diameter, alpha);
    }

    private void drawHud(GraphicsContext gc, GameSnapshot snapshot, double width, double hudHeight) {
        this.drawPlayerBlock(gc, "Игрок 1", this.theme.playerHull(), snapshot.playerOne(), 0);

        if (snapshot.playerTwo().available()) {
            if (snapshot.playerTwo().active()) {
                this.drawPlayerBlock(gc, "Игрок 2", this.theme.playerTwoHull(), snapshot.playerTwo(),
                        GameRenderer.PLAYER_BLOCK_HEIGHT);
            } else {
                this.drawJoinHint(gc, width, GameRenderer.PLAYER_BLOCK_HEIGHT);
            }
        }

        gc.setFill(this.theme.hudText());
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(String.format("FPS: %.0f", snapshot.fps()), width - 16, 20);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Одна строка статистики игрока — подпись с именем (цветом его корпуса),
     * очки, здоровье — и строка перезарядки под ней. И это, и {@link #drawJoinHint}
     * занимают ровно {@link #PLAYER_BLOCK_HEIGHT} по вертикали, начиная с {@code blockOffsetY}.
     */
    private void drawPlayerBlock(GraphicsContext gc, String label, Color labelColor, PlayerHudInfo info, double blockOffsetY) {
        double statsY = blockOffsetY + 34;
        double reloadY = blockOffsetY + 50;

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        gc.setFill(labelColor);
        gc.fillText(label, 16, statsY);

        gc.setFill(this.theme.hudText());
        gc.fillText("Очки: " + info.score(), 110, statsY);
        gc.fillText("Жизни: " + info.health() + " / " + info.maxHealth(), 250, statsY);
        if (info.extraLives() > 0) {
            gc.setFill(this.theme.pickupExtraLifeBody());
            gc.fillText("+" + info.extraLives(), 430, statsY);
        }

        gc.setFill(this.theme.hudText());
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 14));
        gc.fillText("Орудие:", 16, reloadY);
        this.drawReloadRing(gc, 78, reloadY - 5, info.reloadProgress());
    }

    /** Приглашение подключиться — вместо блока статистики, пока второй игрок ещё не сыграл ни одной клавиши. */
    private void drawJoinHint(GraphicsContext gc, double width, double blockOffsetY) {
        double y = blockOffsetY + GameRenderer.PLAYER_BLOCK_HEIGHT / 2.0 + 5;

        gc.setFill(this.theme.joinHintText());
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        gc.fillText("Игрок 2: нажмите ↑ ↓ ← → или Enter, чтобы подключиться", width / 2.0, y);
        gc.setTextAlign(TextAlignment.LEFT);
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

        String scoreText = snapshot.playerTwo().active()
                ? "Игрок 1: " + snapshot.playerOne().score() + "    Игрок 2: " + snapshot.playerTwo().score()
                : "Счёт: " + snapshot.playerOne().score();
        this.drawCentered(gc, scoreText, this.theme.hudText(), 28, width, height / 2.0 - 10);

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
    private double mapY(double mapPixelY, double hudHeight) {
        return mapPixelY + hudHeight;
    }
}
