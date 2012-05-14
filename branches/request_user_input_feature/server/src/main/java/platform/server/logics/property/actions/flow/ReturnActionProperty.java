package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ReturnActionProperty extends KeepContextActionProperty {
    public ReturnActionProperty() {
        super("return", "return", new ValueClass[0]);

        finalizeInit();
    }

    public Set<CalcProperty> getChangeProps() {
        return new HashSet<CalcProperty>();
    }

    public Set<CalcProperty> getUsedProps() {
        return new HashSet<CalcProperty>();
    }

    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        return FlowResult.RETURN;
    }
}
