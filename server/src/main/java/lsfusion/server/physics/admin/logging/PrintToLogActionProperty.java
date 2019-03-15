package lsfusion.server.physics.admin.logging;

import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.Iterator;


public class PrintToLogActionProperty extends ScriptingAction {
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