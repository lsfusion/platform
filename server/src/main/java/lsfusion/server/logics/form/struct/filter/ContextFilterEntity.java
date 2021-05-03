package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.input.InputContextProperty;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ContextFilterEntity<P extends PropertyInterface, V extends PropertyInterface, O extends ObjectSelector> extends ContextFilterSelector<P, V, O> {

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

    public <C extends PropertyInterface> ContextFilterSelector<P, C, O> map(ImRevMap<V, C> map) {
        return new ContextFilterEntity<>(property, mapValues.join(map), mapObjects);
    }

    public <C extends ObjectSelector> ContextFilterEntity<P, V, C> mapObjects(ImRevMap<O, C> map) {
        return new ContextFilterEntity<>(property, mapValues, mapObjects.join(map));
    }

    public PropertyMapImplement<P, V> getWhereProperty(ImRevMap<O, V> objects) {
        return new PropertyMapImplement<>(property, mapValues.addRevExcl(mapObjects.join(objects)));
    }

    public ImSet<O> getObjects() {
        return mapObjects.valuesSet();
    }

    public InputContextProperty<?, V> getInputContextProperty(O object, ImRevMap<O, V> mapping) {
        assert mapObjects.containsValue(object);
        // just like in InputListEntity.mapInner we will ignore the cases when there are not all objects
        ImRevMap<P, V> mappedObjects = mapObjects.innerJoin(mapping);
        if(mappedObjects.size() != mapObjects.size() - 1)
            return null;

        return new InputContextProperty<>(property, mapValues.addRevExcl(mappedObjects));
    }
}
