package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerCustomSerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class PropertyObjectEntity<P extends PropertyInterface> extends PropertyImplement<P, PropertyObjectInterfaceEntity> implements OrderEntity<PropertyObjectInstance>, ServerCustomSerializable {

    public PropertyObjectEntity() {
    }

    public PropertyObjectEntity(LP<P> property, PropertyObjectInterfaceEntity... objects) {
        super(property.property);
        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    public PropertyObjectEntity(Property<P> property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        super(property, mapping);
    }

    public PropertyObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
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

        property = pool.context.BL.getProperty(propertySID);
        mapping = new HashMap<P, PropertyObjectInterfaceEntity>();

        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            int interId = inStream.readInt();
            P inter = property.getInterfaceById(interId);
            PropertyObjectInterfaceEntity value = pool.deserializeObject(inStream);

            mapping.put(inter, value);
        }
    }

    public PropertyObjectEntity<P> getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        Map<P, PropertyObjectInterfaceEntity> nmapping = new HashMap<P, PropertyObjectInterfaceEntity>();

        for (P iFace : property.interfaces) {
            nmapping.put(iFace, mapping.get(iFace).getRemappedEntity(oldObject, newObject, instanceFactory));
        }

        return new PropertyObjectEntity<P>(property, nmapping);
    }
}
