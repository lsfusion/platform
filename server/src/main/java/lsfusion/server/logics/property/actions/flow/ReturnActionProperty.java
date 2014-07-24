package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ReturnActionProperty extends ChangeFlowActionProperty {
    public ReturnActionProperty() {
        super("return");

        finalizeInit();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.RETURN;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return FlowResult.RETURN;
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createNull();
    }
}
