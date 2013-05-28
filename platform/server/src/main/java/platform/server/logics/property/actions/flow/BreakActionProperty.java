package platform.server.logics.property.actions.flow;

import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class BreakActionProperty extends ChangeFlowActionProperty {
    public BreakActionProperty() {
        super("break", "break");

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.BREAK;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        return FlowResult.BREAK;
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createNull();
    }
}
