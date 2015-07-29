package lsfusion.server.logics.service;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateStatsMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;

    public RecalculateStatsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ExecutorService executorService = null;
        try {
            Integer threadCount = (Integer) context.getKeyValue(threadCountInterface).getValue();
            if (threadCount == null || threadCount == 0)
                threadCount = BaseUtils.max(Runtime.getRuntime().availableProcessors() / 2, 1);

            final TaskPool taskPool = new TaskPool(context.getBL().LM.tableFactory.getImplementTables(), context.getBL().LM.baseClass.getUpObjectClassFields().values());
            final Context threadLocalContext = ThreadLocalContext.get();

            //Recalculate Tables
            executorService = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
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
                                        recalculateStats(context, session, table);
                                }
                                session.apply(context.getBL());
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate stats error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(12, TimeUnit.HOURS);

            //Recalculate Table Classes
            executorService = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ThreadLocalContext.get() == null)
                                ThreadLocalContext.set(threadLocalContext);
                            try (DataSession session = context.getDbManager().createSession()) {
                                while (!Thread.currentThread().isInterrupted() && taskPool.hasTableClasses()) {
                                    ObjectValueClassSet tableClass = taskPool.getTableClass();
                                    if (tableClass != null)
                                        recalculateClassStat(context, session, tableClass);
                                }
                                session.apply(context.getBL());
                            }
                        } catch (SQLException | SQLHandledException e) {
                            serviceLogger.error("Recalculate stats error", e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(12, TimeUnit.HOURS);

        } catch (Exception e) {
            serviceLogger.error("Recalculate stats error", e);
            if(executorService != null)
                executorService.shutdownNow();
        } finally {
            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();
        }
    }

    public void recalculateStats(ExecutionContext context, DataSession session, ImplementTable table) throws SQLException, SQLHandledException {
        long start = System.currentTimeMillis();
        serviceLogger.info(String.format("Recalculate Stats %s", table));
        table.calculateStat(context.getBL().reflectionLM, session);
        long time = System.currentTimeMillis() - start;
        serviceLogger.info(String.format("Recalculate Stats: %s, %sms", table, time));
    }

    public void recalculateClassStat(ExecutionContext context, DataSession session, ObjectValueClassSet tableClass) throws SQLException, SQLHandledException {
        long start = System.currentTimeMillis();
        serviceLogger.info(String.format("Recalculate Stats: %s", tableClass));
        QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));

        KeyExpr countKeyExpr = new KeyExpr("count");
        Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(context.getBL().LM.baseClass)),
                new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(tableClass), GroupType.SUM, classes.getMapExprs());

        classes.addProperty(0, countExpr);
        classes.and(countExpr.getWhere());

        ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
        ImSet<ConcreteCustomClass> concreteChilds = tableClass.getSetConcreteChildren();
        for (int i = 0, size = concreteChilds.size(); i < size; i++) {
            ConcreteCustomClass customClass = concreteChilds.get(i);
            ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, (Object) customClass.ID));
            context.getBL().LM.statCustomObjectClass.change(classStat == null ? 1 : (Integer) classStat.singleValue(), session, customClass.getClassObject());
        }
        long time = System.currentTimeMillis() - start;
        serviceLogger.info(String.format("Recalculate Stats: %s, %sms", tableClass, time));
    }

    public class TaskPool {
        int i, j;
        ImSet<ImplementTable> tables;
        ImCol<ObjectValueClassSet> tableClasses;

        public TaskPool(ImSet<ImplementTable> tables, ImCol<ObjectValueClassSet> tableClasses) {
            this.tables = tables;
            this.tableClasses = tableClasses;
            i = 0;
            j = 0;
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

        //метод, выдающий ObjectValueClassSet подпотокам
        synchronized ObjectValueClassSet getTableClass() {
            if (tableClasses.size() > j) {
                ObjectValueClassSet tableClass = tableClasses.get(j);
                j++;
                return tableClass;
            } else return null;
        }

        synchronized boolean hasTableClasses() {
            return j < tables.size();
        }
    }

}