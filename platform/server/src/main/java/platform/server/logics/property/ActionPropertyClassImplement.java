package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.logics.linear.LAP;

import java.util.List;

public class ActionPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, ActionProperty<P>> {

    public ActionPropertyClassImplement(ActionProperty<P> property, List<ValueClassWrapper> classes, List<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LAP<P> createLP(List<ValueClassWrapper> listInterfaces) {
        return new LAP<P>(property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(mapping)));
    }
}
