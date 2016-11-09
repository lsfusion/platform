package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ServiceDBActionProperty extends ScriptingActionProperty {
    public ServiceDBActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getBL().recalculateClasses(session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, getString("logics.service.db")));
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getDbManager().recalculateAggregations(context.stack, session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, getString("logics.service.db")));
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                context.getDbManager().packTables(session, context.getBL().LM.tableFactory.getImplementTables(), isolatedTransaction);
            }});

        SQLSession sqlSession = context.getSession().sql;
        context.getDbManager().analyzeDB(sqlSession);

        runData(context, new RunServiceData() {
            public void run(SessionCreator session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getBL().recalculateFollows(session, isolatedTransaction, context.stack);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, getString("logics.service.db")));
            }});

        DataSession dataSession = context.getSession();
        context.getBL().recalculateStats(dataSession);
        dataSession.apply(context);

        context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
    
    public static boolean singleTransaction(ExecutionContext context) throws SQLException, SQLHandledException {
        return context.getBL().serviceLM.singleTransaction.read(context)!=null;
    }

    public static void runData(ExecutionContext<?> context, final RunServiceData run) throws SQLException, SQLHandledException {
        final boolean singleTransaction = singleTransaction(context);
        DBManager.runData(context, singleTransaction, new DBManager.RunServiceData() {
            public void run(SessionCreator session) throws SQLException, SQLHandledException {
                run.run(session, !singleTransaction);
            }
        });
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
}