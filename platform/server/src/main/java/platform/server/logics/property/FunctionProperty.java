package platform.server.logics.property;

import platform.server.session.Modifier;
import platform.server.session.Changes;

import java.util.Collection;
import java.util.Set;
import java.util.List;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    public static void fillDepends(Set<Property> depends, Collection<? extends PropertyInterfaceImplement> propImplements) {
        for(PropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return Property.getUsedChanges(getDepends(), modifier);
    }
}
