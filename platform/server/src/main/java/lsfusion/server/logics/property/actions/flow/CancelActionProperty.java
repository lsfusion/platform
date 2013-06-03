package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class CancelActionProperty extends SystemExplicitActionProperty {

    public CancelActionProperty() {
        super("cancel", "cancel", new ValueClass[] {});
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.CANCEL;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.cancel();
    }

}
