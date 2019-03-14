package lsfusion.server.physics.admin.service.task;

import lsfusion.interop.exception.ApplyCanceledException;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.action.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateFollowsTask extends GroupGraphTask<ActionProperty> {

    @Override
    protected Graph<ActionProperty> getGraph(DataSession session, BusinessLogics BL) {
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
            try (DataSession session = createSession()) {
                session.resolve(element, stack);
                session.applyException(getBL(), stack);
            }
        } catch (ApplyCanceledException e) { // suppress'им так как понятная ошибка
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
