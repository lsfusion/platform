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

public class PackActionProperty extends ScriptingActionProperty {
    public PackActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        BusinessLogics BL = context.getBL();
        
        sqlSession.startTransaction();
        context.getDbManager().packTables(sqlSession, BL.LM.tableFactory.getImplementTables());
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}