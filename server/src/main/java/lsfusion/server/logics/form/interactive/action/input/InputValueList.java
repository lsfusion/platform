package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputValueList<P extends PropertyInterface> {

    public final Property<P> property;

    public final ImMap<P, ObjectValue> mapValues; // external context

    public InputValueList(Property<P> property, ImMap<P, ObjectValue> mapValues) {
        this.property = property;

        this.mapValues = mapValues;
    }

    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }
}
