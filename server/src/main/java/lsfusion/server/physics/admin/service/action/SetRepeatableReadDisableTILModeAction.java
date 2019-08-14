package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class SetRepeatableReadDisableTILModeAction extends InternalAction {

    public SetRepeatableReadDisableTILModeAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Object value = context.getSingleKeyObject();
        if(value!=null)
            DBManager.SESSION_TIL =  - 1;
        else
            DBManager.SESSION_TIL = Connection.TRANSACTION_REPEATABLE_READ;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}