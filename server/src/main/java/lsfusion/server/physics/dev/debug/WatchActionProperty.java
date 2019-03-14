package lsfusion.server.physics.dev.debug;

import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitActionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

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
