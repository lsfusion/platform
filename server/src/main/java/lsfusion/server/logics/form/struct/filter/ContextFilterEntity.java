package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ContextFilterEntity<P extends PropertyInterface, V extends PropertyInterface, O extends ObjectSelector> extends ContextFilterSelector<V, O> {

    private final Property<P> property;

    private final ImRevMap<P, V> mapValues; // external context
    private final ImRevMap<P, O> mapObjects; // objects

    public ContextFilterEntity(Property<P> property, ImRevMap<P, V> mapValues, ImRevMap<P, O> mapObjects) {
        this.property = property;
        this.mapValues = mapValues;
        this.mapObjects = mapObjects;
    }

    @Override
    public ImSet<? extends ContextFilterEntity<?, V, O>> getEntities() {
        return SetFact.singleton(this);
    }

    public ContextFilterInstance getInstance(ImMap<V, ? extends ObjectValue> values, ImRevMap<O, ObjectEntity> objects) {
        return new ContextFilterInstance<>(property, mapValues.join(values), mapObjects.join(objects));
    }

    public <C extends PropertyInterface> ContextFilterEntity<P, C, O> map(ImRevMap<V, C> map) {
        return new ContextFilterEntity<>(property, mapValues.join(map), mapObjects);
    }

    public <C extends ObjectSelector> ContextFilterEntity<P, V, C> mapObjects(ImRevMap<O, C> map) {
        return new ContextFilterEntity<>(property, mapValues, mapObjects.join(map));
    }

    public PropertyMapImplement<?, V> getWhereProperty(ImRevMap<O, V> valueObjects) {
        ImRevMap<P, V> mappedObjects = mapObjects.innerJoin(valueObjects);
        ImRevMap<P, V> mapAllInterfaces = mapValues.addRevExcl(mappedObjects);
        if(mappedObjects.size() != mapObjects.size()) // we don't have all the interfaces, so we'll use the ExtendContextAction approach
            return IsClassProperty.getMapProperty(mapAllInterfaces.innerCrossJoin(property.getInterfaceClasses(ClassType.wherePolicy)));

        return new PropertyMapImplement<>(property, mapAllInterfaces);
    }

    public <T extends PropertyInterface> PropertyMapImplement<P, T> getWhereProperty(ImRevMap<V, T> values, ImRevMap<O, T> objects) {
        return new PropertyMapImplement<>(property, mapValues.join(values).addRevExcl(mapObjects.join(objects)));
    }

    public ImSet<O> getObjects() {
        return mapObjects.valuesSet();
    }

    public InputFilterEntity<?, V> getInputFilterEntity(O object, ImRevMap<O, V> mapping) {
        assert mapObjects.containsValue(object);
        assert !mapping.containsKey(object);
        // just like in InputListEntity.mapInner we will ignore the cases when there are not all objects
        ImRevMap<P, V> mappedObjects = mapObjects.innerJoin(mapping);
        if(mappedObjects.size() != mapObjects.size() - 1)
            return null;

        return new InputFilterEntity<>(property, mapValues.addRevExcl(mappedObjects));
    }
}
