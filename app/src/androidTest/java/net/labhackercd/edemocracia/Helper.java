package net.labhackercd.edemocracia;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class Helper {
    private static Properties properties = null;

    public static String getProperty(String name) throws IOException {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) throws IOException {
        if (properties == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream("test.properties");
            if (stream == null) {
                throw new IOException("Resource `test.properties` doesn't exist");
            }
            try {
                properties = new Properties();
                properties.load(stream);
            } catch (IOException | NullPointerException e) {
                throw new IOException("Failed to load testing properties: " + e.toString());
            }
        }
        return properties.getProperty(name, defaultValue);
    }
}
