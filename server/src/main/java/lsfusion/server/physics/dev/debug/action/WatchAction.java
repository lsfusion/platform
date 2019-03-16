package lsfusion.server.physics.dev.debug.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class WatchAction extends SystemExplicitAction {
    
    public WatchAction() {
        super();
    }

    public static final WatchAction instance = new WatchAction();
    
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ExecutionStack stack = context.stack;
        stack.getWatcher().proceed(stack.getAllParamsWithValuesInStack());
    }
}
