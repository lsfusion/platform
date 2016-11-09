package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ServiceDBMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;
    ServiceLogicsModule serviceLM;
    public ServiceDBMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.serviceLM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        final ObjectValue threadCountObject = context.getKeyValue(threadCountInterface);

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                serviceLM.recalculateMultiThreadAction.execute(context, threadCountObject);
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                serviceLM.recalculateClassesMultiThreadAction.execute(context, threadCountObject);
            }
        });

        context.getDbManager().analyzeDB(context.getSession().sql);

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                serviceLM.recalculateFollowsMultiThreadAction.execute(context, threadCountObject);
            }
        });

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                serviceLM.recalculateStatsMultiThreadAction.execute(context, threadCountObject);
            }
        });

        context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    public static void run(ExecutionContext<?> context, final RunService run) throws SQLException, SQLHandledException {
        // транзакция в Service Action'ах не особо нужна, так как действия атомарные
        final boolean singleTransaction = singleTransaction(context); 
        SQLSession sql = context.getSession().sql;
        DBManager.run(sql, singleTransaction, new DBManager.RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                run.run(sql, !singleTransaction);
            }});
    }

    public static boolean singleTransaction(ExecutionContext context) throws SQLException, SQLHandledException {
        return context.getBL().serviceLM.singleTransaction.read(context)!=null;
    }
}