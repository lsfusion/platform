package lsfusion.server.physics.admin.service.task;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateAggregationsTask extends GroupGraphTask<AggregateProperty> {

    @Override
    public String getTaskCaption(AggregateProperty element) {
        return "Recalculate Aggregation";
    }

    @Override
    protected void runInnerTask(final AggregateProperty element, ExecutionStack stack) throws SQLException, SQLHandledException {
        try (final DataSession session = createSession()) {
            serviceLogger.info(String.format("Recalculate Aggregation started: %s", element.getSID()));
            DBManager.run(session.sql, true, sql -> element.recalculateAggregation(getBL(), session, sql, getBL().LM.baseClass));
            session.applyException(getBL(), stack);
        }
    }

    @Override
    protected Graph<AggregateProperty> getGraph(DataSession session, BusinessLogics BL) throws SQLException, SQLHandledException {
        return BL.getRecalculateAggregateStoredGraph(session);
    }

    @Override
    protected String getElementCaption(AggregateProperty element) {
        return element.getSID();
    }

    @Override
    protected long getTaskComplexity(AggregateProperty element) {
        Stat stat;
        try {
            stat = element.mapTable.table.getStatProps().get(element.field).notNull;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
