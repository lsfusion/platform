package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class RecalculateClassesTask extends GroupPropertiesSingleTask<Object> { // 1 - excl, ImplementTable
    public static int RECALC_TIL = -1;
    Map<ImplementTable, List<Property>> propertiesMap;
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
    protected void runInnerTask(final Object element, ExecutionStack stack) throws SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        if (element instanceof Integer) {
            getDbManager().recalculateClassesExclusiveness(sql, true);
        } else if (element instanceof ImplementTable) {
            DBManager.recalculateTableClasses((ImplementTable) element, sql, true, getBL().LM.baseClass);

            DBManager.packTable(sql, (ImplementTable) element, true);
        } else if (element instanceof Property) {
            ((Property) element).recalculateClasses(sql, true, getBL().LM.baseClass);
        }
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
            propertiesMap = new HashMap<>();
            for (Property property : storedDataPropertiesList) {
                List<Property> entry = propertiesMap.get(property.mapTable.table);
                if (entry == null)
                    entry = new ArrayList<>();
                entry.add(property);
                propertiesMap.put(property.mapTable.table, entry);
            }
            for (Map.Entry<ImplementTable, List<Property>> entry : propertiesMap.entrySet()) {
                entry.getValue().sort(COMPARATOR);
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
    protected ImSet<Object> getDependElements(Object key) {
        ImSet<Object> depends = SetFact.EMPTY();
        if(key instanceof Property && groupByTables) {
            List<Property> entry = propertiesMap.get(((Property) key).mapTable.table);
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

    private static Comparator<Property> COMPARATOR = Comparator.comparingInt(RecalculateClassesTask::getNotNullWeight);

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
