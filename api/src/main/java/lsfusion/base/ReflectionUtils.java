package lsfusion.base;

import com.google.common.base.Throwables;
import sun.misc.Unsafe;

import java.lang.reflect.*;

public class ReflectionUtils {

    static {
        disableWarning();
    }

    /*since java 9 there is 'Illegal reflective access' warning for the first usage of reflection methods,
    so we just disable IllegalAccessLogger
    https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
    */
    private static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception ignored) {
        }
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

    public static <T> void invokeStaticMethod(Class clazz, String methodName, Class[] paramsClasses, Object[] params) {
        try {
            Method method = clazz.getMethod(methodName, paramsClasses);
            method.setAccessible(true);
            method.invoke(null, params);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> T getStaticMethodValue(Class clazz, String methodName, Class[] paramsClasses, Object[] params) throws ClassNotFoundException {
        return getMethodValueWithException(clazz, null, methodName, paramsClasses, params);
    }

    public static <T> T getMethodValue(Class clazz, Object target, String methodName, Class[] paramsClasses, Object[] params) {
        try {
            return getMethodValueWithException(clazz, target, methodName, paramsClasses, params);
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> T getMethodValueWithException(Class clazz, Object target, String methodName, Class[] paramsClasses, Object[] params) throws ClassNotFoundException {
        try {
            Method method = clazz.getMethod(methodName, paramsClasses);
            method.setAccessible(true);
            return (T) method.invoke(target, params);
        } catch (InvocationTargetException e) {
            if(e.getCause() instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) e.getCause();
            } else {
                throw Throwables.propagate(e);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Method getSingleMethod(Object object, String method, int paramCount) {
        for (Method methodObject : object.getClass().getMethods())
            if (methodObject.getName().equals(method) && (paramCount == -1 || methodObject.getParameterTypes().length == paramCount))
                return methodObject;
        throw new RuntimeException("no single method");
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

    public static Class classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
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