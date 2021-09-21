package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public interface InputFilterSelector<V extends PropertyInterface> {

    InputFilterEntity<?, V> getEntity();

    <C extends PropertyInterface> InputFilterSelector<C> map(ImRevMap<V, C> map);

}
