package platform.gwt.form2.server;

import platform.base.BaseUtils;

import java.lang.reflect.Method;

public class ObjectConverter {

    public <F, T> T convertOrNull(F from, Object... context) {
        if (from == null) {
            return null;
        }

        Object[] fullContext = BaseUtils.addElement(from, context, Object.class);

        Class<?> fromClass = from.getClass();

        for (Method converterMethod : this.getClass().getDeclaredMethods()) {
            Converter annotation = converterMethod.getAnnotation(Converter.class);
            if (annotation == null || annotation.from() != fromClass) {
                continue;
            }

            boolean suitableConverter = true;

            Class<?>[] paramTypes = converterMethod.getParameterTypes();
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
                    return (T) converterMethod.invoke(this, parameters);
                } catch (Exception ignore) {
                }
            }
        }

        return null;
    }

    public <F, T> T convertOrCast(F from, Object... context) {
        T convertOrNull = convertOrNull(from, context);
        return convertOrNull == null ? (T) from : convertOrNull;
    }
}
