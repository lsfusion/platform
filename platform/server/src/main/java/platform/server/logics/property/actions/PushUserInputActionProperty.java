package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.CalcPropertyInterfaceImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;

import java.sql.SQLException;

public class PushUserInputActionProperty extends AroundAspectActionProperty {

    private final CalcPropertyInterfaceImplement<PropertyInterface> push;

    // по аналогии с If
    public <I extends PropertyInterface> PushUserInputActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, CalcPropertyInterfaceImplement<I> push, ActionPropertyMapImplement<?, I> action) {
        super(sID, caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.push = push.map(mapInterfaces);
    }

    @Override
    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
        return context.pushUserInput(push.readClasses(context, context.getKeys()));
    }
}
