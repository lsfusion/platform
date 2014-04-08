package lsfusion.base;

import com.google.common.base.Throwables;

import java.lang.reflect.*;

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

    public static Object getPrivateStaticFieldValue(Class clazz, String fieldName) {
        return getPrivateFieldValue(clazz, null, fieldName);
    }

    public static void setPrivateStaticFieldValue(Class clazz, String fieldName, Object value) {
        setPrivateFieldValue(clazz, null, fieldName, value);
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
            throw Throwables.propagate(e);
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
            throw Throwables.propagate(e);
        }
    }

    public static <T> T getPrivateMethodValue(Class clazz, Object target, String methodName, Class[] paramsClasses, Object[] params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramsClasses);
            method.setAccessible(true);
            return (T) method.invoke(target, params);
        } catch (Exception e) {
            throw Throwables.propagate(e);
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

    public static <T> T makeSynchronized(Class<T> ifaceClass, T object) {
        return (T) Proxy.newProxyInstance(ifaceClass.getClassLoader(), new Class<?>[]{ifaceClass}, new Handler(object));
    }

    public static Method getSingleMethod(Object object, String method, int paramCount) {
        for (Method methodObject : object.getClass().getMethods())
            if (methodObject.getName().equals(method) && (paramCount == -1 || methodObject.getParameterTypes().length == paramCount))
                return methodObject;
        throw new RuntimeException("no single method");
    }

    public static Method getSingleMethod(Object object, String method) {
        return getSingleMethod(object, method, -1);
    }

    public static void invokeCheckSetter(Object object, String field, Object set) {
        if (!BaseUtils.nullEquals(invokeGetter(object, field), set))
            invokeSetter(object, field, set);
    }

    public static void invokeSetter(Object object, String field, Object set) {
        try {
            getSingleMethod(object, "set" + BaseUtils.capitalize(field), 1).invoke(object, set);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Object invokeGetter(Object object, String field) {
        try {
            Method method = object.getClass().getMethod("get" + BaseUtils.capitalize(field));
            return method.invoke(object);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void invokeAdder(Object object, String field, Object add) {
        try {
            getSingleMethod(object, "addTo" + BaseUtils.capitalize(field), 1).invoke(object, add);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void invokeRemover(Object object, String field, Object add) {
        try {
            getSingleMethod(object, "removeFrom" + BaseUtils.capitalize(field), 1).invoke(object, add);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> T invokeMethod(Method method, Object object, Object... parameters) {
        try {
            return (T) method.invoke(object, parameters);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Method getPrivateMethod(Class clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    public static <T> T invokePrivateMethod(Class clazz, Object object, String methodName, Class<?>[] parameterTypes, Object... parameters) {
        return invokeMethod(getPrivateMethod(clazz, methodName, parameterTypes), object, parameters);
    }

    public static Object invokeTransp(Method method, Object object, Object... args) throws Throwable {
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static <T> T createByPrivateConstructor(Class<T> clazz, Class<?>[] parameterTypes, Object... parameters) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(parameters);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static class Handler<T> implements InvocationHandler {
        private final T object;

        public Handler(T object) {
            this.object = object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            synchronized (object) {
                try {
                    return method.invoke(object, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }
    }
}