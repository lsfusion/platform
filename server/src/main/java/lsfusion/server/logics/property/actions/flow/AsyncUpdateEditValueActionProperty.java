package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.io.IOException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serializeObject;

public class AsyncUpdateEditValueActionProperty extends SystemActionProperty {

    public AsyncUpdateEditValueActionProperty(String caption) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
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
