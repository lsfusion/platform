package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;

public class CalcPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, CalcProperty<P>> {

    public CalcPropertyClassImplement(CalcProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LCP createLP(ImOrderSet<ValueClassWrapper> listInterfaces) {
        return new LCP<P>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
