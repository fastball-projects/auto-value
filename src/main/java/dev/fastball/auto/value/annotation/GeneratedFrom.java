package dev.fastball.auto.value.annotation;

import java.lang.annotation.*;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedFrom {
    Class<?> value();

    String generatorClass() default "";

    String date() default "";
}
