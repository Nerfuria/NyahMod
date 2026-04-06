package org.nia.niamod.config.setting;

import lombok.Getter;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
public class ChoiceSetting extends ConfigSetting<String> {
    private final List<String> options;
    private final Function<String, String> labelResolver;

    public ChoiceSetting(
            String id,
            String title,
            String description,
            Supplier<String> getter,
            java.util.function.Consumer<String> setter,
            List<String> options,
            Function<String, String> labelResolver
    ) {
        super(id, title, description, SettingKind.CHOICE, getter, setter);
        this.options = List.copyOf(options);
        this.labelResolver = labelResolver;
    }

    @Override
    public String format() {
        return displayValue(get());
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        String resolved = resolveOption(rawValue);
        if (resolved == null) {
            return false;
        }

        set(resolved);
        return true;
    }

    public void next() {
        cycle(1);
    }

    public void previous() {
        cycle(-1);
    }

    public String displayValue(String value) {
        return labelResolver.apply(value);
    }

    private void cycle(int direction) {
        if (options.isEmpty()) {
            return;
        }

        int currentIndex = options.indexOf(resolveOption(get()));
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        int nextIndex = Math.floorMod(currentIndex + direction, options.size());
        set(options.get(nextIndex));
    }

    private String resolveOption(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return options.isEmpty() ? null : options.getFirst();
        }

        String normalized = normalize(rawValue);
        for (String option : options) {
            if (normalize(option).equals(normalized) || normalize(displayValue(option)).equals(normalized)) {
                return option;
            }
        }

        return null;
    }

    private String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
