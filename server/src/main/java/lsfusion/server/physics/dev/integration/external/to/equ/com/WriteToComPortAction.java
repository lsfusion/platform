package lsfusion.server.physics.dev.integration.external.to.equ.com;

import lsfusion.base.file.FileData;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.base.com.WriteToComPortClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class WriteToComPortAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface baudRateInterface;
    private final ClassPropertyInterface comPortInterface;
    private final ClassPropertyInterface useJsscInterface;

    public WriteToComPortAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        baudRateInterface = i.next();
        comPortInterface = i.next();
        useJsscInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        FileData file = (FileData) context.getKeyValue(fileInterface).getValue();
        Integer baudRate = (Integer) context.getKeyValue(baudRateInterface).getValue();
        Integer comPort = (Integer) context.getKeyValue(comPortInterface).getValue();
        boolean useJssc = context.getKeyValue(useJsscInterface).getValue() != null;

        if(file != null && baudRate != null && comPort != null) {
            String result = (String) context.requestUserInteraction(new WriteToComPortClientAction(file.getRawFile(), "COM" + comPort, baudRate, useJssc));
            if (result != null) {
                context.messageError(result);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
