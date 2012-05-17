package platform.server.logics.property.actions;

import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.reverse;

public class PushUserInputActionProperty extends AroundAspectActionProperty {

    private final CalcPropertyInterfaceImplement<PropertyInterface> push;

    // по аналогии с If
    public <I extends PropertyInterface> PushUserInputActionProperty(String sID, String caption, List<I> innerInterfaces, CalcPropertyInterfaceImplement<I> push, ActionPropertyMapImplement<?, I> action) {
        super(sID, caption, innerInterfaces, action);

        Map<I, PropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.push = push.map(mapInterfaces);
    }

    @Override
    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
        return context.pushUserInput(push.readClasses(context, context.getKeys()));
    }
}
