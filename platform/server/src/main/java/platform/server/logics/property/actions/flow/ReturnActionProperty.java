package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ReturnActionProperty extends KeepContextActionProperty {
    public ReturnActionProperty() {
        super("return", "return", new ValueClass[0]);
    }

    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        return FlowResult.RETURN;
    }
}
