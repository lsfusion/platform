package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.linear.LAP;

public class ActionPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, ActionProperty<P>> {

    public ActionPropertyClassImplement(ActionProperty<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public ActionPropertyClassImplement(ActionProperty<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LAP<P> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        return new LAP<>(property, listInterfaces.mapOrder(mapping.reverse()));
    }

    public ActionPropertyClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new ActionPropertyClassImplement<P>(property, mapping.join(remap));
    }
}
