package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class SleepActionProperty extends ScriptingActionProperty {

    public SleepActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        long updatedValue = ((Number) context.getSingleKeyObject()).longValue();
        try {
            Thread.sleep(updatedValue);
        } catch (InterruptedException e) {
        }
    }
}
