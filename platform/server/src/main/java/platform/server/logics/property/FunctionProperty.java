package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return modifier.getUsedChanges(getDepends());
    }
}
