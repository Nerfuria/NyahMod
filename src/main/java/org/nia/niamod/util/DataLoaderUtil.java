package org.nia.niamod.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class DataLoaderUtil {
    public static InputStream openStream(String file) {
        InputStream stream = DataLoaderUtil.class
                .getResourceAsStream("/assets/niamod/data/" + file);

        if (stream == null) {
            throw new RuntimeException("Missing data file: " + file);
        }

        return stream;
    }

    public static InputStreamReader openReader(String file) {
        return new InputStreamReader(openStream(file), StandardCharsets.UTF_8);
    }
}