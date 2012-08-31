package platform.server.form.entity;

import platform.base.TwinImmutableInterface;
import platform.server.logics.property.*;
import platform.server.serialization.ServerCustomSerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class PropertyObjectEntity<P extends PropertyInterface, T extends Property<P>> implements ServerCustomSerializable {

    public T property;
    public Map<P, PropertyObjectInterfaceEntity> mapping;

    protected PropertyObjectEntity() {
        //нужен для десериализации
        creationScript = null;
        creationPath = null;
    }

    public String toString() {
        return property.toString();
    }

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((PropertyObjectEntity) o).property) && mapping.equals(((PropertyObjectEntity) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public PropertyObjectEntity(T property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        this(property, mapping, null, null);
    }

    public PropertyObjectEntity(T property, Map<P, PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        this.property = property;
        this.mapping = mapping;
        this.creationScript = creationScript;
        this.creationPath = creationPath;
    }

    public GroupObjectEntity getApplyObject(List<GroupObjectEntity> groupList) {
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

    public Collection<ObjectEntity> getObjectInstances() {
        Collection<ObjectEntity> result = new ArrayList<ObjectEntity>();
        for(PropertyObjectInterfaceEntity object : mapping.values())
            if(object instanceof ObjectEntity)
                result.add((ObjectEntity) object);
        return result;
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.addAll(getObjectInstances());
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);

        outStream.writeInt(mapping.size());
        for (Map.Entry<P, PropertyObjectInterfaceEntity> entry : mapping.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            pool.serializeObject(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        String propertySID = inStream.readUTF();

        property = (T) pool.context.BL.getProperty(propertySID);

        mapping = new HashMap<P, PropertyObjectInterfaceEntity>();

        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            int interId = inStream.readInt();
            P inter = property.getInterfaceById(interId);
            PropertyObjectInterfaceEntity value = pool.deserializeObject(inStream);

            mapping.put(inter, value);
        }
    }

    protected final String creationScript;
    protected final String creationPath;

    public String getCreationScript() {
        return creationScript;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public static <T extends PropertyInterface> PropertyObjectEntity<T, ?> create(Property<T> property, Map<T, PropertyObjectInterfaceEntity> map, String creationScript, String creationPath) {
        if(property instanceof CalcProperty)
            return new CalcPropertyObjectEntity<T>((CalcProperty<T>)property, map, creationScript, creationPath);
        else
            return new ActionPropertyObjectEntity<T>((ActionProperty<T>) property, map, creationScript, creationPath);
    }
}
