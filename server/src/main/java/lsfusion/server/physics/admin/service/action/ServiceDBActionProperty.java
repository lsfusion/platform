package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.admin.service.RunServiceData;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.controller.init.SessionCreator;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ServiceDBActionProperty extends ScriptingAction {
    public ServiceDBActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getDbManager().recalculateClasses(session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getDbManager().recalculateAggregations(context.stack, session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
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
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
            }});

        context.getDbManager().recalculateStats(context.getSession());
        context.apply();

        context.delayUserInterfaction(new MessageClientAction(localize("{logics.service.db.completed}"), localize("{logics.service.db}")));
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