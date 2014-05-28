package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.SetFact;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class MessageActionProperty extends SystemActionProperty {
    protected final String title;

    public <I extends PropertyInterface> MessageActionProperty(String sID, String caption, String title) {
        super(sID, caption, SetFact.singletonOrder(new PropertyInterface()));

        this.title = title;
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        // TRUE AND a OR (NOT a), т.е. значение всегда TRUE, но при join'е будет учавствовать в classWhere - FULL
        return DerivedProperty.createUnion(interfaces, DerivedProperty.createAnd(DerivedProperty.createTrue(), interfaces.single()), DerivedProperty.createNot(interfaces.single()));
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue objValue = context.getSingleKeyValue();
        showMessage(context, objValue == null ? null : objValue.getValue());
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException, SQLHandledException {
        context.requestUserInteraction(new MessageClientAction(String.valueOf(msgValue), title));
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        // потому как важен порядок, в котором выдаются MESSAGE'и, иначе компилятор начнет их переставлять
        return type == ChangeFlowType.VOLATILE;
    }
}
