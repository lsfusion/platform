package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.SetFact;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class MessageAction extends SystemAction {
    protected final String title;
    private boolean noWait = false;

    public <I extends PropertyInterface> MessageAction(LocalizedString caption, String title) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));

        this.title = title;
    }

    public <I extends PropertyInterface> MessageAction(LocalizedString caption, String title, boolean noWait) {
        this(caption, title);

        this.noWait = noWait;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue objValue = context.getSingleKeyValue();
        showMessage(context, objValue.getValue());
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException, SQLHandledException {
        if (noWait) {
            context.delayUserInteraction(new MessageClientAction(String.valueOf(msgValue), title));
        } else {
            context.requestUserInteraction(new MessageClientAction(String.valueOf(msgValue), title));
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
