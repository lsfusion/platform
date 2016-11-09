package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

public class OverCalculateStatsTask extends GroupPropertiesSingleTask<ImplementTable> {
    MSet<Integer> propertiesSet;

    @Override
    public String getTaskCaption(ImplementTable element) {
        return "Recalculate Stats";
    }

    @Override
    protected void runInnerTask(ImplementTable element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        try (DataSession session = getDbManager().createSession()) {
            element.overCalculateStat(getBL().reflectionLM, session, propertiesSet, null/*progressBar*/);
            session.apply(getBL(), stack);
        }
    }

    @Override
    protected List<ImplementTable> getElements() {
        checkContext();
        try (DataSession session = getDbManager().createSession()) {
            Integer maxQuantity = (Integer) getBL().serviceLM.findProperty("maxQuantityOverCalculate[]").read(session);
            propertiesSet = getBL().getOverCalculatePropertiesSet(session, maxQuantity);
        } catch (Exception e) {
            propertiesSet = SetFact.mSet();
        }
        return getBL().LM.tableFactory.getImplementTables().toOrderSet().toJavaList();
    }

    @Override
    protected String getElementCaption(ImplementTable element) {
        return element.getName();
    }

    @Override
    protected String getErrorsDescription(ImplementTable element) {
        return "";
    }

    @Override
    protected ImSet<ImplementTable> getDependElements(ImplementTable key) {
        return SetFact.EMPTY();
    }

    @Override
    protected long getTaskComplexity(ImplementTable element) {
        Stat stat = ((ImplementTable) element).getStatKeys().rows;
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}