package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.logics.linear.LAP;

public class ActionPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, ActionProperty<P>> {

    public ActionPropertyClassImplement(ActionProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LAP<P> createLP(ImOrderSet<ValueClassWrapper> listInterfaces) {
        return new LAP<P>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
