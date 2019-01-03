package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class DeleteActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface isClientInterface;

    public DeleteActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String sourcePath = (String) context.getKeyValue(sourceInterface).getValue();
            boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
            if (sourcePath != null) {
                if (isClient) {
                    String result = (String) context.requestUserInteraction(new DeleteFileClientAction(sourcePath));
                    if (result != null)
                        throw new RuntimeException(result);
                } else {
                    FileUtils.delete(sourcePath);
                }
            }
        } catch (IOException | JSchException | SftpException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}