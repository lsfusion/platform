package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.struct.FormEntity;

public class ModuleFormFinder extends ModuleSingleElementFinder<FormEntity, Object> {
    @Override
    protected FormEntity getElement(LogicsModule module, String simpleName, Object param) {
        return module.getForm(simpleName);
    }
}
