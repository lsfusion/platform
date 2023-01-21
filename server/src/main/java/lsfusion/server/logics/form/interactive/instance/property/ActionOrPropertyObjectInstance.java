package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ActionOrPropertyObjectInstance<P extends PropertyInterface, T extends ActionOrProperty<P>> extends TwinImmutableObject {

    public T property;
    public ImMap<P, PropertyObjectInterfaceInstance> mapping;

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionOrPropertyObjectInstance) o).property) && mapping.equals(((ActionOrPropertyObjectInstance) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public ActionOrPropertyObjectInstance(T property, ImMap<P,? extends PropertyObjectInterfaceInstance> mapping) {
        this.property = property;
        this.mapping = (ImMap<P, PropertyObjectInterfaceInstance>) mapping;

        ImMap<P, ? extends PropertyObjectInterfaceInstance> mapObjects;
        // used in PropertyObjectInstance.getInputValueList for example
        assert (mapObjects = mapping.filterFnValues(element -> element instanceof ObjectInstance)).toRevMap().size() == mapObjects.size();
    }
    
    public abstract ActionOrPropertyObjectInstance<P, ?> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues, boolean fullKey);

    // получает GRID в котором рисоваться
    public GroupObjectInstance getApplyObject() {
        GroupObjectInstance applyObject=null;
        for(ObjectInstance intObject : getObjectInstances())
            if(applyObject==null || intObject.groupTo.order >applyObject.order)
                applyObject = intObject.getApplyObject();

        return applyObject;
    }

    public ImCol<ObjectInstance> getObjectInstances() {
        return BaseUtils.immutableCast(mapping.values().filterCol(element -> element instanceof ObjectInstance));
    }

    public void fillObjects(MSet<ObjectInstance> objects) {
        objects.addAll(getObjectInstances().toSet());
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        // assert что classGroups все в GRID представлении
        ImMap<P, AndClassSet> classImplement = mapping.mapValues(value -> value.getClassSet(classGroups));
        return property.isInInterface(classImplement, any);
    }

    public ImMap<P, ObjectValue> getInterfaceObjectValues() {
        return mapping.mapValues(PropertyObjectInterfaceInstance::getObjectValue);
    }

    // in filter and orders columnKey is the COLUMNS objects, and for the execute event all the objects on the form
    protected ImMap<P, PropertyObjectInterfaceInstance> remapSkippingEqualsObjectInstances(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues, boolean fullKey) {
        return mapping.mapValues(value -> {
            ObjectValue mapValue = ((ImMap<PropertyObjectInterfaceInstance, ? extends ObjectValue>) mapKeyValues).get(value);
            // in theory the second check can lead to some unpredictable behaviour for COLUMNS (and other objects)
            if (mapValue != null && !(fullKey && value instanceof ObjectInstance && BaseUtils.hashEquals(value.getObjectValue(), mapValue)))
                value = mapValue;
            return value;
        });
    }

    @Override
    public String toString() {
        return property.toString();
    }
}
