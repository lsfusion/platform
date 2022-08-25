package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.Property;
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

}
