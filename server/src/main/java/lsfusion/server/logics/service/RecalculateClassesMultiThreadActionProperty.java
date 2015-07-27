package lsfusion.server.logics.service;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.TableOwner;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateClassesMultiThreadActionProperty extends ScriptingActionProperty {
    public static int RECALC_TIL = -1;

    public RecalculateClassesMultiThreadActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        int threadsCount = BaseUtils.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        ExecutorService executorService = null;

        final boolean singleTransaction = singleTransaction(context);

        try {
            final TaskPool taskPool = new TaskPool(context.getBL().LM.tableFactory.getImplementTables(), context.getBL().getStoredDataProperties(true));
            final Context threadLocalContext = ThreadLocalContext.get();

            //Recalculate Table Classes
            executorService = Executors.newFixedThreadPool(threadsCount);
            for (int i = 0; i < threadsCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasTables()) {
                                    ImplementTable table = taskPool.getTable();
                                    if (table != null)
                                        recalculateTableClasses(table, session, !singleTransaction);
                                }
                                session.apply(context);
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate Classes error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(8, TimeUnit.HOURS);

            //Recalculate Property Classes
            executorService = Executors.newFixedThreadPool(threadsCount);
            for (int i = 0; i < threadsCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasProperties()) {
                                    CalcProperty property = taskPool.getProperty();
                                    if (property != null)
                                        recalculatePropertyClasses(property, session, !singleTransaction);
                                }
                                session.apply(context);
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate Classes error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(8, TimeUnit.HOURS);

            //Pack Tables
            executorService = Executors.newFixedThreadPool(threadsCount);
            for (int i = 0; i < threadsCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasPackTables()) {
                                    ImplementTable table = taskPool.getPackTable();
                                    if (table != null)
                                        packTable(table, session, !singleTransaction);
                                }
                                session.apply(context);
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate Classes error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(8, TimeUnit.HOURS);

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculating.data.classes")));
        } catch (InterruptedException e) {
            serviceLogger.error("Recalculate Classes error", e);
        } finally {
            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();
        }
    }


    public void recalculateTableClasses(final ImplementTable table, DataSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        DBManager.run(session.sql, isolatedTransactions, new DBManager.RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                long start = System.currentTimeMillis();
                DataSession.recalculateTableClasses(table, sql, LM.baseLM.baseClass);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Table Classes: %s, %s", table.toString(), time));
            }
        });
    }

    public void recalculatePropertyClasses(final CalcProperty property, DataSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        DBManager.run(session.sql, isolatedTransactions, new DBManager.RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                long start = System.currentTimeMillis();
                property.recalculateClasses(sql, LM.baseLM.baseClass);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Class: %s, %s", property.getSID(), time));
            }
        });
    }

    public void packTable(final ImplementTable table, DataSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        serviceLogger.info(getString("logics.info.packing.table") + " (" + table + ")... ");
        run(session.sql, isolatedTransaction, new RunService() {
            @Override
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                sql.packTable(table, OperationOwner.unknown, TableOwner.global);
            }
        });
        serviceLogger.info("Done");
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

    public interface RunService {
        void run(SQLSession sql) throws SQLException, SQLHandledException;
    }

    public static boolean singleTransaction(ExecutionContext context) throws SQLException, SQLHandledException {
        return context.getBL().serviceLM.singleTransaction.read(context) != null;
    }

    public class TaskPool {
        int i, j, k;
        ImSet<ImplementTable> tables;
        ImOrderSet<CalcProperty> storedDataProperties;

        public TaskPool(ImSet<ImplementTable> tables, ImOrderSet<CalcProperty> storedDataProperties) {
            this.tables = tables;
            this.storedDataProperties = storedDataProperties;
            i = 0;
            j = 0;
            k = 0;
        }

        //метод, выдающий ImplementTable подпотокам
        synchronized ImplementTable getTable() {
            if (tables.size() > i) {
                ImplementTable table = tables.get(i);
                i++;
                return table;
            } else return null;
        }

        synchronized boolean hasTables() {
            return i < tables.size();
        }

        //метод, выдающий CalcProperty подпотокам
        synchronized CalcProperty getProperty() {
            if (storedDataProperties.size() > j) {
                CalcProperty property = storedDataProperties.get(j);
                j++;
                return property;
            } else return null;
        }

        synchronized boolean hasProperties() {
            return j < storedDataProperties.size();
        }

        //метод, выдающий ImplementTable для pack подпотокам
        synchronized ImplementTable getPackTable() {
            if (tables.size() > k) {
                ImplementTable table = tables.get(k);
                k++;
                return table;
            } else return null;
        }

        synchronized boolean hasPackTables() {
            return k < tables.size();
        }
    }

}