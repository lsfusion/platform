package platform.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.action.AsyncGetRemoteChangesClientAction;
import platform.interop.action.UpdateEditValueClientAction;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;
import platform.server.logics.property.derived.DerivedProperty;

import java.io.IOException;
import java.sql.SQLException;

import static platform.base.BaseUtils.serializeObject;

public class AsyncUpdateEditValueActionProperty extends SystemActionProperty {

    public AsyncUpdateEditValueActionProperty(String sID, String caption) {
        super(sID, caption, SetFact.singletonOrder(new PropertyInterface()));
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createNull();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        Object updatedValue = context.getSingleKeyObject();
        try {
            context.delayUserInteraction(new UpdateEditValueClientAction(serializeObject(updatedValue)));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        context.delayUserInteraction(new AsyncGetRemoteChangesClientAction());
        return FlowResult.FINISH;
    }
}
