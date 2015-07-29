package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.PublicTask;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateFollowsTask extends GroupPropertiesSingleTask{
    ExecutionContext context;
    boolean singleTransaction;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        this.context = context;
        this.singleTransaction = context.getBL().serviceLM.singleTransaction.read(context) != null;
        setBL(context.getBL());
        initTasks();
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected void runTask(final Property property) throws RecognitionException {
        try {
            if (property instanceof ActionProperty) {
                final ActionProperty<?> action = (ActionProperty) property;
                if (action.hasResolve()) {
                    long start = System.currentTimeMillis();
                    try {
                        getBL().getDbManager().runDataMultiThread(context, !singleTransaction, new DBManager.RunServiceData() {
                            public void run(SessionCreator session) throws SQLException, SQLHandledException {
                                ((DataSession) session).resolve(action);
                            }
                        });
                    } catch (LogMessageLogicsException e) { // suppress'им так как понятная ошибка
                        serviceLogger.info(e.getMessage());
                    }
                    long time = System.currentTimeMillis() - start;
                    serviceLogger.info(String.format("Recalculate Follows: %s, %sms", property.getSID(), time));
                }
            }
        } catch (SQLException | SQLHandledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List getElements() {
        return getBL().getPropertyList().toJavaList();
    }

    @Override
    protected String getErrorsDescription(Property element) {
        return "";
    }

    @Override
    protected ImSet<Property> getDependElements(Property key) {
        return SetFact.EMPTY();
    }
}
