package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.SFunctionSet;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

public abstract class PropertyObjectInstance<P extends PropertyInterface, T extends Property<P>> extends TwinImmutableObject {

    public T property;
    public ImMap<P, PropertyObjectInterfaceInstance> mapping;

    public boolean twins(TwinImmutableObject o) {
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

    @Override
    public String toString() {
        return property.toString();
    }
}
