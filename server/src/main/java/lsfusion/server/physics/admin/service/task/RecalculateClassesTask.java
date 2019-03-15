package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.TableOwner;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.*;

public class RecalculateClassesTask extends GroupPropertiesSingleTask<Object> { // 1 - excl, ImplementTable
    public static int RECALC_TIL = -1;
    Map<ImplementTable, List<Property>> calcPropertiesMap;
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
        assert element instanceof Property;
        return "Recalculate Class";
    }

    @Override
    protected void runInnerTask(final Object element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        if (element instanceof Integer) {
            getDbManager().recalculateExclusiveness(sql, true);
        } else if (element instanceof ImplementTable) {
            DBManager.run(sql, true, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    DataSession.recalculateTableClasses((ImplementTable) element, sql, getBL().LM.baseClass);
                }
            });

            run(sql, new DBManager.RunService() {
                @Override
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    sql.packTable((ImplementTable) element, OperationOwner.unknown, TableOwner.global);
                }
            });
        } else if (element instanceof Property) {
            DBManager.run(sql, true, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    ((Property) element).recalculateClasses(sql, getBL().LM.baseClass);
                }
            });
        }
    }

    public static void run(SQLSession session, DBManager.RunService run) throws SQLException, SQLHandledException {
        DBManager.run(session, true, run);
    }

    @Override
    protected List getElements() {
        checkContext();
        List elements = new ArrayList();
        elements.add(1);
        List<Property> storedDataPropertiesList;
        try(DataSession session = createSession()) {
            elements.addAll(getBL().LM.tableFactory.getImplementTables(getDbManager().getDisableClassesTableSet(session)).toJavaSet());
            storedDataPropertiesList = getBL().getStoredDataProperties(session).toJavaList();
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        if(groupByTables) {
            calcPropertiesMap = new HashMap<>();
            for (Property property : storedDataPropertiesList) {
                List<Property> entry = calcPropertiesMap.get(property.mapTable.table);
                if (entry == null)
                    entry = new ArrayList<>();
                entry.add(property);
                calcPropertiesMap.put(property.mapTable.table, entry);
            }
            for (Map.Entry<ImplementTable, List<Property>> entry : calcPropertiesMap.entrySet()) {
                java.util.Collections.sort(entry.getValue(), COMPARATOR);
            }
        }
        elements.addAll(storedDataPropertiesList);
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof Property ? ((Property) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        ImSet<Object> depends = SetFact.EMPTY();
        if(key instanceof Property && groupByTables) {
            List<Property> entry = calcPropertiesMap.get(((Property) key).mapTable.table);
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
                    element instanceof Property ? ((Property) element).mapTable.table.getStatProps().get(((Property) element).field).notNull :
                            Stat.MAX;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }

    private static Comparator<Property> COMPARATOR = new Comparator<Property>() {
        public int compare(Property c1, Property c2) {
            return getNotNullWeight(c1) - getNotNullWeight(c2);
        }
    };

    private static int getNotNullWeight(Property c) {
        Stat stat;
        try {
            stat = c == null ? null : c.mapTable.table.getStatProps().get(c.field).notNull;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
