package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class ToggleExplainAnalyzeModeActionProperty extends ScriptingActionProperty {

    public ToggleExplainAnalyzeModeActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getSession().sql.toggleExplainAnalyzeMode();
    }
}
