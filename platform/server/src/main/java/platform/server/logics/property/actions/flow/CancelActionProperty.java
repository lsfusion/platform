package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class CancelActionProperty extends SystemActionProperty {

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
