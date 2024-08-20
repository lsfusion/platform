package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputActionListEntity<P extends PropertyInterface, V extends PropertyInterface> extends InputListEntity<P, V, Action<P>> {

    public InputActionListEntity(Action<P> property, ImRevMap<P, V> mapValues) {
        this(property, mapValues, false);
    }
    public InputActionListEntity(Action<P> property, ImRevMap<P, V> mapValues, boolean newSession) {
        super(property, mapValues, newSession);
    }

    public InputActionListEntity<P, V> newSession() {
        return new InputActionListEntity<>(property, mapValues, true);
    }

    protected  <C extends PropertyInterface> InputActionListEntity<P, C> create(ImRevMap<P, C> joinMapValues) {
        return new InputActionListEntity<>(property, joinMapValues, newSession);
    }

    protected  <C extends PropertyInterface> InputActionListEntity<PropertyInterface, C> createJoin(ImMap<P, PropertyInterfaceImplement<C>> mappedValues) {
        Pair<Action<PropertyInterface>, ImRevMap<PropertyInterface, C>> joinImplement = PropertyFact.createPartJoinAction(new ActionImplement<>(property, mappedValues));
        return new InputActionListEntity<>(joinImplement.first, joinImplement.second, newSession);
    }

    public InputActionValueList<P> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects) {
        return new InputActionValueList<>(property, BaseUtils.immutableCast(mapValues.join(map)));
    }
}
