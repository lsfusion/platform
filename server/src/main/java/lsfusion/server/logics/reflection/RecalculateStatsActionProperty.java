package lsfusion.server.logics.reflection;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

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