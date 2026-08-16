package ru.rsreu.ineprokin.model.entity;

import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.capability.BulletSpawnRequest;
import ru.rsreu.ineprokin.model.capability.Damageable;
import ru.rsreu.ineprokin.model.capability.Fireable;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.util.Optional;

/**
 * Танк — игрока или противника. Модель хранит только собственное состояние
 * (здоровье, перезарядку, курс) и не знает ни о карте, ни о других танках:
 * перемещение по полю — забота {@code engine.CollisionService} /
 * {@code engine.GameWorld}, которые вызывают {@link #setPosition(double, double)}
 * только после проверки, что клетка свободна.
 * <p>
 * Танк не привязан к четырём сторонам света — он поворачивается на
 * произвольный угол ({@link #rotate}) и едет вперёд/назад вдоль текущего
 * курса ({@link #getHeadingDegrees()}). Курс отсчитывается в градусах по
 * часовой стрелке от направления "вверх": 0° — вверх, 90° — вправо,
 * 180° — вниз, 270° — влево.
 * <p>
 * Принадлежность танка игроку выражена не булевым флагом, а {@code null}-able
 * {@link PlayerId}: {@code null} — танк управляется ИИ, конкретное значение —
 * танк принадлежит соответствующему игроку. Это и есть та самая точка
 * расширения под второго локального игрока — {@link PlayerId#PLAYER_TWO}
 * уже существует, просто пока никто не создаёт танк с этим значением.
 */
public final class Tank extends GameObject implements Damageable, Fireable {

    public static final int MAX_HEALTH = 100;
    /** Чуть меньше клетки карты ({@code GameMap.TILE_SIZE}), чтобы корпус не задевал стены углом при повороте. */
    public static final double SIZE = 30.0;

    public static final double PLAYER_SPEED_PX_PER_SEC = 220.0;
    public static final double ENEMY_SPEED_PX_PER_SEC = 150.0;

    public static final double ROTATION_SPEED_DEG_PER_SEC = 210.0;

    public static final double PLAYER_RELOAD_SECONDS = 0.8;
    public static final double ENEMY_RELOAD_SECONDS = 1.5;

    /** Насколько дульный срез вынесен вперёд от центра танка при рождении пули. */
    private static final double MUZZLE_OFFSET = SIZE / 2.0 + 1.0;

    private final PlayerId playerId;
    private final double speed;
    private final double reloadSeconds;

    private double headingDegrees;
    private int health;
    private double timeSinceLastShot;

    /**
     * @param headingDegrees курс в градусах по часовой стрелке от направления "вверх"
     * @param playerId       {@code null} для танка под управлением ИИ, иначе — владелец-игрок
     */
    public Tank(double startX, double startY, double headingDegrees, PlayerId playerId) {
        super(startX, startY);
        this.headingDegrees = Tank.normalizeDegrees(headingDegrees);
        this.playerId = playerId;
        this.health = MAX_HEALTH;
        this.speed = playerId != null ? PLAYER_SPEED_PX_PER_SEC : ENEMY_SPEED_PX_PER_SEC;
        this.reloadSeconds = playerId != null ? PLAYER_RELOAD_SECONDS : ENEMY_RELOAD_SECONDS;
        // Танк готов выстрелить сразу после появления на поле.
        this.timeSinceLastShot = this.reloadSeconds;
    }

    /** Удобный фабричный метод для читаемости в местах, создающих вражеские танки. */
    public static Tank enemy(double startX, double startY, double headingDegrees) {
        return new Tank(startX, startY, headingDegrees, null);
    }

    @Override
    public void update(double deltaTimeSeconds) {
        if (this.timeSinceLastShot < this.reloadSeconds) {
            this.timeSinceLastShot += deltaTimeSeconds;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.health <= 0;
    }

    public boolean canFire() {
        return this.timeSinceLastShot >= this.reloadSeconds;
    }

    /** Доля перезарядки орудия: {@code 0.0} сразу после выстрела, {@code 1.0} — орудие готово. */
    public double getReloadProgress() {
        return Math.min(1.0, this.timeSinceLastShot / this.reloadSeconds);
    }

    @Override
    public Optional<BulletSpawnRequest> tryFire() {
        if (!this.canFire()) {
            return Optional.empty();
        }
        this.timeSinceLastShot = 0.0;

        double centerX = this.getX() + SIZE / 2.0;
        double centerY = this.getY() + SIZE / 2.0;
        double bulletX = centerX + this.forwardX() * MUZZLE_OFFSET;
        double bulletY = centerY + this.forwardY() * MUZZLE_OFFSET;

        return Optional.of(new BulletSpawnRequest(bulletX, bulletY, this.headingDegrees, this.playerId));
    }

    @Override
    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    /**
     * Поворачивает танк на месте на скорость {@link #ROTATION_SPEED_DEG_PER_SEC}.
     *
     * @param turnDirection знак задаёт сторону поворота: отрицательный — против часовой (влево),
     *                      положительный — по часовой (вправо); величина не важна, учитывается только знак
     */
    public void rotate(double turnDirection, double deltaTimeSeconds) {
        double delta = Math.signum(turnDirection) * ROTATION_SPEED_DEG_PER_SEC * deltaTimeSeconds;
        this.headingDegrees = Tank.normalizeDegrees(this.headingDegrees + delta);
    }

    /** Мгновенно разворачивает танк на один из четырёх кардинальных курсов — например, при появлении на поле. */
    public void faceDirection(Direction direction) {
        this.headingDegrees = direction.headingDegrees();
    }

    /**
     * Плавно доворачивает танк к целевому курсу кратчайшей стороной — не быстрее
     * {@link #ROTATION_SPEED_DEG_PER_SEC}. Если до цели остаётся меньше одного
     * шага, довершает поворот точно на неё, а не проскакивает мимо. Так рулит
     * и игрок (кадр за кадром, пока зажата клавиша), и вражеский ИИ, выбравший
     * себе новое направление.
     */
    public void rotateTowards(double targetHeadingDegrees, double deltaTimeSeconds) {
        double target = Tank.normalizeDegrees(targetHeadingDegrees);
        double delta = Tank.normalizeDegrees(target - this.headingDegrees + 180.0) - 180.0; // в диапазон (-180, 180]
        double maxStep = ROTATION_SPEED_DEG_PER_SEC * deltaTimeSeconds;

        if (Math.abs(delta) <= maxStep) {
            this.headingDegrees = target;
        } else {
            this.headingDegrees = Tank.normalizeDegrees(this.headingDegrees + Math.copySign(maxStep, delta));
        }
    }

    public double getHeadingDegrees() {
        return this.headingDegrees;
    }

    /** X-компонента единичного вектора "вперёд" при текущем курсе. */
    public double forwardX() {
        return Math.sin(Math.toRadians(this.headingDegrees));
    }

    /** Y-компонента единичного вектора "вперёд" при текущем курсе (ось Y направлена вниз). */
    public double forwardY() {
        return -Math.cos(Math.toRadians(this.headingDegrees));
    }

    private static double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    /** {@code true}, если танк принадлежит любому из игроков (не ИИ). */
    public boolean isPlayer() {
        return this.playerId != null;
    }

    public Optional<PlayerId> getPlayerId() {
        return Optional.ofNullable(this.playerId);
    }

    public int getHealth() {
        return this.health;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public double getSpeed() {
        return this.speed;
    }
}
