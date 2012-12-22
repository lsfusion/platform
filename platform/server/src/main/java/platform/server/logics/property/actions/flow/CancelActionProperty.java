package platform.server.logics.property.actions.flow;

import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

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
