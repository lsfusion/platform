package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.session.StructChanges;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    public static void fillDepends(Set<Property> depends, Collection<? extends PropertyInterfaceImplement> propImplements) {
        for(PropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    public QuickSet<Property> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return propChanges.getUsedChanges(getDepends(), cascade);
    }
}
