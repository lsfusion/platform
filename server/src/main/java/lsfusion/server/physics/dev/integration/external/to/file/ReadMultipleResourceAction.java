package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;

public class ReadMultipleResourceAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadMultipleResourceAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        resourcePathInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();
            int count = 0;
            Enumeration<URL> systemResources = ClassLoader.getSystemClassLoader().getResources(resourcePath);
            LP resource = findProperty("resource[INTEGER]");
            while (systemResources.hasMoreElements()) {
                RawFileData rawFileData = new RawFileData(IOUtils.toByteArray(systemResources.nextElement().openStream()));
                resource.change(new FileData(rawFileData, BaseUtils.getFileExtension(resourcePath)), context, new DataObject(count++));
            }
        } catch (ScriptingErrorLog.SemanticErrorException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
}