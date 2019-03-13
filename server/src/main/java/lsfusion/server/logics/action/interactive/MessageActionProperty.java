package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.SetFact;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class MessageActionProperty extends SystemActionProperty {
    protected final String title;
    private boolean noWait = false;

    public <I extends PropertyInterface> MessageActionProperty(LocalizedString caption, String title) {
        super(caption, SetFact.singletonOrder(new PropertyInterface()));

        this.title = title;
    }

    public <I extends PropertyInterface> MessageActionProperty(LocalizedString caption, String title, boolean noWait) {
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
