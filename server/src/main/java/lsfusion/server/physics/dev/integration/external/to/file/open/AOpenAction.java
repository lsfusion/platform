package lsfusion.server.physics.dev.integration.external.to.file.open;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

public abstract class AOpenAction extends InternalAction {
    public AOpenAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return false;
        return super.hasFlow(type);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}