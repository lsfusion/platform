package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class ReadProfilingSettingsActionProperty extends ScriptingActionProperty {
    public ReadProfilingSettingsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            Settings settings = Settings.get();

            findProperty("explainNoAnalyze[]").change(settings.isExplainNoAnalyze() ? true : null, context);
            findProperty("explainJavaStack[]").change(settings.isExplainJavaStack() ? true : null, context);
            findProperty("explainCompile[]").change(settings.isExplainCompile() ? true : null, context);
            findProperty("explainThreshold[]").change(settings.getExplainThreshold(), context);

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
    }
}