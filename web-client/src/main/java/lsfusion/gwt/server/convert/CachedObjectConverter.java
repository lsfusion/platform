package lsfusion.gwt.server.convert;

import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.shared.view.ImageDescription;
import lsfusion.interop.SerializableImageIconHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * The class is NOT syncronized
 */
public class CachedObjectConverter extends ObjectConverter {
    private final HashMap cache = new HashMap();

    private final String logicsName;
    protected CachedObjectConverter(String logicsName) {
        this.logicsName = logicsName;
    }

    protected ImageDescription createImage(SerializableImageIconHolder iconHolder, String iconPath, String imagesFolderName, boolean canBeDisabled) {
        return FileUtils.createImage(logicsName, iconHolder, iconPath, imagesFolderName, canBeDisabled);
    }

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
