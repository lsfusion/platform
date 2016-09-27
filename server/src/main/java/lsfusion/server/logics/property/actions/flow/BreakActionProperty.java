package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class BreakActionProperty extends ChangeFlowActionProperty {
    public BreakActionProperty() {
        super(LocalizedString.create("break"));

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.BREAK;
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return FlowResult.BREAK;
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }
}
