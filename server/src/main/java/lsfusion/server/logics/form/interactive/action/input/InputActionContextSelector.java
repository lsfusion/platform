package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputActionContextSelector<F extends PropertyInterface, V extends PropertyInterface> implements InputContextSelector<V> {

    private final InputFilterEntity<F, V> filter;

    public InputActionContextSelector(InputFilterEntity<F, V> filter) {
        this.filter = filter;
    }

    @Override
    public Pair<InputFilterEntity<?, V>, ImOrderMap<InputOrderEntity<?, V>, Boolean>> getFilterAndOrders() {
        return new Pair<>(filter, MapFact.EMPTYORDER());
    }

    @Override
    public <C extends PropertyInterface> InputContextSelector<C> map(ImRevMap<V, C> map) {
        return new InputActionContextSelector<F, C>(filter != null ? filter.map(map) : null);
    }
}
