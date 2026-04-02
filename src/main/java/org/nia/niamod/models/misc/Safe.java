package org.nia.niamod.models.misc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Safe {
    int ordinal() default -1;
}
