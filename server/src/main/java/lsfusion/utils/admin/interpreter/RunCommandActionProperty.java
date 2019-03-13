package lsfusion.utils.admin.interpreter;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.util.Iterator;

public class RunCommandActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface commandInterface;
    private final ClassPropertyInterface isClientInterface;

    public RunCommandActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        commandInterface = i.next();
        isClientInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String command = (String) context.getKeyValue(commandInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        if(command != null) {
            if (isClient) {
                String result = (String) context.requestUserInteraction(new RunCommandClientAction(command));
                if (result != null) {
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
                }
            } else {
                try {
                    Process p = Runtime.getRuntime().exec(command);
                    BufferedInputStream err = new BufferedInputStream(p.getErrorStream());
                    StringBuilder errS = new StringBuilder();
                    byte[] b = new byte[1024];
                    while (err.read(b) != -1) {
                        errS.append(new String(b, "cp866").trim()).append("\n");
                    }
                    err.close();
                    String result = errS.toString();
                    if (!result.isEmpty()) {
                        throw new RuntimeException(result);
                    }
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}