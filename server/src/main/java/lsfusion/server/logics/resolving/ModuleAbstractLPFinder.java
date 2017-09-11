package lsfusion.server.logics.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;

import java.util.List;

public class ModuleAbstractLPFinder extends ModulePropertyOrActionFinder<LP<?, ?>> {
    @Override
    protected List<LP<?, ?>> getSourceList(LogicsModule module, String name) {
        return module.getAllLPByName(name);
    }

    @Override
    protected boolean accepted(LogicsModule module, LP<?, ?> property, List<ResolveClassSet> signature) {
        return isAbstract(property.property) && 
                SignatureMatcher.isCompatible(module.getParamClasses(property), signature, false, false);
    }

    private boolean isAbstract(Property<?> property) {
        return property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract() ||
               property instanceof ListCaseActionProperty && ((ListCaseActionProperty) property).isAbstract();        
    } 
}
