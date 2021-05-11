package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.property.checked.ConstraintCheckChangeProperty;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// Constraint Check Change
// need this because form action with CONSTRAINTFILTER is created during parsing, and not in the end, and we need getCheckChangeProperties
// just like ClassFormSelector
public class CCCContextFilterEntity<P extends PropertyInterface, V extends PropertyInterface, O extends ObjectSelector> extends ContextFilterSelector<P, V, O> {

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

    public <C extends PropertyInterface> ContextFilterSelector<P, C, O> map(ImRevMap<V, C> map) {
        return new CCCContextFilterEntity<P, C, O>(propertyImplement.map(map), object);
    }

    public PropertyMapImplement<P, V> getWhereProperty(ImRevMap<O, V> objects) {
        return propertyImplement;
    }
}
