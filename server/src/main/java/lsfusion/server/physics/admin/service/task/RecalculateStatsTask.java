package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecalculateStatsTask extends GroupPropertiesSingleTask<Object> { // ImplementTable, ObjectValueClassSet

    @Override
    public String getTaskCaption(Object element) {
        return "Recalculate Stats";
    }

    @Override
    protected void runInnerTask(Object element, ExecutionStack stack) throws SQLException, SQLHandledException {
        try (DataSession session = createSession()) {
            if (element instanceof ImplementTable) {
                ((ImplementTable) element).recalculateStat(getBL().reflectionLM, getDbManager().getDisableStatsTableColumnSet(), session, null);
            } else if (element instanceof ObjectValueClassSet) {
                ((ObjectValueClassSet) element).recalculateClassStat(getBL().LM, session, null);
            }
            session.applyException(getBL(), stack);
        }
    }

    @Override
    protected List<Object> getElements() {
        checkContext();
        Set<String> notRecalculateStatsTableSet;
        try(DataSession session = createSession()) {
            notRecalculateStatsTableSet = getDbManager().getDisableStatsTableSet(session);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        List<Object> elements = new ArrayList<>();
        for (ImplementTable table : getBL().LM.tableFactory.getImplementTables(notRecalculateStatsTableSet)) {
            elements.add(table);

            ObjectValueClassSet classSet = table.getClassDataSet();
            if(classSet != null)
                elements.add(classSet);
        }
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof ObjectValueClassSet ? String.valueOf(element) : null;
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }

    @Override
    protected long getTaskComplexity(Object element) {
        if (element instanceof ImplementTable) {
            Stat stat = ((ImplementTable) element).getStatRows();
            return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
        } else if (element instanceof ObjectValueClassSet)
            return ((ObjectValueClassSet) element).getCount();
        else
            return Stat.MIN.getWeight();
    }
}
