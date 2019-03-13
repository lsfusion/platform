package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.physics.dev.integration.external.to.file.client.FileExistsClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class FileExistsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface isClientInterface;

    public FileExistsActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

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