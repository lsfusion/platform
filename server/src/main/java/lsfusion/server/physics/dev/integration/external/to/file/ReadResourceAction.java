package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ResourceUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ReadResourceAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadResourceAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourcePathInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();

            InputStream stream = null;
            //same as in FormReportManager.findCustomReportFileName
            if (resourcePath.startsWith("/")) {
                //absolute path
                stream = ResourceUtils.getResourceAsStream(resourcePath);
            } else {
                //relative path
                Pattern pattern = Pattern.compile("/.*" + resourcePath);
                Collection<String> allResources = ResourceUtils.getResources(pattern);

                for (String entry : allResources) {
                    if (entry.endsWith("/" + resourcePath)) {
                        stream = getClass().getResourceAsStream(entry);
                        break;
                    }
                }
            }

            findProperty("resourceFile[]").change(stream != null ? new FileData(new RawFileData(IOUtils.readBytesFromStream(stream)), BaseUtils.getFileExtension(resourcePath)) : null, context);

        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
}