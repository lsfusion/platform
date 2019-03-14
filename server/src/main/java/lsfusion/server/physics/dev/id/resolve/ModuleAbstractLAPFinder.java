package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.action.flow.ListCaseActionProperty;

public class ModuleAbstractLAPFinder extends ModuleAbstractLPFinder<LAP<?>> {
    @Override
    protected Iterable<LAP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedActions(name);
    }    

    @Override
    protected boolean isAbstract(ActionOrProperty property) {
        assert property instanceof ActionProperty;
        return property instanceof ListCaseActionProperty && ((ListCaseActionProperty) property).isAbstract();
    }
}
