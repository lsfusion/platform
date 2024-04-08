package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.file.NamedFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class OpenFileAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface noWaitInterface;

    public OpenFileAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        noWaitInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            NamedFileData source = (NamedFileData) context.getKeyValue(sourceInterface).getValue();
            if (source != null) {
                OpenFileClientAction action = new OpenFileClientAction(source.getRawFile(), checkParamEmpty(source.getName()), checkParamEmpty(source.getExtension()));

                if (context.getKeyValue(noWaitInterface).getValue() != null)
                    context.delayUserInteraction(action);
                else
                    context.requestUserInteraction(action);
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String checkParamEmpty(String param) {
        return param != null && !param.equals("empty") ? param : null;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}