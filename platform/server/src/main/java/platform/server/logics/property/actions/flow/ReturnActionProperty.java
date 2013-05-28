package platform.server.logics.property.actions.flow;

import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ReturnActionProperty extends ChangeFlowActionProperty {
    public ReturnActionProperty() {
        super("return", "return");

        finalizeInit();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.RETURN;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        return FlowResult.RETURN;
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createNull();
    }
}
