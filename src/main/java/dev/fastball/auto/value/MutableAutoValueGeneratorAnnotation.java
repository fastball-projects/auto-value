package dev.fastball.auto.value;


import dev.fastball.auto.value.annotation.AutoValue;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
public class MutableAutoValueGeneratorAnnotation extends AbstractAutoValueGeneratorAnnotation<AutoValue> {

    @Override
    protected String getClassNameSuffix(AutoValue autoValueAnnotation) {
        return autoValueAnnotation.generatedClassSuffix();
    }

    @Override
    protected boolean accessorPrefixed(AutoValue autoValueAnnotation) {
        return autoValueAnnotation.accessorPrefixed();
    }

    @Override
    protected boolean generateBuilder(AutoValue autoValueAnnotation) {
        return autoValueAnnotation.generateBuilder();
    }


    @Override
    protected boolean mutable() {
        return true;
    }
}
