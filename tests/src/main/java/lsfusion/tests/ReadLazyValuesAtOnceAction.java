package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

// two threads reading values of one property into the server's cache of lazy values at once, as two first reads of a
// LAZY property do. A read registers itself in a set of the property's, and the cache's map of those sets - a private
// field of a private class, reached by reflection - is swapped for one that holds the first thread inside its put of a
// new set until the second thread either is inside too - two sets of the property, one of them lost - or waits for the
// map the sets are made under; then the map is put back. The reads are of a property of the test module, under keys no
// other read has. What the test saw goes into lvFailure
public class ReadLazyValuesAtOnceAction extends AtOnceAction {

    public ReadLazyValuesAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Field cacheField = DBManager.class.getDeclaredField("weakLazyValueCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(context.getDbManager());
            Field mapField = cache.getClass().getDeclaredField("propCache");
            mapField.setAccessible(true);
            Object map = mapField.get(cache);
            Method proceed = cache.getClass().getDeclaredMethod("proceed", ActionOrProperty.class, Object.class, SQLCallable.class);
            proceed.setAccessible(true);
            ActionOrProperty<?> property = findProperty("lvName[]").property;

            Hold hold = new Hold();
            LRUWVSMap<Object, Object> holding = new LRUWVSMap<Object, Object>(LRUUtil.G1) {
                @Override
                public Object put(Object key, Object refs) {
                    return hold.call(() -> super.put(key, refs));
                }
            };
            mapField.set(cache, holding);
            String failure;
            try {
                failure = runAtOnce(context, hold, holding, "two threads made sets of the property's reads at once",
                        () -> read(proceed, cache, property, "lvFirst"),
                        () -> read(proceed, cache, property, "lvSecond"));
            } finally {
                mapField.set(cache, map);
            }
            Object refs = holding.get(property);
            if (failure == null && (refs == null || ((ConcurrentIdentityWeakHashSet<?>) refs).size() != 2))
                failure = "the property's set kept " + (refs == null ? 0 : ((ConcurrentIdentityWeakHashSet<?>) refs).size()) + " of the two reads";

            findProperty("lvFailure[]").change(failure, context);
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static void read(Method proceed, Object cache, ActionOrProperty<?> property, String key) {
        try {
            proceed.invoke(cache, property, key, (SQLCallable<Object>) () -> key);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }
}
