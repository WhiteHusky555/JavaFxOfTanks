package ru.rsreu.ineprokin.config;

import javafx.scene.input.KeyCode;
import ru.rsreu.ineprokin.model.PlayerId;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Раскладки клавиш обоих игроков, вычитанные из текстового ресурса
 * {@code controls.properties}: у {@link PlayerId#PLAYER_ONE} — свой набор
 * клавиш (по умолчанию WASD), у {@link PlayerId#PLAYER_TWO} — свой (стрелки).
 * Управление можно поменять, отредактировав этот ресурс, — без пересборки проекта.
 */
public final class ControlsConfig {

    private static final String RESOURCE_PATH = "/ru/rsreu/ineprokin/controls.properties";

    private final Map<PlayerId, PlayerControlScheme> playerSchemes;
    private final Set<KeyCode> pauseKeys;
    private final Set<KeyCode> backKeys;

    private ControlsConfig(Map<PlayerId, PlayerControlScheme> playerSchemes, Set<KeyCode> pauseKeys, Set<KeyCode> backKeys) {
        this.playerSchemes = playerSchemes;
        this.pauseKeys = pauseKeys;
        this.backKeys = backKeys;
    }

    /** Загружает раскладку из ресурса, упакованного вместе с приложением. */
    public static ControlsConfig loadDefault() {
        try (InputStream input = ControlsConfig.class.getResourceAsStream(ControlsConfig.RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Файл настроек управления не найден: " + ControlsConfig.RESOURCE_PATH);
            }
            Properties properties = new Properties();
            properties.load(input);

            Map<PlayerId, PlayerControlScheme> schemes = new EnumMap<>(PlayerId.class);
            schemes.put(PlayerId.PLAYER_ONE, ControlsConfig.loadScheme(properties, "p1"));
            schemes.put(PlayerId.PLAYER_TWO, ControlsConfig.loadScheme(properties, "p2"));

            Set<KeyCode> pause = ControlsConfig.parseKeys(properties, "pause");
            Set<KeyCode> back = ControlsConfig.parseKeys(properties, "back");

            return new ControlsConfig(schemes, pause, back);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать настройки управления", e);
        }
    }

    private static PlayerControlScheme loadScheme(Properties properties, String keyPrefix) {
        Map<SteeringInput, Set<KeyCode>> steering = new EnumMap<>(SteeringInput.class);
        steering.put(SteeringInput.MOVE_FORWARD, ControlsConfig.parseKeys(properties, keyPrefix + ".move.forward"));
        steering.put(SteeringInput.MOVE_BACKWARD, ControlsConfig.parseKeys(properties, keyPrefix + ".move.backward"));
        steering.put(SteeringInput.TURN_LEFT, ControlsConfig.parseKeys(properties, keyPrefix + ".turn.left"));
        steering.put(SteeringInput.TURN_RIGHT, ControlsConfig.parseKeys(properties, keyPrefix + ".turn.right"));

        Set<KeyCode> fire = ControlsConfig.parseKeys(properties, keyPrefix + ".fire");

        return new PlayerControlScheme(steering, fire);
    }

    private static Set<KeyCode> parseKeys(Properties properties, String propertyKey) {
        String raw = properties.getProperty(propertyKey, "");
        Set<KeyCode> codes = EnumSet.noneOf(KeyCode.class);
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                codes.add(KeyCode.valueOf(trimmed));
            }
        }
        return codes;
    }

    public Optional<SteeringInput> steeringInputFor(PlayerId playerId, KeyCode code) {
        return this.playerSchemes.get(playerId).steeringInputFor(code);
    }

    public boolean isFireKey(PlayerId playerId, KeyCode code) {
        return this.playerSchemes.get(playerId).isFireKey(code);
    }

    public boolean isPauseKey(KeyCode code) {
        return this.pauseKeys.contains(code);
    }

    public boolean isBackKey(KeyCode code) {
        return this.backKeys.contains(code);
    }
}
