package ru.rsreu.ineprokin.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * {@link Sprite}, нарисованный готовой растровой текстурой, а не кодом.
 * Текстуры низкого разрешения (пиксель-арт) при масштабировании смазываются
 * сглаживанием Canvas — для них {@code pixelArt} отключает сглаживание на
 * время отрисовки, чтобы пиксели остались чёткими, а не размывались в кашу.
 */
final class TextureSprite implements Sprite {

    private final Image image;
    private final boolean pixelArt;

    TextureSprite(Image image, boolean pixelArt) {
        this.image = image;
        this.pixelArt = pixelArt;
    }

    @Override
    public void draw(GraphicsContext gc, double x, double y, double size, double opacity) {
        gc.setGlobalAlpha(opacity);
        if (this.pixelArt) {
            gc.setImageSmoothing(false);
        }
        gc.drawImage(this.image, x, y, size, size);
        if (this.pixelArt) {
            gc.setImageSmoothing(true);
        }
        gc.setGlobalAlpha(1.0);
    }
}
