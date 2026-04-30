package lsfusion.server.physics.admin.runtime;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ReadServerRuntimeOptions extends InternalAction {

    public ReadServerRuntimeOptions(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Runtime runtime = Runtime.getRuntime();
        try {
            findProperty("serverAvailableProcessors[]").change(runtime.availableProcessors(), context);
            findProperty("serverFreeMemoryMB[]").change(toMB(runtime.freeMemory()), context);
            findProperty("serverTotalMemoryMB[]").change(toMB(runtime.totalMemory()), context);
            findProperty("serverMaxMemoryMB[]").change(toMB(runtime.maxMemory()), context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private long toMB(long bytes) {
        return bytes / (1024L * 1024L);
    }
}
