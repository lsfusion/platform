package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;

import java.util.List;

public class CalcPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, CalcProperty<P>> {

    public CalcPropertyClassImplement(CalcProperty<P> property, List<ValueClassWrapper> classes, List<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LCP createLP(List<ValueClassWrapper> listInterfaces) {
        return new LCP<P>(property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(mapping)));
    }
}
