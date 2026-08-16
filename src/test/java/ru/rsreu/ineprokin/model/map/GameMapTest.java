package ru.rsreu.ineprokin.model.map;

import org.junit.jupiter.api.Test;
import ru.rsreu.ineprokin.model.PlayerId;
import ru.rsreu.ineprokin.model.geometry.TileCoord;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapTest {

    private static InputStream textOf(String... lines) {
        String content = String.join("\n", lines);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesWallsAndSpawnPoints() {
        GameMap map = GameMap.load(GameMapTest.textOf(
                "#####",
                "#P..#",
                "#...#",
                "#..E#",
                "#####"
        ));

        assertEquals(5, map.width());
        assertEquals(5, map.height());
        assertTrue(map.isWall(0, 0));
        assertFalse(map.isWall(1, 1));
        assertEquals(new TileCoord(1, 1), map.playerStarts().get(PlayerId.PLAYER_ONE));
        assertEquals(1, map.enemyStarts().size());
        assertEquals(new TileCoord(3, 3), map.enemyStarts().get(0));
    }

    @Test
    void parsesSecondPlayerMarker() {
        GameMap map = GameMap.load(GameMapTest.textOf(
                "#####",
                "#P.2#",
                "#####"
        ));

        assertEquals(new TileCoord(1, 1), map.playerStarts().get(PlayerId.PLAYER_ONE));
        assertEquals(new TileCoord(3, 1), map.playerStarts().get(PlayerId.PLAYER_TWO));
    }

    @Test
    void outOfBoundsTileIsTreatedAsWall() {
        GameMap map = GameMap.load(GameMapTest.textOf("#P#"));

        assertTrue(map.isWall(-1, 0));
        assertTrue(map.isWall(99, 0));
        assertTrue(map.isWall(0, -1));
    }

    @Test
    void rejectsRaggedRows() {
        InputStream input = GameMapTest.textOf("####", "#P.#", "##");

        assertThrows(MapLoadException.class, () -> GameMap.load(input));
    }

    @Test
    void rejectsMapWithoutPlayerStart() {
        InputStream input = GameMapTest.textOf("####", "#..#", "####");

        assertThrows(MapLoadException.class, () -> GameMap.load(input));
    }
}
