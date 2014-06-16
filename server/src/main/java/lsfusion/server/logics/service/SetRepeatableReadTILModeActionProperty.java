package lsfusion.server.logics.service;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
//import net.sourceforge.jtds.jdbc.ConnectionJDBC2;

import java.sql.Connection;
import java.sql.SQLException;

public class SetRepeatableReadTILModeActionProperty extends ScriptingActionProperty {

    public SetRepeatableReadTILModeActionProperty(ServiceLogicsModule LM) {
        super(LM, LogicalClass.instance);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object value = context.getSingleKeyObject();
        if(value!=null)
            DBManager.SESSION_TIL = Connection.TRANSACTION_REPEATABLE_READ;
        else
            DBManager.SESSION_TIL = -1; 
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
