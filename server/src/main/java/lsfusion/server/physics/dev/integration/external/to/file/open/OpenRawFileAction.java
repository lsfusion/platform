package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class OpenRawFileAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;
    private final ClassPropertyInterface noWaitInterface;

    public OpenRawFileAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
        noWaitInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            ObjectValue sourceObject = context.getKeyValue(sourceInterface);
            RawFileData source = (RawFileData) sourceObject.getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();
            boolean noWait = context.getKeyValue(noWaitInterface).getValue() != null;

            if (sourceObject instanceof DataObject && source != null) {
                String extension = BaseUtils.firstWord(((StaticFormatFileClass) ((DataObject) sourceObject).objectClass).getExtension(source), ",");
                OpenFileClientAction action = new OpenFileClientAction(source, name, extension);
                if (noWait) {
                    context.delayUserInteraction(action);
                } else {
                    context.requestUserInteraction(action);
                }
            }


        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return false;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}