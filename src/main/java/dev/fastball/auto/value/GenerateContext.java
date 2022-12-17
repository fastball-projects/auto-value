package dev.fastball.auto.value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gr@fastball.dev
 * @since 2022/12/17
 */
public class GenerateContext {
    private final Map<String, List<ExecutableElement>> methodMap = new HashMap<>();

    private final Map<String, List<VariableElement>> fieldMap = new HashMap<>();
    private final ProcessingEnvironment processingEnv;

    public GenerateContext(TypeElement element, ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        loadMethod(element);
        loadField(element);
    }

    public Map<String, List<ExecutableElement>> getMethodMap() {
        return methodMap;
    }

    public Map<String, List<VariableElement>> getFieldMap() {
        return fieldMap;
    }

    private void loadMethod(TypeElement element) {
        TypeMirror superclass = element.getSuperclass();
        if (!(superclass instanceof NoType)) {
            TypeElement superClass = (TypeElement) processingEnv.getTypeUtils().asElement(superclass);
            loadMethod(superClass);
        }
        List<? extends TypeMirror> interfaces = element.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            for (TypeMirror anInterface : interfaces) {
                TypeElement superInterface = (TypeElement) processingEnv.getTypeUtils().asElement(anInterface);
                loadMethod(superInterface);
            }
        }
        ElementFilter.methodsIn(element.getEnclosedElements()).forEach(method -> {
            List<ExecutableElement> methods = methodMap.computeIfAbsent(method.getSimpleName().toString(), methodName -> new ArrayList<>());
            methods.add(method);
        });
    }


    private void loadField(TypeElement element) {
        TypeMirror superclass = element.getSuperclass();
        if (!(superclass instanceof NoType)) {
            TypeElement superClass = (TypeElement) processingEnv.getTypeUtils().asElement(superclass);
            loadField(superClass);
        }
        List<? extends TypeMirror> interfaces = element.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            for (TypeMirror anInterface : interfaces) {
                TypeElement superInterface = (TypeElement) processingEnv.getTypeUtils().asElement(anInterface);
                loadField(superInterface);
            }
        }
        ElementFilter.fieldsIn(element.getEnclosedElements()).forEach(field -> {
            List<VariableElement> fields = fieldMap.computeIfAbsent(field.getSimpleName().toString(), fieldName -> new ArrayList<>());
            fields.add(field);
        });
    }

}

