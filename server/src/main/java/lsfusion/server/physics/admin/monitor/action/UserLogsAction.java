package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.UserLogsClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.external.to.file.ZipUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class UserLogsAction extends InternalAction {

    public UserLogsAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Map<String, RawFileData> logFiles = (Map<String, RawFileData>) context.requestUserInteraction(new UserLogsClientAction());
        if (logFiles != null && !logFiles.isEmpty()) {
            try {
                FileData zipFile = ZipUtils.makeZipFile(logFiles, false);
                try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                    ObjectValue currentConnection = findProperty("currentConnection[]").readClasses(newContext);
                    if (currentConnection instanceof DataObject) findProperty("fileUserLogs[Connection]").change(zipFile, newContext, (DataObject) currentConnection);
                    newContext.apply();
                } catch (ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}