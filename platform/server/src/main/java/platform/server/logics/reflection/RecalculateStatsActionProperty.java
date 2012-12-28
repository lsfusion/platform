package platform.server.logics.reflection;

import platform.server.classes.ValueClass;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class RecalculateStatsActionProperty extends ScriptingActionProperty {
    public RecalculateStatsActionProperty(ReflectionLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getBL().recalculateStats(context.getSession());
    }
}