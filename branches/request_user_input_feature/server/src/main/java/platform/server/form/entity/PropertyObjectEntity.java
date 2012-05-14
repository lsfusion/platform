package platform.server.form.entity;

import org.omg.CORBA.PUBLIC_MEMBER;
import platform.base.TwinImmutableInterface;
import platform.server.form.instance.InstanceFactory;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.serialization.ServerCustomSerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class PropertyObjectEntity<P extends PropertyInterface, T extends Property<P>> extends PropertyImplement<P, PropertyObjectInterfaceEntity> implements ServerCustomSerializable {

    public T property;
    public Map<P, PropertyObjectInterfaceEntity> mapping;

    public String toString() {
        return property.toString();
    }

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((PropertyObjectEntity) o).property) && mapping.equals(((PropertyObjectEntity) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public PropertyObjectEntity(LP<P> property, PropertyObjectInterfaceEntity... objects) {
        this((T) property.property, property.getMap(objects), property.getCreationScript());
    }

    public PropertyObjectEntity(T property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        this(property, mapping, null);
    }

    public PropertyObjectEntity(T property, Map<P, PropertyObjectInterfaceEntity> mapping, String creationScript) {
        this.property = property;
        this.mapping = mapping;
        this.creationScript = creationScript;
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

    public String getCreationScript() {
        return creationScript;
    }

    public static <T extends PropertyInterface> PropertyObjectEntity<T, ?> create(Property<T> property, Map<T, PropertyObjectInterfaceEntity> map, String creationScript) {
        if(property instanceof CalcProperty)
            return new CalcPropertyObjectEntity<T>((CalcProperty<T>)property, map, creationScript);
        else
            return (PropertyObjectEntity<T, ?>) new ActionPropertyObjectEntity((ActionProperty) property, ActionProperty.cast(map), creationScript);
    }
}
