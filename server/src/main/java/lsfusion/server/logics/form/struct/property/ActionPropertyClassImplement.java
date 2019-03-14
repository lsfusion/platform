package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.linear.LA;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionPropertyClassImplement<P extends PropertyInterface> extends PropertyClassImplement<P, Action<P>> {

    public ActionPropertyClassImplement(Action<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public ActionPropertyClassImplement(Action<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LA<P> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        return new LA<>(property, listInterfaces.mapOrder(mapping.reverse()));
    }

    public ActionPropertyClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new ActionPropertyClassImplement<P>(property, mapping.join(remap));
    }
}
