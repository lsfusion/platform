package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.logics.linear.LCP;

public class CalcPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, CalcProperty<P>> {

    public CalcPropertyClassImplement(CalcProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LCP createLP(ImOrderSet<ValueClassWrapper> listInterfaces) {
        return new LCP<P>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
