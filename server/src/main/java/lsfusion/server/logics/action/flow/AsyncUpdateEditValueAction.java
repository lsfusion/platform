package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serializeObject;

public class AsyncUpdateEditValueAction extends SystemAction {

    public AsyncUpdateEditValueAction(LocalizedString caption) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        Object updatedValue = context.getSingleKeyObject();
        try {
            context.delayUserInteraction(new UpdateEditValueClientAction(serializeObject(updatedValue)));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        context.delayUserInteraction(new AsyncGetRemoteChangesClientAction(false));
        return FlowResult.FINISH;
    }
}
