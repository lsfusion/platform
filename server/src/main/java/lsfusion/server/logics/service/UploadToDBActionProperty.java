package lsfusion.server.logics.service;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.MSSQLDataAdapter;
import lsfusion.server.data.sql.PostgreDataAdapter;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class UploadToDBActionProperty extends ScriptingActionProperty {

    public UploadToDBActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String type = (String) LM.getLCPByOldName("uploadStaticNameType").read(context);
        String host = (String) LM.getLCPByOldName("uploadHost").read(context);
        String user = (String) LM.getLCPByOldName("uploadUser").read(context);
        String password = (String) LM.getLCPByOldName("uploadPassword").read(context);
        String db = (String) LM.getLCPByOldName("uploadDB").read(context);
        String instance = (String) LM.getLCPByOldName("uploadInstance").read(context);

        final DataAdapter adapter;
        try {
            if(type.trim().equals("Service_DBType.POSTGRE"))
                adapter = new PostgreDataAdapter(db, host, user, password);
            else if(type.trim().equals("Service_DBType.MSSQL")) 
                adapter = new MSSQLDataAdapter(db, host, user, password, instance);
            else
                throw new UnsupportedOperationException();
        } catch (Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }

        ServiceDBActionProperty.run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                try {
                    context.getDbManager().uploadToDB(session, isolatedTransaction, adapter);
                } catch (Exception e) {
                    throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
                }
            }});

        context.delayUserInterfaction(new MessageClientAction(getString("logics.upload.was.completed"), getString("logics.upload.db")));
    }

}
