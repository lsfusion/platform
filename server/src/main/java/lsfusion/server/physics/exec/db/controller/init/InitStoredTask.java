package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.service.task.GroupGraphTask;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;

public class InitStoredTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing fields for data and materialized properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof Property && ((Property) property).isMarkedStored() && ((Property) property).field == null) { // last check we need because we already initialized some stored (see other initStored usages)
            ((Property) property).initStored(getTableFactory(), getDBNamingPolicy());
        }
    }
}
