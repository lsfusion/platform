package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.tasks.GroupGraphTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateFollowsTask extends GroupGraphTask<ActionProperty> {

    @Override
    protected Graph<ActionProperty> getGraph(BusinessLogics<?> BL) {
        return BL.getRecalculateFollowsGraph();
    }

    @Override
    public String getTaskCaption(ActionProperty element) {
        return "Recalculate Follows";
    }

    @Override
    protected void runInnerTask(ActionProperty element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        assert element.hasResolve();

        try {
            try (DataSession session = getDbManager().createSession()) {
                session.resolve(element, stack);
                session.apply(getBL(), stack);
            }
        } catch (LogMessageLogicsException e) { // suppress'им так как понятная ошибка
            serviceLogger.info(e.getMessage());
        }
    }

    @Override
    protected String getElementCaption(ActionProperty element) {
        return element.getSID();
    }

    @Override
    protected String getErrorsDescription(ActionProperty element) {
        return "";
    }
}
