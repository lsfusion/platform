package platform.server.logics.properties;

import platform.server.session.DataChanges;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String iSID, Collection<T> iInterfaces) {
        super(iSID, iInterfaces);
    }

    protected abstract void fillDepends(Set<Property> depends);

    public static void fillDepends(Set<Property> depends, Collection<? extends PropertyInterfaceImplement> propImplements) {
        for(PropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    <C extends DataChanges<C>,U extends UsedChanges<C,U>> U calculateUsedChanges(C changes, Collection<DataProperty> usedDefault, Depends<C, U> depends) {
        Set<Property> dependProps = new HashSet<Property>();
        fillDepends(dependProps);
        return Property.getUsedChanges(dependProps,changes, usedDefault, depends);
    }
}
