package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.admin.service.RunServiceData;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ServiceDBAction extends InternalAction {
    public ServiceDBAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        run(context, (session, isolatedTransaction) -> {
            String result = context.getDbManager().recalculateClasses(session, isolatedTransaction);
            if(result != null)
                context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
        });

        run(context, (session, isolatedTransaction) -> {
            String result = context.getDbManager().recalculateMaterializations(context.stack, session, isolatedTransaction);
            if(result != null)
                context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
        });

        run(context, (session, isolatedTransaction) -> context.getDbManager().packTables(session, context.getBL().LM.tableFactory.getImplementTables(), isolatedTransaction));

        SQLSession sqlSession = context.getSession().sql;
        context.getDbManager().analyzeDB(sqlSession);

        runData(context, (session, isolatedTransaction) -> {
            String result = context.getBL().recalculateFollows(session, isolatedTransaction, context.stack);
            if(result != null)
                context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.service.db}")));
        });

        context.getDbManager().recalculateStats(context.getSession());
        context.apply();

        context.delayUserInterfaction(new MessageClientAction(localize("{logics.service.db.completed}"), localize("{logics.service.db}")));
    }
    
    public static boolean singleTransaction(ExecutionContext context) throws SQLException, SQLHandledException {
        return context.getBL().serviceLM.singleTransaction.read(context)!=null;
    }

    public static void runData(ExecutionContext<?> context, final RunServiceData run) throws SQLException, SQLHandledException {
        final boolean singleTransaction = singleTransaction(context);
        DBManager.runData(context, singleTransaction, session -> run.run(session, !singleTransaction));
    }

    public static void run(ExecutionContext<?> context, final RunService run) throws SQLException, SQLHandledException {
        // транзакция в Service Action'ах не особо нужна, так как действия атомарные
        final boolean singleTransaction = singleTransaction(context); 
        SQLSession sql = context.getSession().sql;
        DBManager.run(sql, singleTransaction, sql1 -> run.run(sql1, !singleTransaction));
    }
}