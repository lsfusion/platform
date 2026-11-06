package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.reflect.Field;

// the server's cache of LAZY values letting go of the sets of the properties' reads, as its LRU map of them does with a
// set not used for its expiry time (minutes) and with the least used ones when memory runs short - here with all of them
// at once. The map is a private field of a private class, reached by reflection
public class LetGoLazyValueSetsAction extends InternalAction {

    public LetGoLazyValueSetsAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            Field cacheField = DBManager.class.getDeclaredField("weakLazyValueCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(context.getDbManager());
            Field setsField = cache.getClass().getDeclaredField("propCache");
            setsField.setAccessible(true);
            ((LRUWVSMap<?, ?>) setsField.get(cache)).clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
