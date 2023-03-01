package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class ReadResourceAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadResourceAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        resourcePathInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();

            Result<String> fullPath = new Result<>();
            RawFileData rawFileData = ResourceUtils.findResourceAsFileData(resourcePath, true, true, fullPath, null);

            findProperty("resource[]").change(rawFileData != null ? new FileData(rawFileData, BaseUtils.getFileExtension(fullPath.result)) : null, context);
            findProperty("resourcePath[]").change(fullPath.result, context);

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

}