package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputOrderEntity<P extends PropertyInterface, V extends PropertyInterface> {

    public final Property<P> property;

    public final ImRevMap<P, V> mapValues; // external context

    public InputOrderEntity(Property<P> property, ImRevMap<P, V> mapValues) {
        this.property = property;

        this.mapValues = mapValues;
        assert singleInterface() != null;
    }

    // input value
    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public PropertyMapImplement<P, V> getOrderProperty(V objectInterface) {
        return new PropertyMapImplement<>(property, mapValues.addRevExcl(singleInterface(), objectInterface));
    }

    public <C extends PropertyInterface> InputOrderEntity<P, C> map(ImRevMap<V, C> map) {
        return new InputOrderEntity<>(property, mapValues.join(map));
    }

    public <C extends PropertyInterface> InputOrderEntity<JoinProperty.Interface, C> createJoin(ImMap<V, PropertyInterfaceImplement<C>> mappedValues) {
        Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, C>> joinImplement = PropertyFact.createPartJoin(new PropertyImplement<>(property, mapValues.join(mappedValues)));
        return new InputOrderEntity<>(joinImplement.first, joinImplement.second);
    }
}
