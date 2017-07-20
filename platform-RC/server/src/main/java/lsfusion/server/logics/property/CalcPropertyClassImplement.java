package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.linear.LCP;

public class CalcPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, CalcProperty<P>> {

    public CalcPropertyClassImplement(CalcProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public CalcPropertyClassImplement(CalcProperty<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LCP createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        CalcProperty<P> createProp = property;
        if(prev && property.noOld())
            createProp = createProp.getOld(PrevScope.DB); 
        return new LCP<>(createProp, listInterfaces.mapOrder(mapping.reverse()));
    }

    public CalcPropertyClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new CalcPropertyClassImplement<P>(property, mapping.join(remap));
    }
}
