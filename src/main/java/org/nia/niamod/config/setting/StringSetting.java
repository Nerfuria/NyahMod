package org.nia.niamod.config.setting;

import org.nia.niamod.models.config.SettingKind;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringSetting extends ConfigSetting<String> {
    public StringSetting(String id, String title, String description, Supplier<String> getter, Consumer<String> setter) {
        super(id, title, description, SettingKind.STRING, getter, setter);
    }

    @Override
    public String format() {
        return get();
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        set(rawValue);
        return true;
    }
}
