package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.SetFact;
import lsfusion.interop.action.MessageClientType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.nvl;

public class MessageAction extends SystemAction {
    private PropertyInterface messageInterface;
    private PropertyInterface headerInterface;

    private boolean noWait;
    private MessageClientType type;

    public MessageAction(LocalizedString caption, boolean hasHeader) {
        super(caption, SetFact.toOrderExclSet(hasHeader ? 2 : 1, i -> new PropertyInterface()));

        this.messageInterface = getOrderInterfaces().get(0);
        this.headerInterface = hasHeader ? getOrderInterfaces().get(1) : null;
    }

    public MessageAction(LocalizedString caption, boolean hasHeader, boolean noWait, MessageClientType type) {
        this(caption, hasHeader);

        this.noWait = noWait;
        this.type = type;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue messageObject = context.getKeyValue(messageInterface);
        String message = messageObject.getType().formatUI(messageObject.getValue());

        String header = null;
        if(headerInterface != null) {
            ObjectValue headerObject = context.getKeyValue(headerInterface);
            header = headerObject.getType().formatUI(headerObject.getValue());
        }

        showMessage(context, message, nvl(header, "lsFusion"));
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, String message, String header) throws SQLException, SQLHandledException {
        context.message(context.getRemoteContext(), message, header, new ArrayList<>(), new ArrayList<>(), type, noWait);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.SYNC)
            return true;
        if(type == ChangeFlowType.INTERACTIVEWAIT)
            return true;
        return super.hasFlow(type);
    }
}
