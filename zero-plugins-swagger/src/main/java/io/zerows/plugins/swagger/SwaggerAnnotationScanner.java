package io.zerows.plugins.swagger;

import io.zerows.core.metadata.store.OCacheClass;
import jakarta.ws.rs.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class SwaggerAnnotationScanner {

    public static Set<Class<?>> scan() {
        Set<Class<?>> allClasses = OCacheClass.entireValue();
        Set<Class<?>> interfaces = new HashSet<>();

        for (Class<?> cls : allClasses) {
            if (!cls.isInterface()) continue;

            boolean hasPath = cls.isAnnotationPresent(Path.class);
            boolean hasOperation = false;

            for (Method method : cls.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Path.class) ||
                    method.isAnnotationPresent(GET.class) ||
                    method.isAnnotationPresent(POST.class) ||
                    method.isAnnotationPresent(PUT.class) ||
                    method.isAnnotationPresent(DELETE.class) ||
                    method.isAnnotationPresent(PATCH.class) ||
                    method.isAnnotationPresent(Operation.class) ||
                    method.isAnnotationPresent(RequestBody.class) ||
                    method.isAnnotationPresent(ApiResponse.class) ||
                    method.isAnnotationPresent(Parameter.class) ||
                    method.isAnnotationPresent(Schema.class) ||
                    hasParameterAnnotations(method)) {
                    hasOperation = true;
                    break;
                }
            }

            if (hasPath && hasOperation) {
                interfaces.add(cls);
            }
        }

        return interfaces;
    }

    /**
     * 检查方法参数是否有 Swagger 注解
     */
    private static boolean hasParameterAnnotations(Method method) {
        for (java.lang.reflect.Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Parameter.class)) {
                return true;
            }
        }
        return false;
    }
}
