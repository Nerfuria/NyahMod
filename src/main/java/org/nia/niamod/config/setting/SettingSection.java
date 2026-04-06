package org.nia.niamod.config.setting;

import lombok.Getter;
import org.nia.niamod.features.IgnoreFeature;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class SettingSection {
    public enum SectionType {
        STANDARD,
        IGNORE_MANAGER
    }

    private final String id;
    private final String title;
    private final String description;
    private final SettingCategory category;
    private final Supplier<Boolean> enabledGetter;
    private final Consumer<Boolean> enabledSetter;
    private final List<ConfigSetting<?>> settings;
    private final SectionType type;
    private final Supplier<IgnoreFeature> ignoreFeatureSupplier;

    public SettingSection(
            String id,
            String title,
            String description,
            SettingCategory category,
            Supplier<Boolean> enabledGetter,
            Consumer<Boolean> enabledSetter,
            List<ConfigSetting<?>> settings,
            SectionType type,
            Supplier<IgnoreFeature> ignoreFeatureSupplier
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.enabledGetter = enabledGetter;
        this.enabledSetter = enabledSetter;
        this.settings = settings;
        this.type = type;
        this.ignoreFeatureSupplier = ignoreFeatureSupplier;
    }

    public static SettingSection standard(
            String id,
            String title,
            String description,
            SettingCategory category,
            Supplier<Boolean> enabledGetter,
            Consumer<Boolean> enabledSetter,
            List<ConfigSetting<?>> settings
    ) {
        return new SettingSection(id, title, description, category, enabledGetter, enabledSetter, settings, SectionType.STANDARD, null);
    }

    public static SettingSection ignoreManager(
            String id,
            String title,
            String description,
            SettingCategory category,
            Supplier<Boolean> enabledGetter,
            Consumer<Boolean> enabledSetter,
            Supplier<IgnoreFeature> ignoreFeatureSupplier
    ) {
        return new SettingSection(id, title, description, category, enabledGetter, enabledSetter, List.of(), SectionType.IGNORE_MANAGER, ignoreFeatureSupplier);
    }

    public boolean hasToggle() {
        return enabledGetter != null && enabledSetter != null;
    }

    public boolean isEnabled() {
        return enabledGetter != null && Boolean.TRUE.equals(enabledGetter.get());
    }

    public void setEnabled(boolean enabled) {
        if (enabledSetter != null) {
            enabledSetter.accept(enabled);
        }
    }

    public IgnoreFeature getIgnoreFeature() {
        return ignoreFeatureSupplier == null ? null : ignoreFeatureSupplier.get();
    }
}
