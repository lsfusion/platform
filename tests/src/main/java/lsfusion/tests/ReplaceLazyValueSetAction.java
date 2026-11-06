package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.reflect.Field;

// the server's cache of LAZY values given a new set of the reads of lfReplaced over the one it has, as two first reads
// of a property at once leave it : the set the second one made put over the first one's. The map of the sets is a
// private field of a private class, reached by reflection
public class ReplaceLazyValueSetAction extends InternalAction {

    public ReplaceLazyValueSetAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            Field cacheField = DBManager.class.getDeclaredField("weakLazyValueCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(context.getDbManager());
            Field setsField = cache.getClass().getDeclaredField("propCache");
            setsField.setAccessible(true);
            ((LRUWVSMap<Object, Object>) setsField.get(cache)).put(findProperty("lfReplaced[]").property, new ConcurrentIdentityWeakHashSet<>());
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
