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

public class PackActionProperty extends ScriptingActionProperty {
    public PackActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        BusinessLogics BL = context.getBL();
        
        sqlSession.startTransaction();
        BL.packTables(sqlSession, BL.LM.tableFactory.getImplementTables());
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}