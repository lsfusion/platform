package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.IsClassProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class CustomActionProperty extends ActionProperty {

    protected CustomActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected CustomActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected abstract void executeCustom(ExecutionContext context) throws SQLException;

    public final FlowResult execute(ExecutionContext context) throws SQLException {
        if(IsClassProperty.fitInterfaceClasses(context.getSession().getCurrentClasses(context.getKeys()))) // если подходит по классам выполнем
            executeCustom(context);
        return FlowResult.FINISH;
    }

    public Set<CalcProperty> getChangeProps() {
        return new HashSet<CalcProperty>();
    }

    public Set<CalcProperty> getUsedProps() {
        return new HashSet<CalcProperty>();
    }
}
