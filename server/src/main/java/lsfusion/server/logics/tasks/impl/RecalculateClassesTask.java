package lsfusion.server.logics.tasks.impl;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.TableOwner;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.PublicTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateClassesTask extends GroupPropertiesSingleTask {
    public static int RECALC_TIL = -1;
    boolean singleTransaction;
    boolean exclusiveness = false;
    private final Object lock = new Object();

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        this.singleTransaction = context.getBL().serviceLM.singleTransaction.read(context) != null;
        setBL(context.getBL());
        initTasks();
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected void runTask(final Object element) throws RecognitionException {
        try {
            final SQLSession sqlSession = getBL().getDbManager().getThreadLocalSql();

            if (!exclusiveness) {
                synchronized (lock) {
                    exclusiveness = true;
                    getBL().recalculateExclusiveness(sqlSession, !singleTransaction);
                }
            } else if (element instanceof ImplementTable) {
                DBManager.run(sqlSession, !singleTransaction, new DBManager.RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        long start = System.currentTimeMillis();
                        DataSession.recalculateTableClasses((ImplementTable) element, sql, getBL().LM.baseClass);
                        long time = System.currentTimeMillis() - start;
                        serviceLogger.info(String.format("Recalculate Table Classes: %s, %s", String.valueOf(element), time));
                    }
                });

                serviceLogger.info(getString("logics.info.packing.table") + " (" + element + ")... ");
                run(sqlSession, !singleTransaction, new RunService() {
                    @Override
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        sql.packTable((ImplementTable) element, OperationOwner.unknown, TableOwner.global);
                    }
                });
                serviceLogger.info("Done");

            } else if (element instanceof CalcProperty) {
                DBManager.run(sqlSession, !singleTransaction, new DBManager.RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        long start = System.currentTimeMillis();
                        ((CalcProperty) element).recalculateClasses(sql, getBL().LM.baseClass);
                        long time = System.currentTimeMillis() - start;
                        serviceLogger.info(String.format("Recalculate Class: %s, %s", ((CalcProperty) element).getSID(), time));
                    }
                });
            }
        } catch (SQLException | SQLHandledException e) {
            e.printStackTrace();
        }
    }

    public interface RunService {
        void run(SQLSession sql) throws SQLException, SQLHandledException;
    }

    public static void run(SQLSession session, boolean runInTransaction, RunService run) throws SQLException, SQLHandledException {
        run(session, runInTransaction, run, 0);
    }

    private static void run(SQLSession session, boolean runInTransaction, RunService run, int attempts) throws SQLException, SQLHandledException {
        if (runInTransaction) {
            session.startTransaction(RECALC_TIL, OperationOwner.unknown);
            try {
                run.run(session);
                session.commitTransaction();
            } catch (Throwable t) {
                session.rollbackTransaction();
                if (t instanceof SQLHandledException && ((SQLHandledException) t).repeatApply(session, OperationOwner.unknown, attempts)) { // update conflict или deadlock или timeout - пробуем еще раз
                    run(session, true, run, attempts + 1);
                    return;
                }

                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }

        } else
            run.run(session);
    }

    @Override
    protected List getElements() {
        List elements = new ArrayList();
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());
        elements.addAll(getBL().getStoredDataProperties(true).toJavaList());
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof CalcProperty ? ((CalcProperty) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }
}
