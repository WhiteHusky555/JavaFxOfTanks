package ru.rsreu.ineprokin.model.entity;

/**
 * Фаза игрового раунда. Экран главного меню в эту машину состояний не входит —
 * переключением экранов занимается пакет {@code navigation}, а не партия игры.
 */
public enum GameState {
    PLAYING,
    PAUSED,
    GAME_OVER
}
