package platform.server.logics.property.actions.flow;

import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class CancelActionProperty extends ChangeFlowActionProperty {

    public CancelActionProperty() {
        super("cancel", "cancel");
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.CANCEL;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        context.cancel();
        return FlowResult.FINISH;
    }

}
