package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;

public class ReadResourcesAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadResourcesAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourcePathInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();
            URL resource = ResourceUtils.getResource(resourcePath);
            File[] files = resource != null ? new File(ResourceUtils.getResource(resourcePath).getPath()).listFiles() : null; //тут нулл
            if (files != null) {
                for (File file : files) {
                    RawFileData fileData = new RawFileData(file);
                    findProperty("resourceFiles[STRING]").change(fileData, context, new DataObject(file.getName(), StringClass.get(20))) ;
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
