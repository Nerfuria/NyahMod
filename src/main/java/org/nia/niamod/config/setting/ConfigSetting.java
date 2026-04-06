package org.nia.niamod.config.setting;

import lombok.Getter;
import org.nia.niamod.models.config.SettingKind;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public abstract class ConfigSetting<T> {
    private final String id;
    private final String title;
    private final String description;
    private final SettingKind kind;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    protected ConfigSetting(String id, String title, String description, SettingKind kind, Supplier<T> getter, Consumer<T> setter) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.getter = getter;
        this.setter = setter;
    }

    public T get() {
        return getter.get();
    }

    public void set(T value) {
        setter.accept(value);
    }

    public abstract String format();

    public abstract boolean tryParseAndSet(String rawValue);

    public boolean isBoolean() {
        return kind == SettingKind.BOOLEAN;
    }
}
