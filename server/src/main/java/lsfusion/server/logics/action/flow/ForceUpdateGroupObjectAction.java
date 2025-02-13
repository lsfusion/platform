package lsfusion.server.logics.action.flow;

import lsfusion.interop.form.UpdateMode;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

public class ForceUpdateGroupObjectAction extends InternalAction {
    public ForceUpdateGroupObjectAction(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String groupObject = (String) context.getSingleDataKeyValue().getValue();
        for(GroupObjectInstance groupObjectInstance : context.getFormFlowInstance().getGroups()) {
            if(groupObjectInstance.getSID().equals(groupObject)) {
                groupObjectInstance.setUpdateMode(UpdateMode.FORCE);
                break;
            }
        }
    }
}
