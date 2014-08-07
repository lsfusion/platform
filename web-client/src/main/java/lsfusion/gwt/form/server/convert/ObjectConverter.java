package lsfusion.gwt.form.server.convert;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ObjectConverter {

    private final HashMap<Class, List<Method>> converterMethods = new HashMap<Class, List<Method>>();

    public ObjectConverter() {
        for (Method converterMethod : this.getClass().getDeclaredMethods()) {
            Converter annotation = converterMethod.getAnnotation(Converter.class);
            if (annotation != null) {
                Class<?> fromClass = annotation.from();
                List<Method> methodList = converterMethods.get(fromClass);
                if (methodList == null) {
                    methodList = new ArrayList<Method>();
                    converterMethods.put(fromClass, methodList);
                }

                methodList.add(converterMethod);
            }
        }
    }

    public <F, T> T convertOrCast(F from, Object... context) {
        T convertOrNull = convertOrNull(from, context);
        return convertOrNull == null ? (T) from : convertOrNull;
    }

    public <F, T> T convertOrNull(F from, Object... context) {
        if (from == null) {
            return null;
        }

        return convertInstance(from, context);
    }

    protected <F, T> T convertInstance(F from, Object... context) {

        Object[] fullContext = BaseUtils.addElement(from, context, Object.class);

        Class<?> fromClass = from.getClass();

        List<Method> classConverters = converterMethods.get(fromClass);
        if (classConverters != null) {
            for (Method classConverter : classConverters) {
                boolean suitableConverter = true;

                Class<?>[] paramTypes = classConverter.getParameterTypes();
                Object parameters[] = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];

                    boolean foundParameter = false;
                    for (Object contextObject : fullContext) {
                        if (contextObject != null && paramType.isInstance(contextObject)) {
                            parameters[i] = contextObject;
                            foundParameter = true;
                            break;
                        }
                    }

                    if (!foundParameter) {
                        suitableConverter = false;
                        break;
                    }
                }

                if (suitableConverter) {
                    try {
                        return convertWithMethod(from, classConverter, parameters);
                    } catch (InvocationTargetException ite) {
                        Throwables.propagate(ite.getCause());
                    } catch (Exception e) {
                        Throwables.propagate(e);
                    }
                }
            }
        }

        return null;
    }

    protected <F, T> T convertWithMethod(F from, Method converterMethod, Object... parameters) throws InvocationTargetException, IllegalAccessException {
        return (T) converterMethod.invoke(this, parameters);
    }
}
