package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.SQLException;

// two threads binding keys to one property at once, as two modules parsed at once that bind actions to keys of one
// property (ASON KEYPRESS) do. The bindings are made on the property's own monitor, so the test holds it, as a first
// binding would, and a second one has to wait. The property's bindings - a private field, reached by reflection - are a
// fresh map for the time of the test and are put back afterwards. What the test saw goes into pkFailure
public class BindPropertyKeysAtOnceAction extends AtOnceAction {

    public BindPropertyKeysAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ActionOrProperty<?> property = findProperty("pkName[]").property;
            String action = findAction("pkAction[]").action.getSID();
            Field field = ActionOrProperty.class.getDeclaredField("keyBindings");
            field.setAccessible(true);
            Object bindings = field.get(property);
            MMap<KeyStroke, String> fresh = MapFact.mMap(MapFact.override());
            field.set(property, fresh);
            String failure;
            try {
                failure = runWhileHeld(context, property, "a thread bound a key to the property while another one held it",
                        () -> property.setKeyAction(KeyStroke.getKeyStroke("F7"), action));
            } finally {
                field.set(property, bindings);
            }
            if (failure == null && !action.equals(fresh.get(KeyStroke.getKeyStroke("F7"))))
                failure = "the property kept the bindings " + fresh.immutableCopy();

            findProperty("pkFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
