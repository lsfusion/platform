package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class CalcPropertyClassImplement<P extends PropertyInterface> extends ActionOrPropertyClassImplement<P, Property<P>> {

    public CalcPropertyClassImplement(Property<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public CalcPropertyClassImplement(Property<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LP createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        Property<P> createProp = property;
        if(prev && property.noOld())
            createProp = createProp.getOld(PrevScope.DB); 
        return new LP<>(createProp, listInterfaces.mapOrder(mapping.reverse()));
    }

    public CalcPropertyClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new CalcPropertyClassImplement<P>(property, mapping.join(remap));
    }
}
