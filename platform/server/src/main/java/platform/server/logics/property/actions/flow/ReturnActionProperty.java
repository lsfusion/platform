package platform.server.logics.property.actions.flow;

import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
