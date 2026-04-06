package org.nia.niamod.models.gui.clickgui.theme;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Locale;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ClickGuiFontOption {
    MINECRAFT_UNIFORM("minecraft_uniform", "Minecraft Uniform", "minecraft", "uniform"),
    MINECRAFT_DEFAULT("minecraft_default", "Minecraft Default", "minecraft", "default"),
    COMFORTAA("comfortaa", "Comfortaa", "niamod", "comfortaa"),
    ADWAITA_SANS("adwaita_sans", "Adwaita Sans", "niamod", "adwaita_sans"),
    LIBERATION_SANS("liberation_sans", "Liberation Sans", "niamod", "liberation_sans"),
    ADWAITA_MONO("adwaita_mono", "Adwaita Mono", "niamod", "adwaita_mono"),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "niamod", "jetbrains_mono");

    private final String key;
    private final String label;
    private final String namespace;
    private final String fontId;

    public static ClickGuiFontOption resolve(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MINECRAFT_UNIFORM;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        return Arrays.stream(values())
                .filter(option -> option.key.equals(normalized))
                .findFirst()
                .orElse(MINECRAFT_UNIFORM);
    }

    public Identifier fontDescriptionId() {
        return Identifier.fromNamespaceAndPath(namespace, fontId);
    }

    public static List<String> keys() {
        return Arrays.stream(values())
                .map(ClickGuiFontOption::getKey)
                .toList();
    }

    public static String labelFor(String key) {
        return resolve(key).getLabel();
    }
}
