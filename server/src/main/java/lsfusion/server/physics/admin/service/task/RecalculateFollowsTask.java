package lsfusion.server.physics.admin.service.task;

import lsfusion.interop.exception.ApplyCanceledException;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.cases.graph.Graph;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateFollowsTask extends GroupGraphTask<Action> {

    @Override
    protected Graph<Action> getGraph(DataSession session, BusinessLogics BL) {
        return BL.getRecalculateFollowsGraph();
    }

    @Override
    public String getTaskCaption(Action element) {
        return "Recalculate Follows";
    }

    @Override
    protected void runInnerTask(Action element, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
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
    protected String getElementCaption(Action element) {
        return element.getSID();
    }

    @Override
    protected String getErrorsDescription(Action element) {
        return "";
    }
}
