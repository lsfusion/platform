package platform.server.logics.property.actions.flow;

import platform.server.caches.IdentityLazy;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
