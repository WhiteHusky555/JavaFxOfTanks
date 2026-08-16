package ru.rsreu.ineprokin.model.map;

/**
 * Карта повреждена или не может быть прочитана. В отличие от исходной
 * C++-версии, которая на любую ошибку разбора тихо возвращала {@code false}
 * и оставляла карту в неопределённом состоянии, здесь ошибка явно долетает
 * до вызывающего кода вместе с описанием причины.
 */
public class MapLoadException extends RuntimeException {

    public MapLoadException(String message) {
        super(message);
    }

    public MapLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
