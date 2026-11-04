package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// two threads binding keys to one property of a form at once, as two modules parsed at once that bind keys to one
// property do. The property's bindings - a private field, reached by reflection - are swapped for a map that holds the
// first binding inside it until the second thread either is inside too - two puts at once, which a plain map does not
// survive - or waits for the property the bindings are made under. What the test saw goes into kbFailure, and so does a
// run that did not have the two at once
public class BindKeysAtOnceAction extends AtOnceAction {

    public BindKeysAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        PropertyDrawEntity<?, ?> property = context.getBL().findForm("KeyBindings.kbForm").getPropertyDraw("name");
        try {
            Field field = PropertyDrawEntity.class.getDeclaredField("keyBindings");
            field.setAccessible(true);
            Object bindings = field.get(property);
            Hold hold = new Hold();
            Map<KeyStroke, String> holding = new HashMap<KeyStroke, String>() {
                @Override
                public String put(KeyStroke key, String action) {
                    return hold.call(() -> super.put(key, action));
                }
            };
            field.set(property, holding);
            String failure;
            try {
                failure = runAtOnce(context, hold, property, "two threads bound keys to the property at once",
                        () -> property.setKeyAction(KeyStroke.getKeyStroke("F5"), "kbFirst"),
                        () -> property.setKeyAction(KeyStroke.getKeyStroke("F6"), "kbSecond"));
            } finally {
                field.set(property, bindings);
            }
            Map<KeyStroke, String> bound = new HashMap<>();
            bound.put(KeyStroke.getKeyStroke("F5"), "kbFirst");
            bound.put(KeyStroke.getKeyStroke("F6"), "kbSecond");
            if (failure == null && !holding.equals(bound))
                failure = "the property kept the bindings " + holding;

            findProperty("kbFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
