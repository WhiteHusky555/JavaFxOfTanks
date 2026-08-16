package ru.rsreu.ineprokin.engine;

import ru.rsreu.ineprokin.engine.ai.AiStrategy;
import ru.rsreu.ineprokin.model.entity.GameState;
import ru.rsreu.ineprokin.model.entity.Tank;
import ru.rsreu.ineprokin.model.map.GameMap;

import java.util.List;

/**
 * Доступное только для чтения представление партии. {@link AiStrategy}
 * получает именно этот интерфейс, а не {@link GameWorld} целиком — стратегия
 * ИИ физически не может изменить состояние мира, только прочитать его.
 */
public interface GameWorldView {

    GameMap getMap();

    List<Tank> getTanks();

    GameState getState();

    int getScore();
}
