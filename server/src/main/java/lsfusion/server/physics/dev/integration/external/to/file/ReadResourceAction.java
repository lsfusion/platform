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
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;

public class ReadResourceAction extends InternalAction {

    public ReadResourceAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String resourcePath = (String) getParam(0, context);
            boolean fullPaths = getParam(1, context) != null;

            RawFileData rawFileData;
            String extension;
            String path;
            if(fullPaths) {
                rawFileData = new RawFileData(IOUtils.toByteArray(new URL(resourcePath).openStream()));
                extension = BaseUtils.getFileExtension(resourcePath);
                path = resourcePath;
            } else {
                Result<String> fullPath = new Result<>();
                rawFileData = ResourceUtils.findResourceAsFileData(resourcePath, true, true, fullPath, null);
                extension = BaseUtils.getFileExtension(fullPath.result);
                path = fullPath.result;
            }

            findProperty("resource[]").change(rawFileData != null ? new FileData(rawFileData, extension) : null, context);
            findProperty("resourcePath[]").change(path, context);

        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}