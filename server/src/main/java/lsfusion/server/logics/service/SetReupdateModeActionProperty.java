package lsfusion.server.logics.service;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.Connection;
import java.sql.SQLException;

public class SetReupdateModeActionProperty extends ScriptingActionProperty {

    public SetReupdateModeActionProperty(ServiceLogicsModule LM) {
        super(LM, LogicalClass.instance);
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
