package ru.rsreu.ineprokin.model.capability;

/**
 * Способность объекта быть помеченным как уничтоженный и подлежащим удалению
 * из мира. Используется движком единообразно для танков и пуль, например
 * {@code list.removeIf(Destructible::isDestroyed)}.
 */
@FunctionalInterface
public interface Destructible {

    boolean isDestroyed();
}
