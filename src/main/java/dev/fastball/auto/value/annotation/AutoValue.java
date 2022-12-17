package dev.fastball.auto.value.annotation;

import java.lang.annotation.*;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoValue {

    boolean accessorPrefixed() default false;

    boolean generateBuilder() default true;

    String generatedClassSuffix() default "_AutoValue";
}
