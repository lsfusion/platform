package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateFollowsActionProperty extends ScriptingActionProperty {
    public RecalculateFollowsActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }
    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataSession session = context.createSession();

        BusinessLogics BL = context.getBL();
        BL.recalculateFollows(session);
        session.apply(BL);
        session.close();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.follows")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}