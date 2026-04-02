package org.nia.niamod.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static String readFile(String fileName) {
        try (InputStream is = FileUtils.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (is == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
    }
}
