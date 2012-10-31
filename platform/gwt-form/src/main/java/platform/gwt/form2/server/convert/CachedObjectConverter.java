package platform.gwt.form2.server.convert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * The class is NOT syncronized
 */
public class CachedObjectConverter extends ObjectConverter {
    private final HashMap cache = new HashMap();

    @Override
    protected <F, T> T convertInstance(F from, Object... context) {
        T cached = (T) cache.get(from);
        if (cached != null) {
            return cached;
        }
        return super.convertInstance(from, context);
    }

    @Override
    protected <F, T> T convertWithMethod(F from, Method converterMethod, Object... parameters) throws InvocationTargetException, IllegalAccessException {
        T to = super.convertWithMethod(from, converterMethod, parameters);

        return converterMethod.getAnnotation(Cached.class) != null
               ? cacheInstance(from, to)
               : to;
    }

    protected <F, T> T cacheInstance(F from, T to) {
        if (from == null || to == null) {
            throw new IllegalStateException("cacheInstance parameters must be not null");
        }
        cache.put(from, to);
        return to;
    }
}
