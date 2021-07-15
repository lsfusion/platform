package lsfusion.server.physics.admin.service.action;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

public class SetRepeatableReadDisableTILModeAction extends InternalAction {

    public SetRepeatableReadDisableTILModeAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Object value = context.getSingleKeyObject();
        DBManager.DISABLE_SESSION_TIL = value != null;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}