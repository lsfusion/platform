package lsfusion.server.language.resolving;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.LogicsModule;

public class ModuleFormFinder extends ModuleSingleElementFinder<FormEntity, Object> {
    @Override
    protected FormEntity getElement(LogicsModule module, String simpleName, Object param) {
        return module.getForm(simpleName);
    }
}
