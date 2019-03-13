package lsfusion.server.language.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.Property;

public class ModuleAbstractLCPFinder extends ModuleAbstractLPFinder<LCP<?>> {
    
    @Override
    protected boolean isAbstract(Property property) {
        assert property instanceof CalcProperty;
        return property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract();
    }

    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
