package ru.rsreu.ineprokin.model.map;

/**
 * Карта повреждена или не может быть прочитана. Бросается вместо того, чтобы
 * молча вернуть какое-то значение по умолчанию — вызывающий код узнаёт
 * о проблеме сразу и получает описание её причины.
 */
public class MapLoadException extends RuntimeException {

    public MapLoadException(String message) {
        super(message);
    }

    public MapLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
