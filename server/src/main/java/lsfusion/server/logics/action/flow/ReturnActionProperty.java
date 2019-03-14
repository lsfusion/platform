package lsfusion.server.logics.action.flow;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ReturnActionProperty extends ChangeFlowAction {
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

    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }
}
