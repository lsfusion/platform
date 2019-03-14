package lsfusion.server.physics.dev.integration.external.to.equ.com;

import lsfusion.base.file.FileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class WriteToComPortActionProperty extends ScriptingAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface baudRateInterface;
    private final ClassPropertyInterface comPortInterface;

    public WriteToComPortActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        baudRateInterface = i.next();
        comPortInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FileData file = (FileData) context.getKeyValue(fileInterface).getValue();
        Integer baudRate = (Integer) context.getKeyValue(baudRateInterface).getValue();
        Integer comPort = (Integer) context.getKeyValue(comPortInterface).getValue();

        if(file != null && baudRate != null && comPort != null) {
            String result = (String) context.requestUserInteraction(new WriteToComPortClientAction(file.getRawFile(), baudRate, comPort));
            if (result != null) {
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
