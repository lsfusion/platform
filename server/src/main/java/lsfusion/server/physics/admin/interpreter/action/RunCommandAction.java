package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class RunCommandAction extends InternalAction {
    private final ClassPropertyInterface commandInterface;
    private final ClassPropertyInterface directoryInterface;
    private final ClassPropertyInterface isClientInterface;

    public RunCommandAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        commandInterface = i.next();
        directoryInterface = i.next();
        isClientInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String command = (String) context.getKeyValue(commandInterface).getValue();
        String directory = (String) context.getKeyValue(directoryInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        if(command != null) {
            if (isClient) {
                String result = (String) context.requestUserInteraction(new RunCommandClientAction(command, directory));
                if (result != null) {
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
                }
            } else {
                try {
                    String result = FileUtils.runCmd(command, directory);
                    if (result != null) {
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