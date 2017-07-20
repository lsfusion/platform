package lsfusion.server.logics.debug;

import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class WatchActionProperty extends SystemExplicitActionProperty {
    
    public WatchActionProperty() {
        super();
    }

    public static final WatchActionProperty instance = new WatchActionProperty();
    
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ExecutionStack stack = context.stack;
        stack.getWatcher().proceed(stack.getAllParamsWithValuesInStack());
    }
}
