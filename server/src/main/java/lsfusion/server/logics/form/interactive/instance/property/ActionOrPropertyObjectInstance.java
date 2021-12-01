package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
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
    }
    
    public abstract ActionOrPropertyObjectInstance<P, ?> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues);

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

    protected ImMap<P, PropertyObjectInterfaceInstance> remap(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return mapping.replaceValues(mapKeyValues);
    }

    protected ImMap<P, PropertyObjectInterfaceInstance> remapSkippingEqualsObjectInstances(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues) {
        return replaceEqualObjectInstances((ImMap<PropertyObjectInterfaceInstance, ? extends ObjectValue>) mapKeyValues);
    }

    private ImMap<P, PropertyObjectInterfaceInstance> replaceEqualObjectInstances(final ImMap<PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues) {
        return mapping.mapValues(value -> {
            ObjectValue mapValue = mapKeyValues.get(value);
            if (mapValue != null) {
                if (value instanceof ObjectInstance) {
                    ObjectValue currentValue = value.getObjectValue();
                    if (!BaseUtils.hashEquals(currentValue, mapValue)) {
                        value = mapValue;
                    }
                } else {
                    value = mapValue;
                }
            }
            return value;
        });
    }
    

    @Override
    public String toString() {
        return property.toString();
    }
}
