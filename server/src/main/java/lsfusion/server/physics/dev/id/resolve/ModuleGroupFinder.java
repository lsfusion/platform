package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.struct.group.Group;

public class ModuleGroupFinder extends ModuleSingleElementFinder<Group, Object> {
    @Override
    protected Group getElement(LogicsModule module, String simpleName, Object param) {
        return module.getGroup(simpleName);
    }
}
