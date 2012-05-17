package platform.server.logics.property.actions.flow;

import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ReturnActionProperty extends KeepContextActionProperty {
    public ReturnActionProperty() {
        super("return", "return", 0);

        finalizeInit();
    }

    public Set<CalcProperty> getChangeProps() {
        return new HashSet<CalcProperty>();
    }

    public Set<CalcProperty> getUsedProps() {
        return new HashSet<CalcProperty>();
    }

    public FlowResult execute(ExecutionContext<PropertyInterface> context) throws SQLException {
        return FlowResult.RETURN;
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return new CalcPropertyMapImplement<PropertyInterface, PropertyInterface>(NullValueProperty.instance);
    }
}
