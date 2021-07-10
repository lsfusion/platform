package lsfusion.server.physics.admin.service.action;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.adapter.PostgreDataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class UploadToDBAction extends InternalAction {

    public UploadToDBAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            String type = (String) findProperty("uploadStaticNameType[]").read(context);
            String host = (String) findProperty("uploadHost[]").read(context);
            Integer port = (Integer) findProperty("uploadPort[]").read(context);
            String user = (String) findProperty("uploadUser[]").read(context);
            String password = (String) findProperty("uploadPassword[]").read(context);
            String db = (String) findProperty("uploadDB[]").read(context);
            String instance = (String) findProperty("uploadInstance[]").read(context);
    
            final DataAdapter adapter;
            try {
                if(type.trim().equals("Service_DBType.POSTGRE"))
                    adapter = new PostgreDataAdapter(db, host, port, user, password);
//                else if(type.trim().equals("Service_DBType.MSSQL"))
//                    adapter = new MSSQLDataAdapter(db, host, user, password, instance);
                else
                    throw new UnsupportedOperationException();
            } catch (Exception e) {
                throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
            }
    
            ServiceDBAction.run(context, (session, isolatedTransaction) -> {
                try {
                    context.getDbManager().uploadToDB(session, isolatedTransaction, adapter);
                } catch (Exception e) {
                    throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
                }
            });
    
            context.delayUserInterfaction(new MessageClientAction(localize("{logics.upload.was.completed}"), localize("{logics.upload.db}")));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }

    }

}
