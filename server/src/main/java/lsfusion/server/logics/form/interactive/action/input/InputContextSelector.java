package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public interface InputContextSelector<V extends PropertyInterface> {

    Pair<InputFilterEntity<?, V>, ImOrderMap<InputOrderEntity<?, V>, Boolean>> getFilterAndOrders();

    <C extends PropertyInterface> InputContextSelector<C> map(ImRevMap<V, C> map);

}
