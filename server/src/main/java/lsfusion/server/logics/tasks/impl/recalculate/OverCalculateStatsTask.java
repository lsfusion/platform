package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;

public class OverCalculateStatsTask extends GroupPropertiesSingleTask {
    MSet<Integer> propertiesSet;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        super.init(context);
    }

    @Override
    protected void runTask(final Object element) throws RecognitionException {
        if(element instanceof ImplementTable) {
            String currentTask = String.format("Recalculate Stats: %s", element);
            startedTask(currentTask);
            try (DataSession session = getDbManager().createSession()) {
                long start = System.currentTimeMillis();
                serviceLogger.info(currentTask);
                ((ImplementTable) element).overCalculateStat(getBL().reflectionLM, session, propertiesSet, null/*progressBar*/);
                session.apply(getBL());
                long time = System.currentTimeMillis() - start;
                if (time > maxRecalculateTime)
                    addMessage(element, time);
                serviceLogger.info(String.format("%s, %sms", currentTask, time));
            } catch (SQLException | SQLHandledException e) {
                addMessage("Recalculate Stats:", element, e);
                serviceLogger.info(currentTask, e);
            } finally {
                finishedTask(currentTask);
            }
        }
    }

    @Override
    protected List getElements() {
        initContext();
        try (DataSession session = getDbManager().createSession()) {
            Integer maxQuantity = (Integer) getBL().serviceLM.findProperty("maxQuantityOverCalculate[]").read(session);
            propertiesSet = getBL().getOverCalculatePropertiesSet(session, maxQuantity);
        } catch (Exception e) {
            propertiesSet = SetFact.mSet();
        }
        return new ArrayList(getBL().LM.tableFactory.getImplementTables().toJavaSet());
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }

    @Override
    protected long getTaskComplexity(Object element) {
        if (element instanceof ImplementTable) {
            Stat stat = ((ImplementTable) element).getStatKeys().rows;
            return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
        } else
            return Stat.MIN.getWeight();
    }
}