package lsfusion.server.physics.admin.service;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.PostgreDataAdapter;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.server.base.thread.ThreadLocalContext.localize;

public class UploadToDBActionProperty extends ScriptingAction {

    public UploadToDBActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            String type = (String) findProperty("uploadStaticNameType[]").read(context);
            String host = (String) findProperty("uploadHost[]").read(context);
            String user = (String) findProperty("uploadUser[]").read(context);
            String password = (String) findProperty("uploadPassword[]").read(context);
            String db = (String) findProperty("uploadDB[]").read(context);
            String instance = (String) findProperty("uploadInstance[]").read(context);
    
            final DataAdapter adapter;
            try {
                if(type.trim().equals("Service_DBType.POSTGRE"))
                    adapter = new PostgreDataAdapter(db, host, user, password);
//                else if(type.trim().equals("Service_DBType.MSSQL"))
//                    adapter = new MSSQLDataAdapter(db, host, user, password, instance);
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
    
            context.delayUserInterfaction(new MessageClientAction(localize("{logics.upload.was.completed}"), localize("{logics.upload.db}")));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }

    }

}
