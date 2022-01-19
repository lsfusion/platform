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

public class OpenNamedFileAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;

    public OpenNamedFileAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            NamedFileData source = (NamedFileData) context.getDataKeyValue(sourceInterface).getValue();
            context.delayUserInteraction(new OpenFileClientAction(source.getRawFile(), source.getName(), source.getExtension()));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}