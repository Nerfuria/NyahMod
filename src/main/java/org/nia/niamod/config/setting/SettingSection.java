package org.nia.niamod.config.setting;

import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.models.config.SettingCategory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record SettingSection(String id, String title, String description, SettingCategory category,
                             Supplier<Boolean> enabledGetter, Consumer<Boolean> enabledSetter,
                             List<ConfigSetting<?>> settings, SectionType type,
                             Supplier<IgnoreFeature> ignoreFeatureSupplier) {

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

    public enum SectionType {
        STANDARD,
        IGNORE_MANAGER
    }
}
