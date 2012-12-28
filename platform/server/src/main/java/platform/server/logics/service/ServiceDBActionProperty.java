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
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class ServiceDBActionProperty extends ScriptingActionProperty {
    public ServiceDBActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        BusinessLogics BL = context.getBL();
        
        sqlSession.startTransaction();
        BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
        sqlSession.commitTransaction();

        BL.recalculateFollows(context.getSession());

        sqlSession.startTransaction();
        BL.packTables(sqlSession, BL.LM.tableFactory.getImplementTables());
        sqlSession.commitTransaction();

        BL.analyzeDB(sqlSession);

        BL.recalculateStats(context.getSession());
        context.getSession().apply(BL);

        context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}