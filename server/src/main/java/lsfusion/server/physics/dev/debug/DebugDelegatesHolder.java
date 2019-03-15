package lsfusion.server.physics.dev.debug;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;

import java.sql.SQLException;

//sample
public class DebugDelegatesHolder {
    public static FlowResult action_12_23(Action action, ExecutionContext context) throws SQLException, SQLHandledException {
        return action.executeImpl(context);
    }
}
