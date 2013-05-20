package platform.server.logics.property.actions;

import com.google.common.base.Throwables;
import platform.base.col.SetFact;
import platform.interop.action.AsyncGetRemoteChangesClientAction;
import platform.interop.action.UpdateEditValueClientAction;
import platform.server.classes.LongClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.logics.property.derived.DerivedProperty;

import java.io.IOException;
import java.sql.SQLException;

import static platform.base.BaseUtils.serializeObject;

public class SleepActionProperty extends SystemExplicitActionProperty {

    public SleepActionProperty() {
        super("sleep", "Sleep", new ValueClass[]{LongClass.instance});
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
