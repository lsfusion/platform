package lsfusion.server.logics.form.struct.property.oraction;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ActionOrPropertyObjectEntity<P extends PropertyInterface, T extends ActionOrProperty<P>> extends TwinImmutableObject {

    public T property;
    public ImRevMap<P, ObjectEntity> mapping;

    protected ActionOrPropertyObjectEntity() {
        //нужен для десериализации
        creationScript = null;
        creationPath = null;
        path = null;
    }

    public String toString() {
        return property.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionOrPropertyObjectEntity) o).property) && mapping.equals(((ActionOrPropertyObjectEntity) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public ActionOrPropertyObjectEntity(T property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath, String path) {
        this.property = property;
        this.mapping = mapping;
        this.creationScript = creationScript==null ? null : creationScript.substring(0, Math.min(10000, creationScript.length()));
        this.creationPath = creationPath;
        this.path = path;
        assert !mapping.containsNull();
        assert property.interfaces.equals(mapping.keys());
    }

    public ImSet<ObjectEntity> getObjectInstances() {
        return mapping.valuesSet();
    }

    protected final String creationScript;
    protected final String creationPath;
    protected final String path;

    public String getCreationScript() {
        return creationScript;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public String getPath() {
        return path;
    }

    public static <I extends PropertyInterface, T extends ActionOrProperty<I>> ActionOrPropertyObjectEntity<I, ?> create(T property, ImRevMap<I, ObjectEntity> map, String creationScript, String creationPath, String path) {
        if(property instanceof Property)
            return new PropertyObjectEntity<>((Property<I>) property, map, creationScript, creationPath, path);
        else
            return new ActionObjectEntity<>((Action<I>) property, map, creationScript, creationPath, path);
    }

    public abstract <X extends PropertyInterface> PropertyObjectEntity<?> getDrawProperty(PropertyObjectEntity<X> readOnly);
}
