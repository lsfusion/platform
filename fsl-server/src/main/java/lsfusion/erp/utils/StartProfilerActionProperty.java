package lsfusion.erp.utils;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.profiler.Profiler;

import java.sql.SQLException;

public class StartProfilerActionProperty extends ScriptingActionProperty {
    public StartProfilerActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Profiler.PROFILER_ENABLED = true;   
    }
}
