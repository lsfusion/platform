package platform.client.descriptor;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientForm;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.Main;
import platform.interop.serialization.RemoteDescriptorInterface;
import platform.base.BaseUtils;

import java.io.*;
import java.util.*;
import java.util.List;

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

    private abstract class IncrementPropertyConstraint implements IncrementView {

        public abstract boolean updateProperty(PropertyDrawDescriptor property);

        public void update(Object updateObject, String updateField) {
            List<PropertyDrawDescriptor> checkProperties;
            if(updateObject instanceof PropertyDrawDescriptor)
                checkProperties = Collections.singletonList((PropertyDrawDescriptor)updateObject);
            else
                checkProperties = new ArrayList<PropertyDrawDescriptor>(propertyDraws);

            boolean removed = false;
            for(PropertyDrawDescriptor checkProperty : checkProperties)
                if(!updateProperty(checkProperty)) // удаляем propertyDraw
                    removed = propertyDraws.remove(checkProperty) || removed;

            if(removed)
                IncrementDependency.update(this, "propertyDraws");
        }
    }

    // по сути IncrementLazy
    IncrementView allPropertiesLazy;
    private List<PropertyObjectDescriptor> allProperties;    
    public List<PropertyObjectDescriptor> getAllProperties() {
        if(allProperties==null)
            allProperties = getProperties(groupObjects, null, Main.remoteLogics);
        return allProperties;
    }

    IncrementView propertyObjectConstraint;
    IncrementView toDrawConstraint;
    IncrementView columnGroupConstraint;
    IncrementView propertyCaptionConstraint;

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

        allPropertiesLazy = new IncrementView() {
            public void update(Object updateObject, String updateField) {
                allProperties = null;
            }
        };
        IncrementDependency.add("baseClass", allPropertiesLazy);
        IncrementDependency.add("objects", allPropertiesLazy);
        IncrementDependency.add(this, "groupObjects", allPropertiesLazy);

        // propertyObject подходит по интерфейсу и т.п.
        propertyObjectConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                return getAllProperties().contains(property.getPropertyObject());
            }
        };
        IncrementDependency.add("propertyObject", propertyObjectConstraint);
        IncrementDependency.add("baseClass", propertyObjectConstraint);
        IncrementDependency.add("objects", propertyObjectConstraint);
        IncrementDependency.add(this, "groupObjects", propertyObjectConstraint);

        // toDraw должен быть из groupObjects (можно убрать)
        toDrawConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                GroupObjectDescriptor toDraw = property.getToDraw();
                if(toDraw!=null && !property.getPropertyObject().getGroupObjects().contains(toDraw))
                    property.setToDraw(null);
                return true;
            }
        };
        IncrementDependency.add("toDraw", toDrawConstraint);
        IncrementDependency.add("propertyObject", toDrawConstraint);

        // toDraw должен быть из groupObjects (можно убрать)
        columnGroupConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                List<GroupObjectDescriptor> upGroups = property.getUpGroupObjects(groupObjects);
                List<GroupObjectDescriptor> columnGroups = property.getColumnGroupObjects();

                List<GroupObjectDescriptor> constrainedColumnGroups = BaseUtils.filterList(columnGroups, upGroups);
                if(!constrainedColumnGroups.equals(columnGroups))
                    property.setColumnGroupObjects(constrainedColumnGroups);
                return true;
            }
        };
        IncrementDependency.add("propertyObject", columnGroupConstraint);
        IncrementDependency.add("toDraw", columnGroupConstraint);
        IncrementDependency.add("objects", columnGroupConstraint);
        IncrementDependency.add(this, "groupObjects", columnGroupConstraint); // порядок тоже важен


        // propertyObject подходит по интерфейсу и т.п.
        propertyCaptionConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                PropertyObjectDescriptor propertyCaption = property.getPropertyCaption();
                if(propertyCaption !=null && !getProperties(property.getColumnGroupObjects(), null, Main.remoteLogics).contains(propertyCaption))
                    property.setPropertyCaption(null);
                return true;                
            }
        };
        IncrementDependency.add("propertyObject", propertyCaptionConstraint);
        IncrementDependency.add("propertyCaption", propertyCaptionConstraint);
        IncrementDependency.add("columnGroupObjects", propertyCaptionConstraint);
        IncrementDependency.add("baseClass", propertyCaptionConstraint);
        IncrementDependency.add("objects", propertyCaptionConstraint);
        IncrementDependency.add(this, "groupObjects", propertyCaptionConstraint);
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

    public List<PropertyObjectDescriptor> getProperties(GroupObjectDescriptor groupObject) {
        if(groupObject==null) return getAllProperties();
        return getProperties(groupObjects.subList(0, groupObjects.indexOf(groupObject)+1), groupObject, Main.remoteLogics);
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
            ClientClass cls = object.getBaseClass();
            if (cls != null) {
                idToObjects.put(object.getID(), object);
                classes.put(object.getID(), object.getBaseClass());
            }
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

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remote.getPropertyObjectsByteArray(outStream.toByteArray(), false)));
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
        return moveGroupObject(groupFrom, groupObjects.indexOf(groupTo) + (groupObjects.indexOf(groupFrom) > groupObjects.indexOf(groupTo) ? 0 : 1));
    }

    public boolean moveGroupObject(GroupObjectDescriptor groupFrom, int index) {
        BaseUtils.moveElement(groupObjects, groupFrom, index);
        BaseUtils.moveElement(client.groupObjects, groupFrom.client, index);
        IncrementDependency.update(this, "groupObjects");
        return true;
    }

    public boolean movePropertyDraw(PropertyDrawDescriptor propFrom, PropertyDrawDescriptor propTo) {
        return movePropertyDraw(propFrom, propertyDraws.indexOf(propTo) + (propertyDraws.indexOf(propFrom) > propertyDraws.indexOf(propTo) ? 0 : 1));
    }

    public boolean movePropertyDraw(PropertyDrawDescriptor propFrom, int index) {

        moveClientComponent(propFrom.client, getElementTo(propertyDraws, propFrom, index).client);

        BaseUtils.moveElement(propertyDraws, propFrom, index);
        BaseUtils.moveElement(client.propertyDraws, propFrom.client, index);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    private static <T> T getElementTo(List<T> list, T elemFrom, int index) {
        if (index == -1) {
            return list.get(list.size()-1);
        } else {
            return list.get(index + (list.indexOf(elemFrom) >= index ? 0 : -1));
        }
    }

    private static void moveClientComponent(ClientComponent compFrom, ClientComponent compTo) {
        if (compFrom.container.equals(compTo.container)) {
            compFrom.container.moveChild(compFrom, compTo);
        }
    }

    public boolean addPropertyDraw(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.add(propertyDraw);
        client.propertyDraws.add(propertyDraw.client);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    public boolean removePropertyDraw(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.remove(propertyDraw);
        client.removePropertyDraw(propertyDraw.client);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    public boolean addGroupObject(GroupObjectDescriptor groupObject) {
        groupObjects.add(groupObject);
        client.groupObjects.add(groupObject.client);

        IncrementDependency.update(this, "groupObjects");
        return true;
    }

    public boolean removeGroupObject(GroupObjectDescriptor groupObject) {
        groupObjects.remove(groupObject);
        client.groupObjects.add(groupObject.client);

        IncrementDependency.update(this, "groupObjects");
        return true;
    }
}
