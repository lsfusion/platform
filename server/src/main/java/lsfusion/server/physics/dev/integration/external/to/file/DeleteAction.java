package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.base.file.DeleteFileClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class DeleteAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface isClientInterface;

    public DeleteAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
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
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}