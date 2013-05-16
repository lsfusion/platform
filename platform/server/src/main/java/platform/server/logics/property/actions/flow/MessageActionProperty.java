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
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class MessageActionProperty extends KeepContextActionProperty {
    protected final CalcPropertyMapImplement<?, PropertyInterface> msgProperty;
    protected final String title;

    public <I extends PropertyInterface> MessageActionProperty(String sID, String caption, String title, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> msgProperty) {
        super(sID, caption, innerInterfaces.size());

        this.title = title;

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.msgProperty = msgProperty.map(mapInterfaces);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        // TRUE OR (TRUE IF msgProperty), т.е. значение всегда TRUE,
        // но определено для тех классов, для которых определено msgProperty
        CalcPropertyMapImplement<?, PropertyInterface> vTrue = DerivedProperty.createTrue();
        return DerivedProperty.createUnion(interfaces, vTrue, DerivedProperty.createAnd(vTrue, msgProperty));
    }

    @Override
    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        Object msgValue = msgProperty.read(context, context.getKeys());
        showMessage(context, msgValue);
        return FlowResult.FINISH;
    }

    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException {
        context.requestUserInteraction(new MessageClientAction(String.valueOf(msgValue), title));
    }
}
