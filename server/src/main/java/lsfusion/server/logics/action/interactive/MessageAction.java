package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.SetFact;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class MessageAction extends SystemAction {
    protected final String title;
    private boolean noWait = false;
    private boolean log = false;

    public <I extends PropertyInterface> MessageAction(LocalizedString caption, String title) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));

        this.title = title;
    }

    public <I extends PropertyInterface> MessageAction(LocalizedString caption, String title, boolean noWait, boolean log) {
        this(caption, title);

        this.noWait = noWait;
        this.log = log;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue singleKeyValue = context.getSingleKeyValue();
        showMessage(context, singleKeyValue instanceof NullValue ? null : singleKeyValue.getType().formatString(singleKeyValue.getValue(), true));
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, String message) throws SQLException, SQLHandledException {
        ClientAction action;
        if(message == null)
            message = "";
        if(log)
            action = new LogMessageClientAction(message, false);
        else
            action = new MessageClientAction(message, title);

        if (noWait) {
            context.delayUserInteraction(action);
        } else {
            context.requestUserInteraction(action);
        }
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        // потому как важен порядок, в котором выдаются MESSAGE'и, иначе компилятор начнет их переставлять
        if(type == ChangeFlowType.SYNC)
            return true;
        return super.hasFlow(type);
    }
}
