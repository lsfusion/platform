package lsfusion.server.logics.tasks.impl;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.TableOwner;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateClassesTask extends GroupPropertiesSingleTask {
    public static int RECALC_TIL = -1;
    boolean singleTransaction;
    private Set<AggregateProperty> notRecalculateSet;
    Map<ImplementTable, List<CalcProperty>> calcPropertiesMap;
    private boolean groupByTables;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        super.init(context);
        this.singleTransaction = context.getBL().serviceLM.singleTransaction.read(context) != null;
        notRecalculateSet = context.getBL().getNotRecalculateAggregateStoredProperties();
        groupByTables = Settings.get().isGroupByTables();
    }

    @Override
    protected void runTask(final Object element) throws RecognitionException {
        String currentTask = String.format("Recalculate Class: %s", element);
        startedTask(currentTask);
        try (DataSession session = getDbManager().createSession()) {

            if (element instanceof Integer) {
                serviceLogger.info("Recalculate Exclusiveness");
                long start = System.currentTimeMillis();
                getBL().recalculateExclusiveness(session.sql, !singleTransaction);
                session.apply(getBL());
                long time = System.currentTimeMillis() - start;
                if(time > maxRecalculateTime)
                    addMessage("Recalculate Exclusiveness", time);
                serviceLogger.info(String.format("Recalculate Exclusiveness, %sms", time));
            } else if (element instanceof ImplementTable) {
                DBManager.run(session.sql, !singleTransaction, new DBManager.RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        serviceLogger.info(String.format("Recalculate Table Classes: %s", element));
                        long start = System.currentTimeMillis();
                        DataSession.recalculateTableClasses((ImplementTable) element, sql, session.env, getBL().LM.baseClass);
                        long time = System.currentTimeMillis() - start;
                        if (time > maxRecalculateTime)
                            addMessage(element, time);
                        serviceLogger.info(String.format("Recalculate Table Classes: %s, %sms", element, time));
                    }
                });

                serviceLogger.info(String.format("Pack table %s", element));
                long start = System.currentTimeMillis();
                run(session.sql, !singleTransaction, new RunService() {
                    @Override
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        sql.packTable((ImplementTable) element, session.env.getOpOwner(), TableOwner.global);
                    }
                });
                session.apply(getBL());
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Pack table: %s, %sms", element, time));

            } else if (element instanceof CalcProperty && !notRecalculateSet.contains(element)) {
                DBManager.run(session.sql, !singleTransaction, new DBManager.RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        serviceLogger.info(String.format("Recalculate Class: %s", ((CalcProperty) element).getSID()));
                        long start = System.currentTimeMillis();
                        ((CalcProperty) element).recalculateClasses(sql, session.env, getBL().LM.baseClass);
                        session.apply(getBL());
                        long time = System.currentTimeMillis() - start;
                        if(time > maxRecalculateTime)
                            addMessage(element, time);
                        serviceLogger.info(String.format("Recalculate Class: %s, %sms", ((CalcProperty) element).getSID(), time));
                    }
                });
            }
        } catch (SQLException | SQLHandledException e) {
            addMessage("Recalculate Class", element, e);
            serviceLogger.info(currentTask, e);
        } finally {
            finishedTask(currentTask);
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
        initContext();
        List elements = new ArrayList();
        elements.add(1);
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());

        List<CalcProperty> storedDataPropertiesList = getBL().getStoredDataProperties(true).toJavaList();
        if(groupByTables) {
            calcPropertiesMap = new HashMap<>();
            for (CalcProperty property : storedDataPropertiesList) {
                List<CalcProperty> entry = calcPropertiesMap.get(property.mapTable.table);
                if (entry == null)
                    entry = new ArrayList<>();
                entry.add(property);
                calcPropertiesMap.put(property.mapTable.table, entry);
            }
            for (Map.Entry<ImplementTable, List<CalcProperty>> entry : calcPropertiesMap.entrySet()) {
                java.util.Collections.sort(entry.getValue(), COMPARATOR);
            }
        }
        elements.addAll(storedDataPropertiesList);
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
        ImSet<Object> depends = SetFact.EMPTY();
        if(key instanceof CalcProperty && groupByTables) {
            List<CalcProperty> entry = calcPropertiesMap.get(((CalcProperty) key).mapTable);
            if(entry != null) {
                int index = entry.indexOf(key);
                if(index > 0)
                    depends.addExcl(entry.get(index - 1));
            }
        }
        return depends;
    }

    @Override
    protected long getTaskComplexity(Object element) {
        Stat stat;
        try {
            stat = element instanceof ImplementTable ? ((ImplementTable) element).getStatKeys().rows :
                    element instanceof CalcProperty ? ((CalcProperty) element).mapTable.table.getStatProps().get(((CalcProperty) element).field).notNull :
                            Stat.MAX;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }

    private static Comparator<CalcProperty> COMPARATOR = new Comparator<CalcProperty>() {
        public int compare(CalcProperty c1, CalcProperty c2) {
            return getNotNullWeight(c1) - getNotNullWeight(c2);
        }
    };

    private static int getNotNullWeight(CalcProperty c) {
        Stat stat;
        try {
            stat = c == null ? null : c.mapTable.table.getStatProps().get(c.field).notNull;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
