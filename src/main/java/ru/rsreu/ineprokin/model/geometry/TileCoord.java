package ru.rsreu.ineprokin.model.geometry;

/** Координата клетки карты в тайлах (не в пикселях) — колонка и строка. */
public record TileCoord(int col, int row) {

    /** Верхний левый угол объекта размера {@code objectSize}, центрированного в клетке. */
    public Position toPixelCenter(double tileSize, double objectSize) {
        double offset = (tileSize - objectSize) / 2.0;
        return new Position(col * tileSize + offset, row * tileSize + offset);
    }
}
