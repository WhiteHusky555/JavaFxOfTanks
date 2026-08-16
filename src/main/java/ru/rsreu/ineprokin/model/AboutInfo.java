package ru.rsreu.ineprokin.model;

/**
 * Статичные сведения, показываемые на экране "О программе". Само содержимое
 * не зашито в код — его читает {@code config.AboutContent} из текстового
 * ресурса {@code about.properties}, эта запись лишь переносит уже
 * распарсенные данные в слой представления.
 */
public record AboutInfo(String appName, String version, String author, String controls, String objective) {
}
