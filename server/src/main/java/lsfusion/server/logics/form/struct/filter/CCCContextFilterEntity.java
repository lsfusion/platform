package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// Constraint Check Change
// need this because form action with CONSTRAINTFILTER is created during parsing, and not in the end, and we need getCheckChangeProperties
// just like ClassFormSelector
public class CCCContextFilterEntity<P extends PropertyInterface, V extends PropertyInterface, O extends ObjectSelector> extends ContextFilterSelector<V, O> {

    private final PropertyMapImplement<P, V> propertyImplement;
    private final O object;

    public CCCContextFilterEntity(PropertyMapImplement<P, V> propertyImplement, O object) {
        this.propertyImplement = propertyImplement;
        this.object = object;
    }

    @Override
    public ImSet<? extends ContextFilterEntity<?, V, O>> getEntities() {
        return propertyImplement.property.getCheckFilters(object).mapSetValues(filter -> filter.map(propertyImplement.mapping));
    }

    public <C extends PropertyInterface> ContextFilterSelector<C, O> map(ImRevMap<V, C> map) {
        return new CCCContextFilterEntity<>(propertyImplement.map(map), object);
    }

    public PropertyMapImplement<P, V> getWhereProperty(ImRevMap<O, V> objects) {
        return propertyImplement;
    }
}
