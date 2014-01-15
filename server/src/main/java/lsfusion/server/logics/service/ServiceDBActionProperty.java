package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ServiceDBActionProperty extends ScriptingActionProperty {
    public ServiceDBActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                context.getBL().recalculateClasses(session, isolatedTransaction);
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                context.getDbManager().recalculateAggregations(session, isolatedTransaction);
            }});

        run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                context.getDbManager().packTables(session, context.getBL().LM.tableFactory.getImplementTables(), isolatedTransaction);
            }});

        DataSession dataSession = context.getSession();
        SQLSession sqlSession = dataSession.sql;
        context.getDbManager().analyzeDB(sqlSession);

        context.getBL().recalculateFollows(dataSession);
        dataSession.apply(context);

        context.getBL().recalculateStats(dataSession);
        dataSession.apply(context);

        context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    public static void run(ExecutionContext<?> context, RunService run) throws SQLException, SQLHandledException {
        // транзакция в Service Action'ах не особо нужна, так как действия атомарные
        SQLSession sql = context.getSession().sql;
//        sql.startTransaction(DBManager.RECALC_TIL);
//        try {
            run.run(sql, true);
//        } catch (Exception e) {
//            sql.rollbackTransaction();
//            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
//        }
//        sql.commitTransaction();
    }
}