package lsfusion.server.physics.dev.debug;

import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class WatchActionProperty extends SystemExplicitAction {
    
    public WatchActionProperty() {
        super();
    }

    public static final WatchActionProperty instance = new WatchActionProperty();
    
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ExecutionStack stack = context.stack;
        stack.getWatcher().proceed(stack.getAllParamsWithValuesInStack());
    }
}
