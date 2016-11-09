package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.AroundAspectActionProperty;

import java.sql.SQLException;

public class PushUserInputActionProperty extends AroundAspectActionProperty {

    private final CalcPropertyInterfaceImplement<PropertyInterface> push;

    // по аналогии с If
    public <I extends PropertyInterface> PushUserInputActionProperty(String caption, ImOrderSet<I> innerInterfaces, CalcPropertyInterfaceImplement<I> push, ActionPropertyMapImplement<?, I> action) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.push = push.map(mapInterfaces);
        
        finalizeInit();
    }

    @Override
    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return context.pushUserInput(push.readClasses(context, context.getKeys()));
    }
}
