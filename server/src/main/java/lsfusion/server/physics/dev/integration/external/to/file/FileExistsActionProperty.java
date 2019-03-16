package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.physics.dev.integration.external.to.file.client.FileExistsClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class FileExistsActionProperty extends InternalAction {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface isClientInterface;

    public FileExistsActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String path = (String) context.getKeyValue(pathInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        try {
            context.getSession().dropChanges((DataProperty) findProperty("fileExists[]").property);
            if (path != null) {
                boolean exists;
                if (isClient) {
                    exists = (boolean) context.requestUserInteraction(new FileExistsClientAction(path));
                } else {
                    exists = FileUtils.checkFileExists(path);
                }
                findProperty("fileExists[]").change(exists ? true : null, context);
            } else {
                throw new RuntimeException("FileExists Error. Path not specified.");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}