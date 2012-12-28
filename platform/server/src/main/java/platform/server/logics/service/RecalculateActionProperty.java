package platform.server.logics.service;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class RecalculateActionProperty extends ScriptingActionProperty {
    public RecalculateActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        BusinessLogics BL = context.getBL();
        SQLSession sqlSession = context.getSession().sql;

        sqlSession.startTransaction();
        BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}