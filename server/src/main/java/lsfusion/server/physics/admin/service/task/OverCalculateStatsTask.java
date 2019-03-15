package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

public class OverCalculateStatsTask extends GroupPropertiesSingleTask<ImplementTable> {
    MSet<Long> propertiesSet;

    @Override
    public String getTaskCaption(ImplementTable element) {
        return "Recalculate Stats";
    }

    @Override
    protected void runInnerTask(ImplementTable element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        try (DataSession session = createSession()) {
            element.overCalculateStat(getBL().reflectionLM, session, propertiesSet, getDbManager().getDisableStatsTableColumnSet(), null/*progressBar*/);
            session.applyException(getBL(), stack);
        }
    }

    @Override
    protected List<ImplementTable> getElements() {
        checkContext();
        try (DataSession session = createSession()) {
            Integer maxQuantity = (Integer) getBL().serviceLM.findProperty("maxQuantityOverCalculate[]").read(session);
            propertiesSet = getBL().getOverCalculatePropertiesSet(session, maxQuantity);
            return getBL().LM.tableFactory.getImplementTables(getDbManager().getDisableStatsTableSet(session)).toOrderSet().toJavaList();
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
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
        Stat stat = ((ImplementTable) element).getStatRows();
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}