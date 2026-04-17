package org.nia.niamod.models.gui.animation;

import java.util.function.Function;

public enum Easing implements Function<Double, Double> {
    LINEAR(t -> t),
    EASE_OUT_EXPO(t -> t >= 1 ? 1 : 1 - Math.pow(2, -10 * t)),
    EASE_OUT_CUBIC(t -> {
        double f = t - 1;
        return f * f * f + 1;
    }),
    EASE_OUT_QUINT(t -> {
        double f = t - 1;
        return f * f * f * f * f + 1;
    });

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    @Override
    public Double apply(Double t) {
        return function.apply(t);
    }
}
