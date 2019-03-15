package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.exec.db.DBManager;

import java.sql.SQLException;

public class SetReupdateModeActionProperty extends ScriptingAction {

    public SetReupdateModeActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object value = context.getSingleKeyObject();
        DBManager.PROPERTY_REUPDATE = value!=null;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

}
