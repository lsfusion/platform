package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateAggregationsTask extends GroupPropertiesSingleTask{
    private Set<AggregateProperty> notRecalculateSet;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        super.init(context);
        notRecalculateSet = context.getBL().getNotRecalculateAggregateStoredProperties();
    }

    @Override
    protected void runTask(final Object property) throws RecognitionException {
        if (property instanceof AggregateProperty && !notRecalculateSet.contains(property)) {
            String currentTask = String.format("Recalculate Aggregation: %s", property);
            startedTask(currentTask);
            try (DataSession session = getDbManager().createSession()) {
                long start = System.currentTimeMillis();
                serviceLogger.info(String.format("Recalculate Aggregation started: %s", ((AggregateProperty) property).getSID()));
                DBManager.run(session.sql, true, new DBManager.RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        ((AggregateProperty) property).recalculateAggregation(session.sql, session.env, getBL().LM.baseClass);
                    }
                });
                session.apply(getBL());
                long time = System.currentTimeMillis() - start;
                if (time > maxRecalculateTime)
                    addMessage(property, time);
                serviceLogger.info(String.format("Recalculate Aggregation: %s, %sms", ((AggregateProperty) property).getSID(), time));
            } catch (SQLException | SQLHandledException e) {
                addMessage("Recalculate Aggregation", property, e);
                serviceLogger.info(String.format("Recalculate Aggregation: %s", property), e);
            } finally {
                finishedTask(currentTask);
            }
        }
    }

    @Override
    protected List getElements() {
        initContext();
        return getBL().getAggregateStoredProperties(false);
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof AggregateProperty ? ((AggregateProperty) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return getDepends((CalcProperty) key);
    }

    protected ImSet<Object> getDepends(CalcProperty key) {
        ImSet<Object> depends = SetFact.EMPTY();
        for (CalcProperty property : (Iterable<CalcProperty>) key.getDepends()) {
            if (property instanceof AggregateProperty && property.isStored() && !depends.contains(property))
                depends = depends.addExcl(property);
            else {
                ImSet<Object> children = getDepends(property);
                for(Object child : children)
                    if(!depends.contains(child) && child != null)
                        depends = depends.addExcl(child);
            }
        }
        return depends;
    }

    @Override
    protected long getTaskComplexity(Object element) {
        Stat stat;
        try {
            stat = element instanceof AggregateProperty ?
                    ((AggregateProperty) element).mapTable.table.getStatProps().get(((AggregateProperty) element).field).notNull : null;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
