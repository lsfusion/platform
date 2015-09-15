package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateFollowsTask extends GroupPropertiesSingleTask {
    ExecutionContext context;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        super.init(context);
        this.context = context;
    }

    @Override
    protected void runTask(final Object property) throws RecognitionException {
        String currentTask = String.format("Recalculate Follows: %s", property);
        startedTask(currentTask);
        try {
            if (property instanceof ActionProperty) {
                final ActionProperty<?> action = (ActionProperty) property;
                if (action.hasResolve()) {
                    long start = System.currentTimeMillis();
                    try {
                        try (DataSession session = getDbManager().createSession()) {
                            session.resolve(action);
                            session.apply(context);
                        }
                    } catch (LogMessageLogicsException e) { // suppress'им так как понятная ошибка
                        serviceLogger.info(e.getMessage());
                    }
                    long time = System.currentTimeMillis() - start;
                    if(time > maxRecalculateTime)
                        addMessage(property, time);
                    serviceLogger.info(String.format("Recalculate Follows: %s, %sms", ((ActionProperty) property).getSID(), time));
                }
            }
        } catch (SQLException | SQLHandledException e) {
            addMessage("Recalculate Follows", property, e);
            serviceLogger.info(currentTask, e);
        } finally {
            finishedTask(currentTask);
        }
    }

    @Override
    protected List getElements() {
        initContext();
        return getBL().getPropertyList().toJavaList();
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ActionProperty ? ((ActionProperty) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }
}
