package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class CancelActionProperty extends CustomActionProperty {

    public CancelActionProperty() {
        super("cancelChanges", "Отменить изменения", new ValueClass[] {} );
    }

    @Override
    public void executeCustom(ExecutionContext context) throws SQLException {
        context.cancel();
    }
}
