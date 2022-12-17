package dev.fastball.auto.value;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.squareup.javapoet.*;
import lombok.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.*;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
public abstract class AbstractAutoValueGeneratorAnnotation<T extends Annotation> extends AnnotationJavaFileGenerator<T> {

    public static final String GET_PREFIX = "get";
    public static final String SET_PREFIX = "set";

    protected abstract String getClassNameSuffix(T autoValueAnnotation);

    protected abstract boolean accessorPrefixed(T autoValueAnnotation);

    protected abstract boolean generateBuilder(T autoValueAnnotation);

    protected abstract boolean mutable();

    @Override
    protected boolean needProcess(TypeElement element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    @Override
    protected TypeSpec.Builder typeBuilder(TypeElement element, T annotation) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(buildClassName(element, annotation))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        GenerateContext context = new GenerateContext(element, processingEnv);
        typeBuilder.addAnnotation(ToString.class);
        typeBuilder.addAnnotation(EqualsAndHashCode.class);
        context.getMethodMap().values().stream()
                .map(methods -> methods.stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .forEach(fieldMethod -> processField(fieldMethod, context, typeBuilder, accessorPrefixed(annotation)));
        typeBuilder.addSuperinterface(element.asType());

        // 如果是 immutable 则字段会是 final, 即无法生成无参构造器
        if (mutable()) {
            typeBuilder.addAnnotation(NoArgsConstructor.class);
        }
        // 如果没有任何字段, NoArgsConstructor 和 AllArgsConstructor 会冲突...
        if (!typeBuilder.fieldSpecs.isEmpty()) {
            typeBuilder.addAnnotation(AllArgsConstructor.class);
        }
        if (generateBuilder(annotation)) {
            typeBuilder.addAnnotation(Builder.class);
        }
        return typeBuilder;
    }

    @Override
    protected String buildClassName(TypeElement element, T annotation) {
        return element.getSimpleName().toString() + getClassNameSuffix(annotation);
    }

    private void processField(ExecutableElement method, GenerateContext context, TypeSpec.Builder typeBuilder, boolean accessorPrefixed) {
        String methodName = method.getSimpleName().toString();
        if (accessorPrefixed && !methodName.startsWith(GET_PREFIX)) {
            typeBuilder.addMethod(buildUnmatchedMethod(methodName, method.getReturnType()));
            return;
        }

        String fieldName = accessorPrefixed ? StringUtils.uncapitalize(methodName.substring(GET_PREFIX.length())) : methodName;
        Optional<VariableElement> fieldOptional = context.getFieldMap().getOrDefault(fieldName, Collections.emptyList()).stream().findFirst();
        if (fieldOptional.isPresent()) {
            VariableElement field = fieldOptional.get();
            List<AnnotationSpec> getterAnnotations = new ArrayList<>();
            for (AnnotationMirror annotationMirror : field.getAnnotationMirrors()) {
                AnnotationSpec annotationSpec = AnnotationSpec.get(annotationMirror);
                if (Arrays.asList(annotationMirror.getAnnotationType().asElement().getAnnotation(Target.class).value()).contains(ElementType.METHOD)) {
                    getterAnnotations.add(annotationSpec);
                }
            }
            typeBuilder.addMethod(buildGetter(fieldName, method.getReturnType(), getterAnnotations, accessorPrefixed, false));
            return;
        }
        List<AnnotationSpec> fieldAnnotations = new ArrayList<>();
        List<AnnotationSpec> getterAnnotations = new ArrayList<>();
        for (AnnotationMirror annotationMirror : method.getAnnotationMirrors()) {
            AnnotationSpec annotationSpec = AnnotationSpec.get(annotationMirror);
            getterAnnotations.add(annotationSpec);
            if (Arrays.asList(annotationMirror.getAnnotationType().asElement().getAnnotation(Target.class).value()).contains(ElementType.FIELD)) {
                fieldAnnotations.add(annotationSpec);
            }
        }

        typeBuilder.addField(buildItemField(fieldName, method.getReturnType(), fieldAnnotations));
        typeBuilder.addMethod(buildGetter(fieldName, method.getReturnType(), getterAnnotations, accessorPrefixed));
        if (mutable()) {
            typeBuilder.addMethod(buildSetter(fieldName, method.getReturnType(), accessorPrefixed));
        }
    }

    private FieldSpec buildItemField(String fieldName, TypeMirror type, List<AnnotationSpec> fieldAnnotations) {
        FieldSpec.Builder builder = FieldSpec
                .builder(TypeName.get(type), fieldName, Modifier.PRIVATE)
                .addAnnotations(fieldAnnotations);
        if (!mutable()) {
            builder.addModifiers(Modifier.FINAL);
        }
        return builder.build();
    }

    private MethodSpec buildGetter(String fieldName, TypeMirror type, List<AnnotationSpec> fieldAnnotations, boolean accessorPrefixed) {
        return buildGetter(fieldName, type, fieldAnnotations, accessorPrefixed, true);
    }

    private MethodSpec buildGetter(String fieldName, TypeMirror type, List<AnnotationSpec> fieldAnnotations, boolean accessorPrefixed, boolean override) {
        String methodName = accessorPrefixed ? GET_PREFIX + StringUtils.capitalize(fieldName) : fieldName;
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(AnnotationSpec.builder(JsonGetter.class).addMember("value", "$S", fieldName).build())
                .addAnnotations(fieldAnnotations)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(type))
                .addStatement("return this." + fieldName);
        if (override) {
            builder.addAnnotation(Override.class);
        }
        return builder.build();
    }

    private MethodSpec buildSetter(String fieldName, TypeMirror type, boolean accessorPrefixed) {
        String methodName = accessorPrefixed ? SET_PREFIX + StringUtils.capitalize(fieldName) : fieldName;
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(AnnotationSpec.builder(JsonSetter.class).addMember("value", "$S", fieldName).build())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(TypeName.get(type), fieldName)
                .addStatement("this." + fieldName + " = " + fieldName)
                .build();
    }

    private MethodSpec buildUnmatchedMethod(String methodName, TypeMirror type) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(type))
                .addComment("FIXME This auto value field ungenerated, cause accessorPrefixed = true and methodName not start with get*")
                .addComment("Try to change method name start with get, Like [get" + StringUtils.capitalize(methodName) + "]")
                .addComment("or change annotation property accessorPrefixed to false, like @AutoValue(accessorPrefixed=false) or @ImmutableAutoValue(accessorPrefixed=false)")
                .addStatement("return null")
                .build();
    }

}
