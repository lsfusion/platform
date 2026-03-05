package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.interop.action.RunCommandActionResult;
import lsfusion.interop.action.RunCommandClientAction;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class RunCommandAction extends InternalAction {
    private final ClassPropertyInterface commandInterface;
    private final ClassPropertyInterface directoryInterface;
    private final ClassPropertyInterface isClientInterface;
    private final ClassPropertyInterface waitInterface;

    public RunCommandAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        commandInterface = i.next();
        directoryInterface = i.next();
        isClientInterface = i.next();
        waitInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String command = (String) context.getKeyValue(commandInterface).getValue();
        String directory = (String) context.getKeyValue(directoryInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        boolean wait = context.getKeyValue(waitInterface).getValue() != null;
        if(command != null) {
            try {
                RunCommandActionResult result = isClient
                        ? (RunCommandActionResult) context.requestUserInteraction(new RunCommandClientAction(command, directory, wait))
                        : SystemUtils.runCmd(command, directory, wait);

                if (result != null) {
                    findProperty("cmdOut[]").change(result.getCmdOut(), context);
                    findProperty("cmdErr[]").change(result.getCmdErr(), context);

                    if (!result.isCompletedSuccessfully())
                        throw new RuntimeException(result.getErrorMessage());
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}