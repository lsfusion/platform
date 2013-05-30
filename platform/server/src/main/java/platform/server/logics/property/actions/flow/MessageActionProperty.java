package platform.server.logics.property.actions.flow;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.action.MessageClientAction;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;
import platform.server.logics.property.derived.DerivedProperty;

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

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        showMessage(context, context.getSingleKeyValue());
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException {
        context.requestUserInteraction(new MessageClientAction(String.valueOf(msgValue), title));
    }
}
