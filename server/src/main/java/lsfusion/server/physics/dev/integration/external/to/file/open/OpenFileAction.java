package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class OpenFileAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;
    private final ClassPropertyInterface extensionInterface;
    private final ClassPropertyInterface noWaitInterface;

    public OpenFileAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
        extensionInterface = i.next();
        noWaitInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            ObjectValue sourceObject = context.getKeyValue(sourceInterface);
            RawFileData source = (RawFileData) sourceObject.getValue();

            if (sourceObject instanceof DataObject && source != null) {
                String extension = (String) context.getKeyValue(extensionInterface).getValue();

                OpenFileClientAction action = new OpenFileClientAction(source,
                        (String) context.getKeyValue(nameInterface).getValue(),
                        extension != null ? extension : BaseUtils.firstWord(((StaticFormatFileClass) ((DataObject) sourceObject).objectClass).getOpenExtension(source), ","));

                if (context.getKeyValue(noWaitInterface).getValue() != null)
                    context.delayUserInteraction(action);
                else
                    context.requestUserInteraction(action);
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}