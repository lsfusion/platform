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

public class AnalyzeDBActionProperty extends ScriptingActionProperty {
    public AnalyzeDBActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        BusinessLogics BL = context.getBL();

        DataSession session = context.createSession();

        context.getDbManager().analyzeDB(session.sql);

        session.apply(BL);
        session.close();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.vacuum.analyze.was.completed"), getString("logics.vacuum.analyze")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}