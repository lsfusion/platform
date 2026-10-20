package lsfusion.server.logics.scripted;

import lsfusion.server.base.caches.CacheAspect;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.lang.reflect.Field;

// LocalizedString's initializer makes NONAME through the start cache, which takes the business logics from the
// thread's context - a server's, and a unit test has none. The class is initialized with the caches off instead,
// so that the server need not guess whether a thread without a context is a test or a bug
public class LocalizedStringOutsideServer {

    public static void init(Class<?>... classes) throws ReflectiveOperationException {
        setCachesDisabled(true);
        try {
            Class.forName(LocalizedString.class.getName());
            for (Class<?> cls : classes)
                Class.forName(cls.getName());
        } finally {
            setCachesDisabled(false);
        }
    }

    // a test that creates localized strings itself (a built-in class creates one when it is created) keeps the caches off while it runs
    public static void setCachesDisabled(boolean disabled) throws ReflectiveOperationException {
        Field disableCaches = CacheAspect.class.getDeclaredField("disableCaches");
        disableCaches.setAccessible(true);
        disableCaches.setBoolean(null, disabled);
    }
}
