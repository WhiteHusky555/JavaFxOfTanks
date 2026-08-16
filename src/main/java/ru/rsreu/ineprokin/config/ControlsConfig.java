package ru.rsreu.ineprokin.config;

import javafx.scene.input.KeyCode;
import ru.rsreu.ineprokin.model.geometry.Direction;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Раскладка клавиш, вычитанная из текстового ресурса {@code controls.properties}.
 * Управление можно поменять, отредактировав этот ресурс, — без пересборки проекта.
 */
public final class ControlsConfig {

    private static final String RESOURCE_PATH = "/ru/rsreu/ineprokin/controls.properties";

    private final Map<Direction, Set<KeyCode>> movementKeys;
    private final Set<KeyCode> fireKeys;
    private final Set<KeyCode> pauseKeys;
    private final Set<KeyCode> backKeys;

    private ControlsConfig(Map<Direction, Set<KeyCode>> movementKeys, Set<KeyCode> fireKeys,
                            Set<KeyCode> pauseKeys, Set<KeyCode> backKeys) {
        this.movementKeys = movementKeys;
        this.fireKeys = fireKeys;
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

            Map<Direction, Set<KeyCode>> movement = new EnumMap<>(Direction.class);
            movement.put(Direction.UP, ControlsConfig.parseKeys(properties, "move.up"));
            movement.put(Direction.DOWN, ControlsConfig.parseKeys(properties, "move.down"));
            movement.put(Direction.LEFT, ControlsConfig.parseKeys(properties, "move.left"));
            movement.put(Direction.RIGHT, ControlsConfig.parseKeys(properties, "move.right"));

            Set<KeyCode> fire = ControlsConfig.parseKeys(properties, "fire");
            Set<KeyCode> pause = ControlsConfig.parseKeys(properties, "pause");
            Set<KeyCode> back = ControlsConfig.parseKeys(properties, "back");

            return new ControlsConfig(movement, fire, pause, back);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать настройки управления", e);
        }
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

    public Optional<Direction> directionFor(KeyCode code) {
        for (Map.Entry<Direction, Set<KeyCode>> entry : this.movementKeys.entrySet()) {
            if (entry.getValue().contains(code)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean isFireKey(KeyCode code) {
        return this.fireKeys.contains(code);
    }

    public boolean isPauseKey(KeyCode code) {
        return this.pauseKeys.contains(code);
    }

    public boolean isBackKey(KeyCode code) {
        return this.backKeys.contains(code);
    }
}
