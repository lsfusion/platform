package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class BreakActionProperty extends KeepContextActionProperty {
    public BreakActionProperty() {
        super("break", "break", new ValueClass[0]);
    }

    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        return FlowResult.BREAK;
    }
}
