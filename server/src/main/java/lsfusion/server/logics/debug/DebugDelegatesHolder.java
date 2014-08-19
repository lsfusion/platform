package lsfusion.server.logics.debug;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

//sample
public class DebugDelegatesHolder {
    public static FlowResult action_12_23(ActionProperty action, ExecutionContext context) throws SQLException, SQLHandledException {
        return action.executeImpl(context);
    }
}
