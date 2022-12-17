package dev.fastball.auto.value;

import dev.fastball.auto.value.annotation.ImmutableAutoValue;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
public class ImmutableAutoValueGeneratorAnnotation extends AbstractAutoValueGeneratorAnnotation<ImmutableAutoValue> {

    @Override
    protected String getClassNameSuffix(ImmutableAutoValue autoValueAnnotation) {
        return autoValueAnnotation.generatedClassSuffix();
    }

    @Override
    protected boolean accessorPrefixed(ImmutableAutoValue autoValueAnnotation) {
        return autoValueAnnotation.accessorPrefixed();
    }

    @Override
    protected boolean generateBuilder(ImmutableAutoValue autoValueAnnotation) {
        return autoValueAnnotation.generateBuilder();
    }

    @Override
    protected boolean mutable() {
        return false;
    }
}
