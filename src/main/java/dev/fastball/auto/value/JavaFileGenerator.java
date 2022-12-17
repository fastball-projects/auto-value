package dev.fastball.auto.value;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import dev.fastball.auto.value.annotation.GeneratedFrom;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class JavaFileGenerator extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        loadElements(roundEnv).map(this::buildJavaFile)
                .forEach(file -> {
                    try {
                        file.writeTo(processingEnv.getFiler());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        return false;
    }

    protected Stream<TypeElement> loadElements(RoundEnvironment roundEnv) {
        return getSupportedAnnotationTypes().stream()
                .map(annotationName -> processingEnv.getElementUtils().getTypeElement(annotationName))
                .filter(Objects::nonNull)
                .flatMap(annotationType -> roundEnv.getElementsAnnotatedWith(annotationType).stream())
                .map(TypeElement.class::cast)
                .filter(this::needProcess);
    }

    protected JavaFile buildJavaFile(TypeElement element) {
        return JavaFile.builder(buildPackageName(element), buildType(element)).build();
    }

    protected TypeSpec buildType(TypeElement element) {
        AnnotationSpec generatedFromAnnotation = AnnotationSpec.builder(GeneratedFrom.class)
                .addMember("value", element.getQualifiedName().toString() + ".class")
                .addMember("generatorClass", "$S", this.getClass().getCanonicalName())
                .addMember("date", "$S", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .build();
        TypeSpec.Builder typeBuilder = typeBuilder(element);
        typeBuilder.addAnnotation(generatedFromAnnotation);
        return typeBuilder.build();
    }

    protected String buildPackageName(TypeElement element) {
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
        if (packageElement != null) {
            return packageElement.getQualifiedName().toString();
        }
        return "";
    }

    protected boolean needProcess(TypeElement element) {
        return true;
    }

    protected abstract TypeSpec.Builder typeBuilder(TypeElement element);

    protected abstract String buildClassName(TypeElement element);


}
