package lsfusion.server.physics.admin.logging;

import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.util.Iterator;


public class PrintToLogActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface stringInterface;

    public PrintToLogActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        ServerLoggers.systemLogger.info(context.getDataKeyValue(stringInterface).object);
    }
}