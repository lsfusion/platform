package platform.client.descriptor;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.ClientForm;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.serialization.RemoteDescriptorInterface;
import platform.base.BaseUtils;

import java.io.*;
import java.util.*;

public class FormDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    public ClientForm client;

    public String caption;
    public boolean isPrintForm;

    public List<GroupObjectDescriptor> groupObjects;
    public List<PropertyDrawDescriptor> propertyDraws;
    public Set<FilterDescriptor> fixedFilters;
    public List<RegularFilterGroupDescriptor> regularFilterGroups;
    public Map<PropertyDrawDescriptor, GroupObjectDescriptor> forceDefaultDraw = new HashMap<PropertyDrawDescriptor, GroupObjectDescriptor>();

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeUTF(caption);
        outStream.writeBoolean(isPrintForm);

        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);
        pool.serializeMap(outStream, forceDefaultDraw);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;

        caption = inStream.readUTF();
        isPrintForm = inStream.readBoolean();

        groupObjects = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        regularFilterGroups = pool.deserializeList(inStream);
        forceDefaultDraw = pool.deserializeMap(inStream);

        client = pool.context;
    }

    @Override
    public String toString() {
        return client.caption;
    }

    public ObjectDescriptor getObject(int objectID) {
        for(GroupObjectDescriptor group : groupObjects)
            for(ObjectDescriptor object : group)
                if(object.getID() == objectID)
                    return object;
        return null;
    }

    public List<PropertyObjectDescriptor> getProperties(GroupObjectDescriptor groupObject, RemoteDescriptorInterface remote) {
        return getProperties(groupObjects.subList(0, groupObjects.indexOf(groupObject)+1), groupObject, remote);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<GroupObjectDescriptor> groupObjects, GroupObjectDescriptor toDraw, RemoteDescriptorInterface remote) {
        Collection<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();
        for(GroupObjectDescriptor groupObject : groupObjects)
            objects.addAll(groupObject);
        return getProperties(objects, toDraw==null?new ArrayList<ObjectDescriptor>():toDraw, remote);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<ObjectDescriptor> objects, Collection<ObjectDescriptor> atLeastOne, RemoteDescriptorInterface remote) {
        Map<Integer, ObjectDescriptor> idToObjects = new HashMap<Integer, ObjectDescriptor>();
        Map<Integer, ClientClass> classes = new HashMap<Integer, ClientClass>();
        for(ObjectDescriptor object : objects) {
            idToObjects.put(object.getID(), object);
            classes.put(object.getID(), object.client.baseClass);
        }

        List<PropertyObjectDescriptor> result = new ArrayList<PropertyObjectDescriptor>();
        for(PropertyDescriptorImplement<Integer> implement : getProperties(remote, classes, BaseUtils.filterValues(idToObjects,atLeastOne).keySet()))
            result.add(new PropertyObjectDescriptor(implement.property, BaseUtils.join(implement.mapping,idToObjects)));
        return result;
    }

    public static <K> Collection<PropertyDescriptorImplement<K>> getProperties(RemoteDescriptorInterface remote, Map<K, ClientClass> classes) {
        // todo:
        return new ArrayList<PropertyDescriptorImplement<K>>();
    }

    public static Collection<PropertyDescriptorImplement<Integer>> getProperties(RemoteDescriptorInterface remote, Map<Integer, ClientClass> classes, Collection<Integer> atLeastOne) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            DataOutputStream dataStream = new DataOutputStream(outStream);

            dataStream.writeInt(classes.size());
            for(Map.Entry<Integer,ClientClass> intClass : classes.entrySet()) {
                dataStream.writeInt(intClass.getKey());
                intClass.getValue().serialize(dataStream);
                dataStream.writeBoolean(atLeastOne.contains(intClass.getKey()));
            }

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remote.getPropertyObjectsByteArray(outStream.toByteArray())));
            ClientSerializationPool pool = new ClientSerializationPool();

            List<PropertyDescriptorImplement<Integer>> result = new ArrayList<PropertyDescriptorImplement<Integer>>();
            int size = inStream.readInt();
            for(int i=0;i<size;i++) {
                PropertyDescriptor implementProperty = (PropertyDescriptor) pool.deserializeObject(inStream);
                Map<PropertyInterfaceDescriptor, Integer> mapInterfaces = new HashMap<PropertyInterfaceDescriptor, Integer>();
                for(int j=0;j<implementProperty.interfaces.size();j++)
                    mapInterfaces.put((PropertyInterfaceDescriptor)pool.deserializeObject(inStream), inStream.readInt());
                result.add(new PropertyDescriptorImplement<Integer>(implementProperty, mapInterfaces));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serialize(FormDescriptor form) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        new ClientSerializationPool(form.client).serializeObject(dataStream, form);
        new ClientSerializationPool(form.client).serializeObject(dataStream, form.client);

        return outStream.toByteArray();
    }

    public boolean moveGroupObject(GroupObjectDescriptor groupFrom, GroupObjectDescriptor groupTo) {
        BaseUtils.moveElement(groupObjects, groupFrom, groupTo);
        BaseUtils.moveElement(client.groupObjects, groupFrom.client, groupTo.client);
        IncrementDependency.update(this, "groupObjects");
        return true;
    }

    public boolean movePropertyDraw(PropertyDrawDescriptor propFrom, PropertyDrawDescriptor propTo) {
        BaseUtils.moveElement(propertyDraws, propFrom, propTo);
        BaseUtils.moveElement(client.propertyDraws, propFrom.client, propTo.client);
        IncrementDependency.update(this, "propertyDraws");
        return true;
    }
}
