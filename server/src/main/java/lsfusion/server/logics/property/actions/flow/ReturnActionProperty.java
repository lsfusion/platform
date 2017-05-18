package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ReturnActionProperty extends ChangeFlowActionProperty {
    public ReturnActionProperty() {
        super(LocalizedString.create("return"));

        finalizeInit();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.RETURN)
            return true;
        return super.hasFlow(type);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return FlowResult.RETURN;
    }

    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }
}
