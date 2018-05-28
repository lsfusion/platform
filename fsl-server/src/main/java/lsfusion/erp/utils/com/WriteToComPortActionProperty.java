package lsfusion.erp.utils.com;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.WriteToComPortClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class WriteToComPortActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface baudRateInterface;
    private final ClassPropertyInterface comPortInterface;
    private final ClassPropertyInterface daemonInterface;

    public WriteToComPortActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        baudRateInterface = i.next();
        comPortInterface = i.next();
        daemonInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        byte[] file = (byte[]) context.getKeyValue(fileInterface).getValue();
        Integer baudRate = (Integer) context.getKeyValue(baudRateInterface).getValue();
        Integer comPort = (Integer) context.getKeyValue(comPortInterface).getValue();
        boolean daemon = context.getKeyValue(daemonInterface).getValue() != null;

        if(file != null && baudRate != null && comPort != null) {
            String result = (String) context.requestUserInteraction(new WriteToComPortClientAction(BaseUtils.getFile(file), baudRate, comPort, daemon));
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
