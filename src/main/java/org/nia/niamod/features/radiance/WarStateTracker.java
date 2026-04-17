package org.nia.niamod.features.radiance;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class WarStateTracker {
    private static final Pattern WAR_START_PATTERN =
            Pattern.compile("The war battle will start in \\d+ seconds\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAR_END_PATTERN =
            Pattern.compile("Your guild has (?:lost|won) the war for .+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAR_DEATH_PATTERN =
            Pattern.compile("You have died at .+", Pattern.CASE_INSENSITIVE);
    private static final String SERVER_CONNECT_MARKER = "Connected to server:";

    private boolean inWar;

    private static String normalizeLine(String line) {
        String normalized = line;
        normalized = normalized.replaceAll("(?i)§[0-9a-fk-or]", "");
        normalized = normalized.replaceAll("(?i)&[0-9a-fk-or]", "");
        normalized = normalized.replaceAll("&\\{[^}]*}", "");
        normalized = normalized.replaceAll("&\\[[^]]*]", "");
        normalized = normalized.replaceAll("&<[^>]*>", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public void reset() {
        inWar = false;
    }

    public void handleMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        for (String line : message.split("\\n")) {
            handleLine(line);
        }
    }

    private void handleLine(String line) {
        String normalized = normalizeLine(line);
        if (normalized.isBlank()) {
            return;
        }
        if (WAR_START_PATTERN.matcher(normalized).find()) {
            inWar = true;
        } else if (WAR_END_PATTERN.matcher(normalized).find()
                || WAR_DEATH_PATTERN.matcher(normalized).find()
                || normalized.contains(SERVER_CONNECT_MARKER)) {
            inWar = false;
        }
    }
}
