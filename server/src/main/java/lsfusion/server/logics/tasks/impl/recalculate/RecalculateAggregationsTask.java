package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.tasks.GroupGraphTask;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateAggregationsTask extends GroupGraphTask<AggregateProperty> {

    @Override
    public String getTaskCaption(AggregateProperty element) {
        return "Recalculate Aggregation";
    }

    @Override
    protected void runInnerTask(final AggregateProperty element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        serviceLogger.info(String.format("Recalculate Aggregation started: %s", element.getSID()));
        DBManager.run(sql, true, new DBManager.RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                element.recalculateAggregation(sql, getBL().LM.baseClass);
            }
        });
    }

    @Override
    protected Graph<AggregateProperty> getGraph(BusinessLogics<?> BL) {
        return BL.getRecalculateAggregateStoredGraph();
    }

    @Override
    protected String getElementCaption(AggregateProperty element) {
        return element.getSID();
    }

    @Override
    protected String getErrorsDescription(AggregateProperty element) {
        return "";
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
