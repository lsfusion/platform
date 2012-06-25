package platform.base;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtils {
    public static Class getFirstTypeParameterOfSuperclass(Class clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }

        Type superclassType = clazz.getGenericSuperclass();

        if (superclassType instanceof ParameterizedType) {
            Type[] params = ((ParameterizedType) superclassType).getActualTypeArguments();

            if (params != null && params.length > 0) {
                return (Class) params[0];
            }
        }

        for (Type ifaceType : clazz.getGenericInterfaces()) {
            if (ifaceType instanceof ParameterizedType) {
                Type[] params = ((ParameterizedType) ifaceType).getActualTypeArguments();

                if (params != null && params.length > 0) {
                    return (Class) params[0];
                }
            }
        }

        throw new IllegalArgumentException("Not generic type: " + clazz.getSimpleName());
    }

    public static Object getPrivateFieldValue(Object target, String fieldName) {
        return getPrivateFieldValue(target.getClass(), target, fieldName);
    }

    public static Object getPrivateFieldValue(Class clazz, Object target, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateFieldValue(Object target, String fieldName, Object value) {
        setPrivateFieldValue(target.getClass(), target, fieldName, value);
    }

    public static void setPrivateFieldValue(Class clazz, Object target, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getDeclaredMethodOrNull(Class<?> clazz, String methodName, Class<?>... args) {
        if (clazz == null || methodName == null) {
            return null;
        }

        try {
            return clazz.getDeclaredMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
