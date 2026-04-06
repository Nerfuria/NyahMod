package org.nia.niamod.models.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettingCategory {
    GENERAL("General"),
    WAR("War"),
    SOCIAL("Social");

    private final String title;
}
