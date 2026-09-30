package lsfusion.server.logics.scripted;

import lsfusion.server.base.caches.CacheAspect;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.lang.reflect.Field;

// LocalizedString's initializer makes NONAME through the start cache, which takes the business logics from the
// thread's context - a server's, and a unit test has none. The class is initialized with the caches off instead,
// so that the server need not guess whether a thread without a context is a test or a bug
class LocalizedStringOutsideServer {

    static void init() throws ReflectiveOperationException {
        Field disableCaches = CacheAspect.class.getDeclaredField("disableCaches");
        disableCaches.setAccessible(true);
        disableCaches.setBoolean(null, true);
        try {
            Class.forName(LocalizedString.class.getName());
        } finally {
            disableCaches.setBoolean(null, false);
        }
    }
}
