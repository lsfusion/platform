package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.LongClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serializeObject;

public class SleepActionProperty extends ScriptingActionProperty {

    public SleepActionProperty(BaseLogicsModule lm) {
        super(lm, new ValueClass[]{LongClass.instance});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        long updatedValue = ((Number) context.getSingleKeyObject()).longValue();
        try {
            Thread.sleep(updatedValue);
        } catch (InterruptedException e) {
        }
    }
}
