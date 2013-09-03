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

public class RecalculateClassesActionProperty extends ScriptingActionProperty {

    public RecalculateClassesActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        try {
            sqlSession.startTransaction();

            BusinessLogics BL = context.getBL();
            BL.recalculateClasses(sqlSession);
            context.getDbManager().packTables(sqlSession, BL.LM.tableFactory.getImplementTables());

            sqlSession.commitTransaction();
        } catch (SQLException e) {
            sqlSession.rollbackTransaction();
        }

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculating.data.classes"), true));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
