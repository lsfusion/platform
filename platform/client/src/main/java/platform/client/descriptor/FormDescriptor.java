package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.Main;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientForm;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.serialization.RemoteDescriptorInterface;

import java.io.*;
import java.util.*;

public class FormDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    public ClientForm client = new ClientForm();

    public String caption;
    public boolean isPrintForm;

    public List<GroupObjectDescriptor> groupObjects = new ArrayList<GroupObjectDescriptor>();
    public List<PropertyDrawDescriptor> propertyDraws = new ArrayList<PropertyDrawDescriptor>();
    public Set<FilterDescriptor> fixedFilters = new HashSet<FilterDescriptor>();
    public List<RegularFilterGroupDescriptor> regularFilterGroups = new ArrayList<RegularFilterGroupDescriptor>();
    public Map<PropertyDrawDescriptor, GroupObjectDescriptor> forceDefaultDraw = new HashMap<PropertyDrawDescriptor, GroupObjectDescriptor>();

    // по сути IncrementLazy
    IncrementView allPropertiesLazy;
    private List<PropertyObjectDescriptor> allProperties;
    public List<PropertyObjectDescriptor> getAllProperties() {
        if (allProperties == null)
            allProperties = getProperties(groupObjects, null);
        return allProperties;
    }

    IncrementView propertyObjectConstraint;
    IncrementView toDrawConstraint;
    IncrementView columnGroupConstraint;
    IncrementView propertyCaptionConstraint;

    IncrementView containerController;

    public FormDescriptor() {
        
    }

    public FormDescriptor(int ID) {
        setID(ID);
        setCaption("Новая форма (" + ID + ")");
    }

    public List<PropertyDrawDescriptor> getGroupPropertyDraws(GroupObjectDescriptor group) {
        List<PropertyDrawDescriptor> result = new ArrayList<PropertyDrawDescriptor>();
        for (PropertyDrawDescriptor propertyDraw : propertyDraws)
            if (group == null || group.equals(propertyDraw.getGroupObject(groupObjects)))
                result.add(propertyDraw);
        return result;
    }

    private abstract class IncrementPropertyConstraint implements IncrementView {

        public abstract boolean updateProperty(PropertyDrawDescriptor property);

        public void update(Object updateObject, String updateField) {
            List<PropertyDrawDescriptor> checkProperties;
            if (updateObject instanceof PropertyDrawDescriptor)
                checkProperties = Collections.singletonList((PropertyDrawDescriptor) updateObject);
            else
                checkProperties = new ArrayList<PropertyDrawDescriptor>(propertyDraws);

            for(PropertyDrawDescriptor checkProperty : checkProperties)
                if(!updateProperty(checkProperty)) // удаляем propertyDraw
                    removeFromPropertyDraws(checkProperty);
        }
    }

    private class ContainerController implements IncrementView {
        public void update(Object updateObject, String updateField) {

            for (PropertyDrawDescriptor propertyDraw : propertyDraws) {
                GroupObjectDescriptor groupObject = propertyDraw.getGroupObject(groupObjects);
                if (groupObject != null) {
                    ClientContainer newContainer = client.findContainerBySID("panelContainer" + propertyDraw.getGroupObject(groupObjects).getID());
                    if (newContainer != null && !newContainer.isAncestorOf(propertyDraw.client.container)) {
                        int insIndex = -1;
                        for (int propIndex = propertyDraws.indexOf(propertyDraw) + 1; propIndex < propertyDraws.size(); propIndex++) {
                            insIndex = newContainer.children.indexOf(propertyDraws.get(propIndex).client);
                            if (insIndex != -1)
                                break;
                        }
                        if (insIndex == -1) insIndex = newContainer.children.size();
                        newContainer.addToChildren(insIndex, propertyDraw.client);
                    }
                }
            }
        }
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        outStream.writeBoolean(isPrintForm);

        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);
        pool.serializeMap(outStream, forceDefaultDraw);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
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
                if (toDraw != null && !property.getPropertyObject().getGroupObjects().contains(toDraw))
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
                if (!constrainedColumnGroups.equals(columnGroups))
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
                if (propertyCaption != null && !getProperties(property.getColumnGroupObjects(), null).contains(propertyCaption))
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

        containerController = new ContainerController();
        IncrementDependency.add("groupObjects", containerController);
        IncrementDependency.add("toDraw", containerController);
        IncrementDependency.add("propertyDraws", containerController);
    }

    @Override
    public String toString() {
        return client.caption;
    }

    public ObjectDescriptor getObject(int objectID) {
        for (GroupObjectDescriptor group : groupObjects)
            for (ObjectDescriptor object : group)
                if (object.getID() == objectID)
                    return object;
        return null;
    }

    public List<PropertyObjectDescriptor> getProperties(GroupObjectDescriptor groupObject) {
        if (groupObject == null) return getAllProperties();
        return getProperties(groupObjects.subList(0, groupObjects.indexOf(groupObject) + 1), groupObject);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<GroupObjectDescriptor> groupObjects, GroupObjectDescriptor toDraw) {
        Collection<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();
        Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();
        for (GroupObjectDescriptor groupObject : groupObjects) {
            objects.addAll(groupObject);
            for (ObjectDescriptor object : groupObject) {
                objectMap.put(object.getID(), groupObject.getID());
            }
        }
        return getProperties(objects, toDraw == null ? new ArrayList<ObjectDescriptor>() : toDraw, Main.remoteLogics, objectMap, false, false);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<GroupObjectDescriptor> groupObjects, RemoteDescriptorInterface remote, ArrayList<GroupObjectDescriptor> toDraw, boolean isCompulsory, boolean isAny) {
        Collection<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();
        Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();
        for (GroupObjectDescriptor groupObject : groupObjects) {
            objects.addAll(groupObject);
            for (ObjectDescriptor object : groupObject) {
                objectMap.put(object.getID(), groupObject.getID());
            }
        }
        ArrayList<ObjectDescriptor> objList = new ArrayList<ObjectDescriptor>();
        for (GroupObjectDescriptor groupObject : toDraw) {
            objList.addAll(groupObject);
        }
        return getProperties(objects, objList, remote, objectMap, isCompulsory, isAny);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<ObjectDescriptor> objects, Collection<ObjectDescriptor> atLeastOne, RemoteDescriptorInterface remote, Map<Integer, Integer> objectMap, boolean isCompulsory, boolean isAny) {
        Map<Integer, ObjectDescriptor> idToObjects = new HashMap<Integer, ObjectDescriptor>();
        Map<Integer, ClientClass> classes = new HashMap<Integer, ClientClass>();
        for (ObjectDescriptor object : objects) {
            ClientClass cls = object.getBaseClass();
            if (cls != null) {
                idToObjects.put(object.getID(), object);
                classes.put(object.getID(), object.getBaseClass());
            }
        }

        List<PropertyObjectDescriptor> result = new ArrayList<PropertyObjectDescriptor>();
        for (PropertyDescriptorImplement<Integer> implement : getProperties(remote, classes, BaseUtils.filterValues(idToObjects, atLeastOne).keySet(), objectMap, isCompulsory, isAny))
            result.add(new PropertyObjectDescriptor(implement.property, BaseUtils.join(implement.mapping, idToObjects)));
        return result;
    }

    public static <K> Collection<PropertyDescriptorImplement<K>> getProperties(RemoteDescriptorInterface remote, Map<K, ClientClass> classes) {
        // todo:
        return new ArrayList<PropertyDescriptorImplement<K>>();
    }

    public static Collection<PropertyDescriptorImplement<Integer>> getProperties(RemoteDescriptorInterface remote, Map<Integer, ClientClass> classes, Collection<Integer> atLeastOne, Map<Integer, Integer> objectMap, boolean isCompulsory, boolean isAny) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            DataOutputStream dataStream = new DataOutputStream(outStream);

            dataStream.writeInt(classes.size());
            for (Map.Entry<Integer, ClientClass> intClass : classes.entrySet()) {
                dataStream.writeInt(intClass.getKey());
                intClass.getValue().serialize(dataStream);
                if (atLeastOne.contains(intClass.getKey())) {
                    dataStream.writeInt(objectMap.get(intClass.getKey()));
                } else {
                    dataStream.writeInt(-1);
                }
            }

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remote.getPropertyObjectsByteArray(outStream.toByteArray(), isCompulsory, isAny)));
            ClientSerializationPool pool = new ClientSerializationPool();

            List<PropertyDescriptorImplement<Integer>> result = new ArrayList<PropertyDescriptorImplement<Integer>>();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                PropertyDescriptor implementProperty = (PropertyDescriptor) pool.deserializeObject(inStream);
                Map<PropertyInterfaceDescriptor, Integer> mapInterfaces = new HashMap<PropertyInterfaceDescriptor, Integer>();
                for (int j = 0; j < implementProperty.interfaces.size(); j++)
                    mapInterfaces.put((PropertyInterfaceDescriptor) pool.deserializeObject(inStream), inStream.readInt());
                result.add(new PropertyDescriptorImplement<Integer>(implementProperty, mapInterfaces));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            return list.get(list.size() - 1);
        } else {
            return list.get(index + (list.indexOf(elemFrom) >= index ? 0 : -1));
        }
    }

    public void setCaption(String caption) {
        this.caption = caption;
        client.caption = caption;

        IncrementDependency.update(this, "caption");
    }

    public String getCaption() {
        return caption;
    }

    private static void moveClientComponent(ClientComponent compFrom, ClientComponent compTo) {
        if (compFrom.container.equals(compTo.container)) {
            compFrom.container.moveChild(compFrom, compTo);
        }
    }

    public boolean addToPropertyDraws(PropertyDrawDescriptor propertyDraw, GroupObjectDescriptor groupObject) {
        if (groupObject != null) {
            propertyDraw.client.groupObject = groupObject.client;
            propertyDraw.toDraw = groupObject;
        }

        return addToPropertyDraws(propertyDraw);
    }

    public boolean addToPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.add(propertyDraw);
        client.propertyDraws.add(propertyDraw.client);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    public boolean removeFromPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.remove(propertyDraw);
        client.removePropertyDraw(propertyDraw.client);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    public boolean addToGroupObjects(GroupObjectDescriptor groupObject) {
        groupObjects.add(groupObject);
        client.groupObjects.add(groupObject.client);

        IncrementDependency.update(this, "groupObjects");
        return true;
    }

    public boolean removeFromGroupObjects(GroupObjectDescriptor groupObject) {
        groupObjects.remove(groupObject);
        client.groupObjects.add(groupObject.client);

        IncrementDependency.update(this, "groupObjects");
        return true;
    }

    public List<RegularFilterGroupDescriptor> getRegularFilterGroups() {
        return regularFilterGroups;
    }

    public void addToRegularFilterGroups(RegularFilterGroupDescriptor filterGroup) {
        regularFilterGroups.add(filterGroup);
        client.addToRegularFilterGroups(filterGroup.client);
        IncrementDependency.update(this, "regularFilterGroups");
    }

    public void removeFromRegularFilterGroups(RegularFilterGroupDescriptor filterGroup) {
        regularFilterGroups.remove(filterGroup);
        client.removeFromRegularFilterGroups(filterGroup.client);
        IncrementDependency.update(this, "regularFilterGroups");
    }


    public static byte[] serialize(FormDescriptor form) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        new ClientSerializationPool(form.client).serializeObject(dataStream, form);
        new ClientSerializationPool(form.client).serializeObject(dataStream, form.client);

        return outStream.toByteArray();
}

    public static FormDescriptor deserialize(byte[] formByteArray) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(formByteArray));

        ClientForm richDesign = new ClientSerializationPool().deserializeObject(inStream);

        return new ClientSerializationPool(richDesign).deserializeObject(inStream);
    }

    public static FormDescriptor deserialize(byte[] richDesignByteArray, byte[] formEntityByteArray) throws IOException {
        ClientForm richDesign = new ClientSerializationPool()
                .deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(richDesignByteArray)));

        return new ClientSerializationPool(richDesign)
                .deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(formEntityByteArray)));
    }
}
