package org.icpclive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    public static void setConfigDirectory(String configDirectory) {
        Config.configDirectory = configDirectory;
    }

    private static String configDirectory = "config";

    public static Properties loadProperties(String name) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(configDirectory + File.separator + name + ".properties"));
        return properties;
    }
}
