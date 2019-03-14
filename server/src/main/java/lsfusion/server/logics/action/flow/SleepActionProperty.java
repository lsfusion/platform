package lsfusion.server.logics.action.flow;

import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class SleepActionProperty extends ScriptingAction {

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
