package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LAP;

public class ActionPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, ActionProperty<P>> {

    public ActionPropertyClassImplement(ActionProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public LAP<P> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        return new LAP<>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
