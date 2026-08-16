package ru.rsreu.ineprokin.model.map;

import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.entity.PickupType;
import ru.rsreu.ineprokin.model.geometry.TileCoord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Неизменяемая карта уровня: сетка тайлов, точки появления танков, бонусов
 * и бочек. Стены на карте никогда не разрушаются в процессе игры, поэтому
 * карта — простой value-объект: она либо успешно построена и корректна,
 * либо не существует вовсе, без методов на изменение состояния "про запас".
 * <p>
 * Точек старта игрока может быть несколько — {@code playerStarts} хранит их
 * по {@link PlayerId}, а не единственную пару координат: карта задаёт
 * {@link PlayerId#PLAYER_ONE} обязательно (маркер {@code P}) и, опционально,
 * {@link PlayerId#PLAYER_TWO} (маркер {@code 2}) — второй игрок подключается
 * к партии позже, но точка для его появления уже должна быть на карте.
 */
public record GameMap(
        List<List<TileType>> rows,
        Map<PlayerId, TileCoord> playerStarts,
        List<TileCoord> enemyStarts,
        Map<PickupType, List<TileCoord>> pickupStarts,
        List<TileCoord> barrelStarts
) {

    public static final double TILE_SIZE = 40.0;

    public GameMap {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Карта не может быть пустой");
        }
        if (playerStarts == null || !playerStarts.containsKey(PlayerId.PLAYER_ONE)) {
            throw new IllegalArgumentException("На карте должна быть стартовая позиция игрока 1");
        }
        rows = rows.stream().map(List::copyOf).toList();
        playerStarts = Map.copyOf(playerStarts);
        enemyStarts = List.copyOf(enemyStarts);
        pickupStarts = pickupStarts.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        barrelStarts = List.copyOf(barrelStarts);
    }

    /**
     * Разбирает карту из текстового ресурса: {@code #} — стена, {@code P} — старт
     * игрока 1 (первый найденный), {@code 2} — старт игрока 2 (первый найденный),
     * {@code E} — старт врага, {@code M} — аптечка, {@code L} — дополнительная
     * жизнь, {@code R} — ускоренная перезарядка, {@code B} — взрывная бочка,
     * всё остальное — пол.
     *
     * @throws MapLoadException если поток нельзя прочитать, карта пуста, строки
     *                          не одинаковой длины или на карте нет позиции игрока 1
     */
    public static GameMap load(InputStream input) {
        List<List<TileType>> rows = new ArrayList<>();
        List<TileCoord> enemyStarts = new ArrayList<>();
        Map<PlayerId, TileCoord> playerStarts = new EnumMap<>(PlayerId.class);
        Map<PickupType, List<TileCoord>> pickupStarts = new EnumMap<>(PickupType.class);
        List<TileCoord> barrelStarts = new ArrayList<>();
        int width = -1;
        int rowIndex = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue; // допускаем завершающие пустые строки в конце файла
                }
                if (width == -1) {
                    width = line.length();
                } else if (line.length() != width) {
                    throw new MapLoadException(
                            "Строка %d имеет длину %d, ожидалась длина первой строки %d"
                                    .formatted(rowIndex, line.length(), width));
                }

                List<TileType> row = new ArrayList<>(width);
                for (int col = 0; col < line.length(); col++) {
                    TileCoord coord = new TileCoord(col, rowIndex);
                    switch (line.charAt(col)) {
                        case '#' -> row.add(TileType.WALL);
                        case 'P' -> {
                            row.add(TileType.EMPTY);
                            playerStarts.putIfAbsent(PlayerId.PLAYER_ONE, coord);
                        }
                        case '2' -> {
                            row.add(TileType.EMPTY);
                            playerStarts.putIfAbsent(PlayerId.PLAYER_TWO, coord);
                        }
                        case 'E' -> {
                            row.add(TileType.EMPTY);
                            enemyStarts.add(coord);
                        }
                        case 'M' -> {
                            row.add(TileType.EMPTY);
                            pickupStarts.computeIfAbsent(PickupType.MEDKIT, ignored -> new ArrayList<>()).add(coord);
                        }
                        case 'L' -> {
                            row.add(TileType.EMPTY);
                            pickupStarts.computeIfAbsent(PickupType.EXTRA_LIFE, ignored -> new ArrayList<>()).add(coord);
                        }
                        case 'R' -> {
                            row.add(TileType.EMPTY);
                            pickupStarts.computeIfAbsent(PickupType.RAPID_RELOAD, ignored -> new ArrayList<>()).add(coord);
                        }
                        case 'B' -> {
                            row.add(TileType.EMPTY);
                            barrelStarts.add(coord);
                        }
                        default -> row.add(TileType.EMPTY);
                    }
                }
                rows.add(row);
                rowIndex++;
            }
        } catch (IOException e) {
            throw new MapLoadException("Не удалось прочитать карту", e);
        }

        if (rows.isEmpty()) {
            throw new MapLoadException("Карта пуста");
        }
        if (!playerStarts.containsKey(PlayerId.PLAYER_ONE)) {
            throw new MapLoadException("На карте не найдена стартовая позиция игрока 1 ('P')");
        }
        return new GameMap(rows, playerStarts, enemyStarts, pickupStarts, barrelStarts);
    }

    public int width() {
        return this.rows.get(0).size();
    }

    public int height() {
        return this.rows.size();
    }

    /** За пределами карты считается стеной — это безопасное значение по умолчанию для проверок коллизий. */
    public TileType tileAt(int col, int row) {
        if (row < 0 || row >= this.rows.size()) {
            return TileType.WALL;
        }
        List<TileType> line = this.rows.get(row);
        if (col < 0 || col >= line.size()) {
            return TileType.WALL;
        }
        return line.get(col);
    }

    public boolean isWall(int col, int row) {
        return this.tileAt(col, row) == TileType.WALL;
    }

    public double widthInPixels() {
        return this.width() * GameMap.TILE_SIZE;
    }

    public double heightInPixels() {
        return this.height() * GameMap.TILE_SIZE;
    }

    /** Точки появления бонусов конкретного типа — пустой список, если на карте таких нет. */
    public List<TileCoord> pickupStarts(PickupType type) {
        return this.pickupStarts.getOrDefault(type, List.of());
    }
}
