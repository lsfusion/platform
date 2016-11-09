package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class PropertyObjectInstance<P extends PropertyInterface, T extends Property<P>> extends TwinImmutableObject {

    public T property;
    public ImMap<P, PropertyObjectInterfaceInstance> mapping;

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((PropertyObjectInstance) o).property) && mapping.equals(((PropertyObjectInstance) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public PropertyObjectInstance(T property,ImMap<P,? extends PropertyObjectInterfaceInstance> mapping) {
        this.property = property;
        this.mapping = (ImMap<P, PropertyObjectInterfaceInstance>) mapping;
    }

    // получает GRID в котором рисоваться
    public GroupObjectInstance getApplyObject() {
        GroupObjectInstance applyObject=null;
        for(ObjectInstance intObject : getObjectInstances())
            if(applyObject==null || intObject.groupTo.order >applyObject.order)
                applyObject = intObject.getApplyObject();

        return applyObject;
    }

    public ImCol<ObjectInstance> getObjectInstances() {
        return BaseUtils.immutableCast(mapping.values().filterCol(new SFunctionSet<PropertyObjectInterfaceInstance>() {
            public boolean contains(PropertyObjectInterfaceInstance element) {
                return element instanceof ObjectInstance;
            }}));
    }

    public void fillObjects(MSet<ObjectInstance> objects) {
        objects.addAll(getObjectInstances().toSet());
    }

    // в интерфейсе
    public boolean isInInterface(GroupObjectInstance classGroup) {
        return isInInterface(classGroup == null ? SetFact.<GroupObjectInstance>EMPTY() : SetFact.singleton(classGroup), false);
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        // assert что classGroups все в GRID представлении
        ImMap<P, AndClassSet> classImplement = mapping.mapValues(new GetValue<AndClassSet, PropertyObjectInterfaceInstance>() {
            public AndClassSet getMapValue(PropertyObjectInterfaceInstance value) {
                return value.getClassSet(classGroups);
            }});
        return property.cacheIsInInterface(classImplement, any);
    }

    public abstract CalcPropertyObjectInstance<?> getDrawProperty();

    public ImMap<P, DataObject> getInterfaceValues() {
        return mapping.mapValues(new GetValue<DataObject, PropertyObjectInterfaceInstance>() {
            public DataObject getMapValue(PropertyObjectInterfaceInstance value) {
                return value.getDataObject();
            }});
    }

    public Type getType() {
        return property.getType();
    }

    protected ImMap<P, PropertyObjectInterfaceInstance> remap(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return mapping.replaceValues(mapKeyValues);
    }

    protected ImMap<P, PropertyObjectInterfaceInstance> remapSkippingEqualsObjectInstances(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return replaceEqualObjectInstances((ImMap<PropertyObjectInterfaceInstance, DataObject>) mapKeyValues);
    }

    private ImMap<P, PropertyObjectInterfaceInstance> replaceEqualObjectInstances(final ImMap<PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return mapping.mapValues(new GetValue<PropertyObjectInterfaceInstance, PropertyObjectInterfaceInstance>() {
            public PropertyObjectInterfaceInstance getMapValue(PropertyObjectInterfaceInstance value) {
                DataObject mapValue = mapKeyValues.get(value);
                if (mapValue != null) {
                    if (value instanceof ObjectInstance) {
                        Object currentValue = ((ObjectInstance) value).getObjectValue().getValue();
                        if (!BaseUtils.nullEquals(currentValue, mapValue.getValue())) {
                            value = mapValue;
                        }
                    } else {
                        value = mapValue;
                    }
                }
                return value;
            }
        });
    }
    

    @Override
    public String toString() {
        return property.toString();
    }
}
