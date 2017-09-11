package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.*;

public class RecalculateClassesTask extends GroupPropertiesSingleTask<Object> { // 1 - excl, ImplementTable
    public static int RECALC_TIL = -1;
    Map<ImplementTable, List<CalcProperty>> calcPropertiesMap;
    private boolean groupByTables;

    public RecalculateClassesTask() {
        groupByTables = Settings.get().isGroupByTables();
    }

    @Override
    public String getTaskCaption(Object element) {
        if (element instanceof Integer) {
            return "Recalculate Exclusiveness";
        } else if (element instanceof ImplementTable) {
            return "Recalculate Table Classes \\ Pack Table";
        }
        assert element instanceof CalcProperty;
        return "Recalculate Class";
    }

    @Override
    protected void runInnerTask(final Object element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        if (element instanceof Integer) {
            getBL().recalculateExclusiveness(sql, true);
        } else if (element instanceof ImplementTable) {
            DBManager.run(sql, true, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    DataSession.recalculateTableClasses((ImplementTable) element, sql, getBL().LM.baseClass);
                }
            });

            run(sql, new RunService() {
                @Override
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    sql.packTable((ImplementTable) element, OperationOwner.unknown, TableOwner.global);
                }
            });
        } else if (element instanceof CalcProperty) {
            DBManager.run(sql, true, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    ((CalcProperty) element).recalculateClasses(sql, getBL().LM.baseClass);
                }
            });
        }
    }

    public interface RunService {
        void run(SQLSession sql) throws SQLException, SQLHandledException;
    }

    public static void run(SQLSession session, RunService run) throws SQLException, SQLHandledException {
        run(session, run, 0);
    }

    private static void run(SQLSession session, RunService run, int attempts) throws SQLException, SQLHandledException {
        session.startTransaction(RECALC_TIL, OperationOwner.unknown);
        try {
            run.run(session);
            session.commitTransaction();
        } catch (Throwable t) {
            session.rollbackTransaction();
            if (t instanceof SQLHandledException && ((SQLHandledException) t).repeatApply(session, OperationOwner.unknown, attempts)) { // update conflict или deadlock или timeout - пробуем еще раз
                run(session, run, attempts + 1);
                return;
            }
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }

    @Override
    protected List getElements() {
        checkContext();
        List elements = new ArrayList();
        elements.add(1);
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());

        List<CalcProperty> storedDataPropertiesList = getBL().getStoredDataProperties().toJavaList();
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
            stat = element instanceof ImplementTable ? ((ImplementTable) element).getStatRows() :
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
