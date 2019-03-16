package lsfusion.server.physics.admin.logging.action;

import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;


public class PrintToLogAction extends InternalAction {
    private final ClassPropertyInterface stringInterface;

    public PrintToLogAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        ServerLoggers.systemLogger.info(context.getDataKeyValue(stringInterface).object);
    }
}