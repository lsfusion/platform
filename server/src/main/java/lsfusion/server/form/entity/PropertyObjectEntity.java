package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public abstract class PropertyObjectEntity<P extends PropertyInterface, T extends Property<P>> extends TwinImmutableObject {

    public T property;
    public ImRevMap<P, ObjectEntity> mapping;

    protected PropertyObjectEntity() {
        //нужен для десериализации
        creationScript = null;
        creationPath = null;
    }

    public String toString() {
        return property.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((PropertyObjectEntity) o).property) && mapping.equals(((PropertyObjectEntity) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public PropertyObjectEntity(T property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath) {
        this.property = property;
        this.mapping = mapping;
        this.creationScript = creationScript==null ? null : creationScript.substring(0, Math.min(10000, creationScript.length()));
        this.creationPath = creationPath;
    }

    public GroupObjectEntity getApplyObject(ImOrderSet<GroupObjectEntity> groupList) {
        GroupObjectEntity applyObject = null;
        int maxIndex = -1;
        for (ObjectEntity object : getObjectInstances()) {
            int index = groupList.indexOf(object.groupTo);
            if (index > maxIndex) {
                applyObject = object.groupTo;
                maxIndex = index;
            }
        }
        return applyObject;
    }

    public abstract CalcPropertyObjectEntity<?> getDrawProperty();

    public ImCol<ObjectEntity> getColObjectInstances() {
        return mapping.values();
    }

    public ImSet<ObjectEntity> getSetObjectInstances() {
        return mapping.values().toSet();
    }

    public Collection<ObjectEntity> getObjectInstances() {
        Collection<ObjectEntity> result = new ArrayList<>();
        for(PropertyObjectInterfaceEntity object : mapping.valueIt())
            if(object instanceof ObjectEntity)
                result.add((ObjectEntity) object);
        return result;
    }

    public Collection<ObjectEntity> getRemappedObjectInstances() {
        Collection<ObjectEntity> result = new ArrayList<>();
        for(Object value : ((JoinProperty) property).implement.mapping.valueIt()) {
            PropertyObjectInterfaceEntity object = mapping.get((P) value);
            if (object instanceof ObjectEntity)
                result.add((ObjectEntity) object);
        }
        return result;
    }
    
    public ImMap<P, ObjectEntity> getMapObjectInstances() {
        return mapping;
    }

    public void fillObjects(MSet<ObjectEntity> objects) {
        objects.addAll(getSetObjectInstances());
    }

    protected final String creationScript;
    protected final String creationPath;

    public String getCreationScript() {
        return creationScript;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public static <I extends PropertyInterface, T extends Property<I>> PropertyObjectEntity<I, ?> create(T property, ImRevMap<I, ObjectEntity> map, String creationScript, String creationPath) {
        if(property instanceof CalcProperty)
            return new CalcPropertyObjectEntity<>((CalcProperty<I>) property, map, creationScript, creationPath);
        else
            return new ActionPropertyObjectEntity<>((ActionProperty<I>) property, map, creationScript, creationPath);
    }
}
