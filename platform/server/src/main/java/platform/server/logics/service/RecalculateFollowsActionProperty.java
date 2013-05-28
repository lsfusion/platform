package platform.server.logics.service;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.session.DataSession;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

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