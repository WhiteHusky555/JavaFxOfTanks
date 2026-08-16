package ru.rsreu.ineprokin.config;

import ru.rsreu.ineprokin.model.AboutInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Загружает содержимое экрана "О программе" из текстового ресурса {@code about.properties}. */
public final class AboutContent {

    private static final String RESOURCE_PATH = "/ru/rsreu/ineprokin/about.properties";

    private AboutContent() {
    }

    public static AboutInfo load() {
        try (InputStream input = AboutContent.class.getResourceAsStream(AboutContent.RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Файл сведений о программе не найден: " + AboutContent.RESOURCE_PATH);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));

            return new AboutInfo(
                    properties.getProperty("app.name", ""),
                    properties.getProperty("version", ""),
                    properties.getProperty("author", ""),
                    properties.getProperty("controls", ""),
                    properties.getProperty("objective", "")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать сведения о программе", e);
        }
    }
}
