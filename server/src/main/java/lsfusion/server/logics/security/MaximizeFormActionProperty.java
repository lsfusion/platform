package lsfusion.server.logics.security;

import lsfusion.interop.action.MaximizeFormClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class MaximizeFormActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface formCanonicalNameInterface;

    public MaximizeFormActionProperty(SecurityLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        formCanonicalNameInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String formCanonicalName = (String) context.getKeyValue(formCanonicalNameInterface).getValue();
        context.delayUserInteraction(new MaximizeFormClientAction(formCanonicalName));
    }
}