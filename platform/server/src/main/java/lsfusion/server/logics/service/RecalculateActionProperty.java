package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateActionProperty extends ScriptingActionProperty {
    public RecalculateActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        BusinessLogics BL = context.getBL();
        SQLSession sqlSession = context.getSession().sql;

        sqlSession.startTransaction();
        context.getDbManager().recalculateAggregations(sqlSession);
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
    }
}