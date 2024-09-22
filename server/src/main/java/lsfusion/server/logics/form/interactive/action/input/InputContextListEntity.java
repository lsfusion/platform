package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.function.Function;

import static lsfusion.server.logics.property.oraction.PropertyInterface.getIdentityMap;

public interface InputContextListEntity<P extends PropertyInterface, V extends PropertyInterface> {

    boolean isNewSession();

    <C extends PropertyInterface> InputContextListEntity<P, C> map(ImRevMap<V, C> map);

    ImSet<V> getValues();

    default <C extends PropertyInterface> InputContextListEntity<P, C> mapInner(ImRevMap<V, C> map) {
        // here it's not evident if we should consider the case like FOR f=g(a) DO INPUT ... LIST x(d) IF g(d) = f as a simple input
        // we won't since we don't do that in FilterEntity, ContextFilterEntity.getInputListEntity
        ImSet<V> usedValues = getValues();

        ImRevMap<V, C> filteredMap = map.filterRev(usedValues);
        if(filteredMap.size() != usedValues.size())
            return null;
        return map(filteredMap);
    }

    default  <C extends PropertyInterface> InputContextListEntity<?, C> mapJoin(ImMap<V, PropertyInterfaceImplement<C>> mapping) {
        ImSet<V> usedValues = getValues();

        ImMap<V, PropertyInterfaceImplement<C>> filteredMap = mapping.filterIncl(usedValues);

        ImRevMap<V, C> identityMap = getIdentityMap(filteredMap);
        if(identityMap != null) // optimization
            return map(identityMap);

        return createJoin(filteredMap);
    }

    <C extends PropertyInterface> InputContextListEntity<?, C> createJoin(ImMap<V, PropertyInterfaceImplement<C>> mappedValues);

    ImMap<V, ValueClass> getInterfaceClasses();

    InputValueList<?> map(ImMap<V, ? extends ObjectValue> map, ImMap<V, PropertyObjectInterfaceInstance> mapObjects);

    default InputValueList<?> map(ExecutionContext<V> context) {
        ImMap<V, ? extends ObjectValue> values = context.getKeys();
        ImMap<V, PropertyObjectInterfaceInstance> objectInstances = context.getObjectInstances();
        if(objectInstances == null)
            objectInstances = BaseUtils.immutableCast(values);
        else
            objectInstances = MapFact.override(values, objectInstances);
        return map(values, objectInstances);
    }

    default InputValueList<?> map(ImMap<V, PropertyObjectInterfaceInstance> outerMapping, Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter) {
        return map(outerMapping.mapValues(valuesGetter), outerMapping);
    }

    InputContextListEntity<P, V> newSession();
}
