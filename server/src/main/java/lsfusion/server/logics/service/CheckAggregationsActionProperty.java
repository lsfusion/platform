package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CheckAggregationsActionProperty extends ScriptingActionProperty {
    public CheckAggregationsActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        String message = null;
        try {
            sqlSession.startTransaction();
            message = context.getDbManager().checkAggregations(sqlSession);
            sqlSession.commitTransaction();
        } catch(SQLException e) {
            sqlSession.rollbackTransaction();
        }

        context.delayUserInterfaction(new MessageClientAction(getString("logics.check.was.completed") + '\n' + '\n' + message, getString("logics.checking.aggregations"), true));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}