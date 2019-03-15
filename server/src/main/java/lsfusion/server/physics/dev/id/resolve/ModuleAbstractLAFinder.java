package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class ModuleAbstractLAFinder extends ModuleAbstractLAPFinder<LA<?>> {
    @Override
    protected Iterable<LA<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }    

    @Override
    protected boolean isAbstract(ActionOrProperty property) {
        assert property instanceof Action;
        return property instanceof ListCaseAction && ((ListCaseAction) property).isAbstract();
    }
}
