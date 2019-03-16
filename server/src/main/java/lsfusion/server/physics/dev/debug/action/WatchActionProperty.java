package lsfusion.server.physics.dev.debug.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class WatchActionProperty extends SystemExplicitAction {
    
    public WatchActionProperty() {
        super();
    }

    public static final WatchActionProperty instance = new WatchActionProperty();
    
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ExecutionStack stack = context.stack;
        stack.getWatcher().proceed(stack.getAllParamsWithValuesInStack());
    }
}
