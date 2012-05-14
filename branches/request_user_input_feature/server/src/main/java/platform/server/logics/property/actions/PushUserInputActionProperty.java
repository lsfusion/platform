package platform.server.logics.property.actions;

import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.reverse;

public class PushUserInputActionProperty extends AroundAspectActionProperty {

    private final CalcPropertyInterfaceImplement<ClassPropertyInterface> push;

    // по аналогии с If
    public <I extends PropertyInterface> PushUserInputActionProperty(String sID, String caption, List<I> innerInterfaces, CalcPropertyInterfaceImplement<I> push, ActionPropertyMapImplement<I> action) {
        super(sID, caption, innerInterfaces, action);

        Map<I, ClassPropertyInterface> mapInterfaces = reverse(getMapInterfaces(innerInterfaces));
        this.push = push.map(mapInterfaces);
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext context) throws SQLException {

        ObjectValue readValue = push.readClasses(context, context.getKeys());
        context.pushUserInput(readValue);
        FlowResult result = proceed(context);
        context.popUserInput(readValue);
        return result;
    }
}
