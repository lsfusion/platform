package platform.server.logics.reflection;

import platform.server.classes.ValueClass;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class RecalculateStatsActionProperty extends ScriptingActionProperty {
    public RecalculateStatsActionProperty(ReflectionLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataSession session = context.getSession();
        context.getBL().recalculateStats(session);
        session.apply(context.getBL());
    }
}