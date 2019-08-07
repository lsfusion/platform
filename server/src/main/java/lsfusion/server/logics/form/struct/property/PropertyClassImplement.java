package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyClassImplement<P extends PropertyInterface> extends ActionOrPropertyClassImplement<P, Property<P>> {

    public PropertyClassImplement(Property<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public PropertyClassImplement(Property<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LP createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        Property<P> createProp = actionOrProperty;
        if(prev && actionOrProperty.noOld())
            createProp = createProp.getOld(PrevScope.DB); 
        return new LP<>(createProp, listInterfaces.mapOrder(mapping.reverse()));
    }

    public PropertyClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new PropertyClassImplement<>(actionOrProperty, mapping.join(remap));
    }
}
