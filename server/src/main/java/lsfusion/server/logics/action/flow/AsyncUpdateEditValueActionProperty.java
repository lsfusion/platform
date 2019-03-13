package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.SystemActionProperty;

import java.io.IOException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serializeObject;

public class AsyncUpdateEditValueActionProperty extends SystemActionProperty {

    public AsyncUpdateEditValueActionProperty(LocalizedString caption) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));
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
