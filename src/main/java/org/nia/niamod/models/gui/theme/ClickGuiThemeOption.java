package org.nia.niamod.models.gui.theme;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ClickGuiThemeOption {
    DEFAULT("default", "Default Dark", ClickGuiTheme.defaultTheme()),
    OCEAN("ocean", "Ocean", ClickGuiTheme.builder()
            .background(0xFF0F172A)
            .secondary(0xFF1E293B)
            .textColor(0xFFF8FAFC)
            .secondaryText(0xFFCBD5E1)
            .trinaryText(0xFF94A3B8)
            .overlay(0x26000000)
            .accentColor(0xFF0EA5E9)
            .shadowColor(0x18000000)
            .sliderTrack(0xFF0F172A)
            .scrollbarColor(0x30FFFFFF)
            .build()),
    CHERRY("cherry", "Cherry Blossom", ClickGuiTheme.builder()
            .background(0xFF2D1B2E)
            .secondary(0xFF3D263F)
            .textColor(0xFFFDF2F8)
            .secondaryText(0xFFFBCFE8)
            .trinaryText(0xFFF472B6)
            .overlay(0x26000000)
            .accentColor(0xFFEC4899)
            .shadowColor(0x18000000)
            .sliderTrack(0xFF2D1B2E)
            .scrollbarColor(0x30FFFFFF)
            .build()),
    CUSTOM("custom", "Custom Colors", ClickGuiTheme.defaultTheme());

    private final String key;
    private final String label;
    private final ClickGuiTheme theme;

    public static ClickGuiThemeOption resolve(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return DEFAULT;
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return Arrays.stream(values()).filter(o -> o.key.equals(normalized)).findFirst().orElse(DEFAULT);
    }
}