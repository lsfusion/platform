package platform.base;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GenericUtils {
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
}
