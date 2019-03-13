package lsfusion.server.logics.form.struct.property;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.*;

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
        assert !mapping.containsNull();
    }

    public ImSet<ObjectEntity> getObjectInstances() {
        return mapping.valuesSet();
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
