package ru.rsreu.ineprokin.config;

import javafx.scene.input.KeyCode;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Раскладка клавиш одного игрока: руление и выстрел. Деталь реализации
 * {@link ControlsConfig} — наружу не выставляется.
 */
record PlayerControlScheme(Map<SteeringInput, Set<KeyCode>> steeringKeys, Set<KeyCode> fireKeys) {

    boolean isFireKey(KeyCode code) {
        return this.fireKeys.contains(code);
    }

    Optional<SteeringInput> steeringInputFor(KeyCode code) {
        for (Map.Entry<SteeringInput, Set<KeyCode>> entry : this.steeringKeys.entrySet()) {
            if (entry.getValue().contains(code)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
}
