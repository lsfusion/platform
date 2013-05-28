package platform.server.logics.service;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class CheckAggregationsActionProperty extends ScriptingActionProperty {
    public CheckAggregationsActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        sqlSession.startTransaction();
        String message = context.getDbManager().checkAggregations(sqlSession);
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.check.aggregation.was.completed") + '\n' + '\n' + message, getString("logics.checking.aggregations"), true));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}