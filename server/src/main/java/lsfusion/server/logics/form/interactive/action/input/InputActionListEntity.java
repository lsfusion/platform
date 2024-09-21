package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputActionListEntity<P extends PropertyInterface, V extends PropertyInterface> extends InputListEntity<P, V, Action<P>> implements InputContextListEntity<P, V> {

    public InputActionListEntity(Action<P> property, ImRevMap<P, V> mapValues) {
        this(property, mapValues, false);
    }
    public InputActionListEntity(Action<P> property, ImRevMap<P, V> mapValues, boolean newSession) {
        super(property, mapValues, newSession);
    }

    @Override
    public boolean isNewSession() {
        return newSession;
    }

    @Override
    public ImSet<V> getValues() {
        return mapValues.valuesSet();
    }

    public InputActionListEntity<P, V> newSession() {
        return new InputActionListEntity<>(property, mapValues, true);
    }

    public <C extends PropertyInterface> InputActionListEntity<P, C> create(ImRevMap<P, C> joinMapValues) {
        return new InputActionListEntity<>(property, joinMapValues, newSession);
    }

    public <C extends PropertyInterface> InputActionListEntity<PropertyInterface, C> createJoin(ImMap<V, PropertyInterfaceImplement<C>> mappedValues) {
        Pair<Action<PropertyInterface>, ImRevMap<PropertyInterface, C>> joinImplement = PropertyFact.createPartJoinAction(new ActionImplement<>(property, mapValues.join(mappedValues)));
        return new InputActionListEntity<>(joinImplement.first, joinImplement.second, newSession);
    }

    public InputActionValueList<?> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects) {
        return new InputActionValueList<>(property, BaseUtils.immutableCast(mapValues.join(map)));
    }

    @Override
    public <C extends PropertyInterface> InputActionListEntity<P, C> map(ImRevMap<V, C> map) {
        return (InputActionListEntity<P, C>) super.map(map);
    }

    public ImMap<V, ValueClass> getInterfaceClasses() {
        return mapValues.innerCrossJoin(property.getInterfaceClasses(ClassType.wherePolicy));
    }
}
