package lsfusion.client.descriptor;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ContextIdentityObject;
import lsfusion.base.context.IncrementView;
import lsfusion.base.serialization.CustomSerializable;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Main;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.client.descriptor.filter.RegularFilterGroupDescriptor;
import lsfusion.client.descriptor.property.PropertyDescriptor;
import lsfusion.client.descriptor.property.PropertyInterfaceDescriptor;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.Constants;
import lsfusion.interop.ModalityType;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.layout.ContainerFactory;
import lsfusion.interop.form.layout.FormContainerSet;
import lsfusion.interop.form.layout.GroupObjectContainerSet;
import lsfusion.interop.form.layout.TreeGroupContainerSet;

import java.io.*;
import java.util.*;

public class FormDescriptor extends ContextIdentityObject implements ClientIdentitySerializable {

    public ClientForm client;

    public String caption;
    public String title;
    public ModalityType modalityType = ModalityType.DOCKED;
    public int autoRefresh;

    public List<GroupObjectDescriptor> groupObjects = new ArrayList<GroupObjectDescriptor>();
    public List<TreeGroupDescriptor> treeGroups = new ArrayList<TreeGroupDescriptor>();
    public List<PropertyDrawDescriptor> propertyDraws = new ArrayList<PropertyDrawDescriptor>();
    public Set<FilterDescriptor> fixedFilters = new HashSet<FilterDescriptor>();
    public List<RegularFilterGroupDescriptor> regularFilterGroups = new ArrayList<RegularFilterGroupDescriptor>();
    public Map<Object, List<PropertyObjectDescriptor>> eventActions = new HashMap<Object, List<PropertyObjectDescriptor>>();
    public OrderedMap<PropertyDrawDescriptor, Boolean> defaultOrders = new OrderedMap<PropertyDrawDescriptor, Boolean>();

    public PropertyDrawDescriptor printActionPropertyDraw;
    public PropertyDrawDescriptor editActionPropertyDraw;
    public PropertyDrawDescriptor xlsActionPropertyDraw;
    public PropertyDrawDescriptor refreshActionPropertyDraw;
    public PropertyDrawDescriptor applyActionPropertyDraw;
    public PropertyDrawDescriptor cancelActionPropertyDraw;
    public PropertyDrawDescriptor okActionPropertyDraw;
    public PropertyDrawDescriptor closeActionPropertyDraw;
    public PropertyDrawDescriptor dropActionPropertyDraw;

    // по сути IncrementLazy
    IncrementView allPropertiesLazy;
    private List<PropertyObjectDescriptor> allProperties;

    public List<PropertyObjectDescriptor> getAllProperties() {
        if (allProperties == null) {
            allProperties = getProperties(groupObjects, null);
        }
        return allProperties;
    }

    IncrementView propertyObjectConstraint;
    IncrementView toDrawConstraint;
    IncrementView columnGroupConstraint;

    IncrementView containerController;

    public FormDescriptor() {
        super();
    }

    // будем считать, что именно этот конструктор используется для создания новых форм
    public FormDescriptor(String sID) {
        super(Main.generateNewID());
        if (sID == null) {
            setSID(Constants.getDefaultFormSID(ID));
        } else {
            setSID(sID);
        }

        context = new ApplicationContext();
        client = new ClientForm(ID, context);

        initialize();

        setCaption(ClientResourceBundle.getString("descriptor.newform") + " (" + ID + ")");

        addFormDefaultContainers();
    }

    public List<PropertyDrawDescriptor> getAddPropertyDraws(GroupObjectDescriptor group) {
        List<PropertyDrawDescriptor> result = new ArrayList<PropertyDrawDescriptor>();
        if (group == null) {
            return result;
        } // предполагается, что для всей папки свойства, любое добавленное свойство будет в getGroupPropertyDraws
        for (PropertyDrawDescriptor propertyDraw : propertyDraws) // добавим новые свойства, предполагается что оно одно
        {
            if (propertyDraw.getPropertyObject() == null && group.equals(propertyDraw.addGroup)) {
                result.add(propertyDraw);
            }
        }
        return result;
    }

    public List<PropertyDrawDescriptor> getGroupPropertyDraws(GroupObjectDescriptor group) {
        List<PropertyDrawDescriptor> result = new ArrayList<PropertyDrawDescriptor>();
        for (PropertyDrawDescriptor propertyDraw : propertyDraws) {
            if (group == null || group.equals(propertyDraw.getGroupObject(groupObjects))) {
                result.add(propertyDraw);
            }
        }
        return result;
    }

    public ApplicationContext getContext() {
        return context;
    }

    private abstract class IncrementPropertyConstraint implements IncrementView {

        public abstract boolean updateProperty(PropertyDrawDescriptor property);

        public void update(Object updateObject, String updateField) {
            List<PropertyDrawDescriptor> checkProperties;
            if (updateObject instanceof PropertyDrawDescriptor) {
                checkProperties = Collections.singletonList((PropertyDrawDescriptor) updateObject);
            } else {
                checkProperties = new ArrayList<PropertyDrawDescriptor>(propertyDraws);
            }

            for (PropertyDrawDescriptor checkProperty : checkProperties) {
                if (!updateProperty(checkProperty)) // удаляем propertyDraw
                {
                    removeFromPropertyDraws(checkProperty);
                }
            }
        }
    }

    IncrementView containerMover;

    // класс, который отвечает за автоматическое перемещение компонент внутри контейнеров при каких-либо изменениях структуры groupObject
    private class ContainerMover implements IncrementView {
        public void update(Object updateObject, String updateField) {
            moveContainer(propertyDraws);
            moveContainer(regularFilterGroups);
        }

        private <T extends ContainerMovable> void moveContainer(List<T> objects) {

            ClientContainer mainContainer = client.mainContainer;

            for (T object : objects) {

                ClientContainer newContainer = object.getDestinationContainer(mainContainer, groupObjects);
                ClientContainer oldContainer = object.getClientComponent(mainContainer).container;

                boolean last = false;
                if (oldContainer == null && newContainer == null) {
                    newContainer = client.mainContainer;
                    last = true;
                }

                if (newContainer != null && !newContainer.isAncestorOf(object.getClientComponent(mainContainer).container)) {
                    int insIndex = -1;
                    // сначала пробуем вставить перед объектом, который идет следующим в этом контейнере
                    for (int propIndex = objects.indexOf(object) + 1; propIndex < objects.size(); propIndex++) {
                        ClientComponent comp = objects.get(propIndex).getClientComponent(mainContainer);
                        if (newContainer.equals(comp.container)) {
                            insIndex = newContainer.children.indexOf(comp);
                            if (insIndex != -1) {
                                break;
                            }
                        }
                    }
                    if (insIndex == -1) {
                        // затем пробуем вставить после объекта, который идет перед вставляемым в этом контейнере
                        for (int propIndex = objects.indexOf(object) - 1; propIndex >= 0; propIndex--) {
                            ClientComponent comp = objects.get(propIndex).getClientComponent(mainContainer);
                            if (newContainer.equals(comp.container)) {
                                insIndex = newContainer.children.indexOf(comp);
                                if (insIndex != -1) {
                                    insIndex++;
                                    break;
                                }
                            }
                        }
                    }

                    // если объект свойство не нашлось куда добавить, то его надо добавлять самым первым в контейнер
                    // иначе свойства будут идти после управляющих объектов
                    if (insIndex == -1) {
                        insIndex = (last ? newContainer.children.size() : 0);
                    }
                    newContainer.addToChildren(insIndex, object.getClientComponent(mainContainer));
                }
            }
        }
    }

    IncrementView containerRenamer;

    // класс, который отвечает за автоматическое переименование компонент внутри контейнеров при каких-либо изменениях структуры groupObject
    private class ContainerRenamer implements IncrementView {
        public void update(Object updateObject, String updateField) {
            renameGroupObjectContainer();
        }

        private void renameGroupObjectContainer() {

            for (GroupObjectDescriptor group : groupObjects) {
                // по сути дублирует логику из GroupObjectContainerSet в плане установки caption для контейнера
                ClientContainer groupContainer = group.getClientComponent(client.mainContainer);
                if (groupContainer != null) {
                    groupContainer.setCaption(group.client.getCaption());
                }
            }
        }
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        pool.writeString(outStream, title);
        pool.writeString(outStream, sID);
        outStream.writeUTF(modalityType.name());
        outStream.writeInt(autoRefresh);

        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, treeGroups);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);

        pool.serializeObject(outStream, printActionPropertyDraw);
        pool.serializeObject(outStream, editActionPropertyDraw);
        pool.serializeObject(outStream, xlsActionPropertyDraw);
        pool.serializeObject(outStream, refreshActionPropertyDraw);
        pool.serializeObject(outStream, applyActionPropertyDraw);
        pool.serializeObject(outStream, cancelActionPropertyDraw);
        pool.serializeObject(outStream, okActionPropertyDraw);
        pool.serializeObject(outStream, closeActionPropertyDraw);
        pool.serializeObject(outStream, dropActionPropertyDraw);

        outStream.writeInt(defaultOrders.size());
        for (Map.Entry<PropertyDrawDescriptor, Boolean> entry : defaultOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey(), serializationType);
            outStream.writeBoolean(entry.getValue());
        }

        outStream.writeInt(eventActions.size());
        for (Map.Entry<Object, List<PropertyObjectDescriptor>> entry : eventActions.entrySet()) {
            Object event = entry.getKey();

            if (event instanceof String) {
                outStream.writeByte(0);
                pool.writeString(outStream, (String) event);
            } else if (event instanceof CustomSerializable) {
                outStream.writeByte(1);
                pool.serializeObject(outStream, (CustomSerializable) event);
            } else {
                outStream.writeByte(2);
                pool.writeObject(outStream, event);
            }

            pool.serializeCollection(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
        title = pool.readString(inStream);
        sID = pool.readString(inStream);
        modalityType = ModalityType.valueOf(inStream.readUTF());
        autoRefresh = inStream.readInt();

        groupObjects = pool.deserializeList(inStream);
        treeGroups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        regularFilterGroups = pool.deserializeList(inStream);

        printActionPropertyDraw = pool.deserializeObject(inStream);
        editActionPropertyDraw = pool.deserializeObject(inStream);
        xlsActionPropertyDraw = pool.deserializeObject(inStream);
        dropActionPropertyDraw = pool.deserializeObject(inStream);
        refreshActionPropertyDraw = pool.deserializeObject(inStream);
        applyActionPropertyDraw = pool.deserializeObject(inStream);
        cancelActionPropertyDraw = pool.deserializeObject(inStream);
        okActionPropertyDraw = pool.deserializeObject(inStream);
        closeActionPropertyDraw = pool.deserializeObject(inStream);

        defaultOrders = new OrderedMap<PropertyDrawDescriptor, Boolean>();
        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawDescriptor order = pool.deserializeObject(inStream);
            defaultOrders.put(order, inStream.readBoolean());
        }

        eventActions = new HashMap<Object, List<PropertyObjectDescriptor>>();
        int length = inStream.readInt();
        for (int i = 0; i < length; ++i) {
            Object event;
            switch (inStream.readByte()) {
                case 0:
                    event = pool.readString(inStream);
                    break;
                case 1:
                    event = pool.deserializeObject(inStream);
                    break;
                default:
                    event = pool.readObject(inStream);
                    break;
            }
            List<PropertyObjectDescriptor> actions = pool.deserializeList(inStream);
            eventActions.put(event, actions);
        }

        client = pool.context;

        initialize();
    }

    @Override
    public String toString() {
        return client.caption;
    }

    private void initialize() {

        allPropertiesLazy = new IncrementView() {
            public void update(Object updateObject, String updateField) {
                allProperties = null;
            }
        };

        addDependency("baseClass", allPropertiesLazy);
        addDependency("objects", allPropertiesLazy);
        addDependency(this, "groupObjects", allPropertiesLazy);

        // propertyObject подходит по интерфейсу и т.п.
        propertyObjectConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                return getAllProperties().contains(property.getPropertyObject());
            }
        };
        addDependency("propertyObject", propertyObjectConstraint);
        addDependency("baseClass", propertyObjectConstraint);
        addDependency("objects", propertyObjectConstraint);
        addDependency(this, "groupObjects", propertyObjectConstraint);

        // toDraw должен быть из groupObjects (можно убрать)
        toDrawConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                GroupObjectDescriptor toDraw = property.getToDraw();
                boolean isEmptyGroupObjects = property.getPropertyObject().getGroupObjects() == null;
                if (toDraw != null && property.getPropertyObject() != null
                        && !property.getPropertyObject().getGroupObjects().contains(toDraw) && isEmptyGroupObjects) {
                    property.setToDraw(null);
                }
                return true;
            }
        };
        addDependency("toDraw", toDrawConstraint);
        addDependency("propertyObject", toDrawConstraint);

        // toDraw должен быть из groupObjects (можно убрать)
        columnGroupConstraint = new IncrementPropertyConstraint() {
            public boolean updateProperty(PropertyDrawDescriptor property) {
                List<GroupObjectDescriptor> upGroups = property.getUpGroupObjects(groupObjects);
                List<GroupObjectDescriptor> columnGroups = property.getColumnGroupObjects();

                List<GroupObjectDescriptor> constrainedColumnGroups = BaseUtils.filterList(columnGroups, upGroups);
                if (!constrainedColumnGroups.equals(columnGroups)) {
                    property.setColumnGroupObjects(constrainedColumnGroups);
                }
                return true;
            }
        };
        addDependency("propertyObject", columnGroupConstraint);
        addDependency("toDraw", columnGroupConstraint);
        addDependency("objects", columnGroupConstraint);
        addDependency(this, "groupObjects", columnGroupConstraint); // порядок тоже важен

        containerMover = new ContainerMover();
        addDependency("groupObjects", containerMover);
        addDependency("toDraw", containerMover);
        addDependency("filters", containerMover);
        addDependency("filter", containerMover);
        addDependency("propertyDraws", containerMover);
        addDependency("property", containerMover);
        addDependency("propertyObject", containerMover);
        addDependency("value", containerMover); // нужно, чтобы перемещать regularFilterGroup, при использовании фильтра Сравнение

        containerRenamer = new ContainerRenamer();
        addDependency("groupObjects", containerRenamer);
        addDependency("objects", containerRenamer);
        addDependency("baseClass", containerRenamer);

    }

    public ObjectDescriptor getObject(int objectID) {
        for (GroupObjectDescriptor group : groupObjects) {
            for (ObjectDescriptor object : group.objects) {
                if (object.getID() == objectID) {
                    return object;
                }
            }
        }
        return null;
    }

    public GroupObjectDescriptor getGroupObject(int groupID) {
        for (GroupObjectDescriptor group : groupObjects) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    public List<PropertyObjectDescriptor> getProperties(GroupObjectDescriptor groupObject) {
        if (groupObject == null) {
            return getAllProperties();
        }
        return getProperties(groupObjects.subList(0, groupObjects.indexOf(groupObject) + 1), groupObject);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<GroupObjectDescriptor> groupObjects, GroupObjectDescriptor toDraw) {
        Collection<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();
        Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();
        for (GroupObjectDescriptor groupObject : groupObjects) {
            objects.addAll(groupObject.objects);
            for (ObjectDescriptor object : groupObject.objects) {
                objectMap.put(object.getID(), groupObject.getID());
            }
        }
        return getProperties(objects, toDraw == null ? new ArrayList<ObjectDescriptor>() : toDraw.objects, Main.remoteLogics, objectMap, false, false);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<GroupObjectDescriptor> groupObjects, RemoteLogicsInterface logics, ArrayList<GroupObjectDescriptor> toDraw, boolean isCompulsory, boolean isAny) {
        Collection<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();
        Map<Integer, Integer> objectMap = new HashMap<Integer, Integer>();
        for (GroupObjectDescriptor groupObject : groupObjects) {
            objects.addAll(groupObject.objects);
            for (ObjectDescriptor object : groupObject.objects) {
                objectMap.put(object.getID(), groupObject.getID());
            }
        }
        ArrayList<ObjectDescriptor> objList = new ArrayList<ObjectDescriptor>();
        for (GroupObjectDescriptor groupObject : toDraw) {
            objList.addAll(groupObject.objects);
        }
        return getProperties(objects, objList, logics, objectMap, isCompulsory, isAny);
    }

    public static List<PropertyObjectDescriptor> getProperties(Collection<ObjectDescriptor> objects, Collection<ObjectDescriptor> atLeastOne, RemoteLogicsInterface logics, Map<Integer, Integer> objectMap, boolean isCompulsory, boolean isAny) {
        Map<Integer, ObjectDescriptor> idToObjects = new HashMap<Integer, ObjectDescriptor>();
        Map<Integer, ClientClass> classes = new HashMap<Integer, ClientClass>();
        for (ObjectDescriptor object : objects) {
            ClientClass cls = object.getBaseClass();
            if (cls != null) {
                idToObjects.put(object.getID(), object);
                classes.put(object.getID(), object.getBaseClass());
            }
        }

        Collection<PropertyDescriptorImplement<Integer>> properties = getProperties(logics, classes, BaseUtils.filterValues(idToObjects, atLeastOne).keySet(), objectMap, isCompulsory, isAny);

        List<PropertyObjectDescriptor> result = new ArrayList<PropertyObjectDescriptor>();
        for (PropertyDescriptorImplement<Integer> implement : properties) {
            result.add(implement.property.createPropertyObject(BaseUtils.join(implement.mapping, idToObjects)));
        }
        return result;
    }

    public static <K> Collection<PropertyDescriptorImplement<K>> getProperties(RemoteLogicsInterface logics, Map<K, ClientClass> classes) {
        // todo:
        return new ArrayList<PropertyDescriptorImplement<K>>();
    }

    public static Collection<PropertyDescriptorImplement<Integer>> getProperties(RemoteLogicsInterface logics, Map<Integer, ClientClass> classes, Collection<Integer> atLeastOne, Map<Integer, Integer> objectMap, boolean isCompulsory, boolean isAny) {
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

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(logics.getPropertyObjectsByteArray(outStream.toByteArray(), isCompulsory, isAny)));
            ClientSerializationPool pool = new ClientSerializationPool();

            List<PropertyDescriptorImplement<Integer>> result = new ArrayList<PropertyDescriptorImplement<Integer>>();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                PropertyDescriptor implementProperty = (PropertyDescriptor) pool.deserializeObject(inStream);
                Map<PropertyInterfaceDescriptor, Integer> mapInterfaces = new HashMap<PropertyInterfaceDescriptor, Integer>();
                for (int j = 0; j < implementProperty.interfaces.size(); j++) {
                    mapInterfaces.put((PropertyInterfaceDescriptor) pool.deserializeObject(inStream), inStream.readInt());
                }
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

        moveClientComponent(groupFrom.getClientComponent(client.mainContainer), getElementTo(groupObjects, groupFrom, index).getClientComponent(client.mainContainer));

        BaseUtils.moveElement(groupObjects, groupFrom, index);
        BaseUtils.moveElement(client.groupObjects, groupFrom.client, index);

        updateDependency(this, "groupObjects");

        return true;
    }

    public boolean moveTreeGroup(TreeGroupDescriptor treeGroupFrom, TreeGroupDescriptor treeGroupTo) {
        return moveTreeGroup(treeGroupFrom, treeGroups.indexOf(treeGroupTo) + (treeGroups.indexOf(treeGroupFrom) > treeGroups.indexOf(treeGroupTo) ? 0 : 1));
    }

    public boolean moveTreeGroup(TreeGroupDescriptor treeGroupFrom, int index) {

        moveClientComponent(treeGroupFrom.getClientComponent(client.mainContainer), getElementTo(treeGroups, treeGroupFrom, index).getClientComponent(client.mainContainer));

        BaseUtils.moveElement(treeGroups, treeGroupFrom, index);
        BaseUtils.moveElement(client.treeGroups, treeGroupFrom.client, index);

        updateDependency(this, "treeGroups");

        return true;
    }

    public boolean movePropertyDraw(PropertyDrawDescriptor propFrom, PropertyDrawDescriptor propTo) {
        return movePropertyDraw(propFrom, propertyDraws.indexOf(propTo) + (propertyDraws.indexOf(propFrom) > propertyDraws.indexOf(propTo) ? 0 : 1));
    }

    public boolean movePropertyDraw(PropertyDrawDescriptor propFrom, int index) {

        moveClientComponent(propFrom.client, getElementTo(propertyDraws, propFrom, index).client);

        BaseUtils.moveElement(propertyDraws, propFrom, index);
        BaseUtils.moveElement(client.propertyDraws, propFrom.client, index);

        updateDependency(this, "propertyDraws");
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

        updateDependency(this, "caption");
    }

    public void setDefaultOrders(OrderedMap<PropertyDrawDescriptor, Boolean> defaultOrders) {
        OrderedMap<ClientPropertyDraw, Boolean> clientOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
        for (Map.Entry<PropertyDrawDescriptor, Boolean> entry : defaultOrders.entrySet()) {
            clientOrders.put(entry.getKey().client, entry.getValue());
        }

        this.defaultOrders = defaultOrders;
        client.defaultOrders = clientOrders;

        context.updateDependency(this, "defaultOrders");
    }

    public String getCaption() {
        return caption;
    }

    private static void moveClientComponent(ClientComponent compFrom, ClientComponent compTo) {
        if (compFrom.container.equals(compTo.container)) {
            compFrom.container.moveChild(compFrom, compTo);
        }
    }

    public boolean addToPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.add(propertyDraw);
        client.propertyDraws.add(propertyDraw.client);

        updateDependency(this, "propertyDraws");
        return true;
    }

    public boolean removeFromPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        propertyDraws.remove(propertyDraw);
        client.removePropertyDraw(propertyDraw.client);

        updateDependency(this, "propertyDraws");
        return true;
    }

    public boolean addToGroupObjects(GroupObjectDescriptor groupObject) {

        groupObjects.add(groupObject);
        client.groupObjects.add(groupObject.client);

        addGroupObjectDefaultContainers(groupObject);

        updateDependency(this, "groupObjects");
        return true;
    }

    public boolean removeFromGroupObjects(GroupObjectDescriptor groupObject) {
        groupObjects.remove(groupObject);
        client.removeGroupObject(groupObject.client);

        updateDependency(this, "groupObjects");
        return true;
    }

    public boolean addToTreeGroups(TreeGroupDescriptor treeGroup) {

        treeGroups.add(treeGroup);
        client.treeGroups.add(treeGroup.client);

        addTreeGroupDefaultContainers(treeGroup);

        updateDependency(this, "treeGroups");
        return true;
    }

    public boolean removeFromTreeGroups(TreeGroupDescriptor treeGroup) {
        treeGroups.remove(treeGroup);
        client.removeTreeGroup(treeGroup.client);

        updateDependency(this, "treeGroups");
        return true;
    }

    public List<RegularFilterGroupDescriptor> getRegularFilterGroups() {
        return regularFilterGroups;
    }

    public void addToRegularFilterGroups(RegularFilterGroupDescriptor filterGroup) {
        regularFilterGroups.add(filterGroup);
        client.addToRegularFilterGroups(filterGroup.client);
        updateDependency(this, "regularFilterGroups");
    }

    public void removeFromRegularFilterGroups(RegularFilterGroupDescriptor filterGroup) {
        regularFilterGroups.remove(filterGroup);
        client.removeFromRegularFilterGroups(filterGroup.client);
        updateDependency(this, "regularFilterGroups");
    }


    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        new ClientSerializationPool().serializeObject(dataStream, this);
        new ClientSerializationPool().serializeObject(dataStream, client);

        return outStream.toByteArray();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        new ClientSerializationPool().serializeObject(outStream, this);
        new ClientSerializationPool().serializeObject(outStream, client);
    }

    public static FormDescriptor deserialize(byte[] richDesignByteArray, byte[] formEntityByteArray) throws IOException {
        ApplicationContext context = new ApplicationContext();
        ClientForm richDesign = new ClientSerializationPool(context)
                .deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(richDesignByteArray)));

        return new ClientSerializationPool(richDesign, context)
                .deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(formEntityByteArray)));
    }

    private class FormContainerFactory implements ContainerFactory<ClientContainer> {
        public ClientContainer createContainer() {
            return new ClientContainer(getContext());
        }
    }

    private void addFormDefaultContainers() {
        FormContainerSet.fillContainers(client, new FormContainerFactory());
    }

    private void addGroupObjectDefaultContainers(GroupObjectDescriptor group) {
        GroupObjectContainerSet<ClientContainer, ClientComponent> set = GroupObjectContainerSet.create(group.client, new FormContainerFactory());

        moveContainerInGroup(group, set.getGroupContainer(), groupObjects);
    }

    private void addTreeGroupDefaultContainers(TreeGroupDescriptor treeGroup) {

        TreeGroupContainerSet<ClientContainer, ClientComponent> set = TreeGroupContainerSet.create(treeGroup.client, new FormContainerFactory());

        moveContainerInGroup(treeGroup, set.getTreeContainer(), treeGroups);
    }

    private void moveContainerInGroup(ContainerMovable<ClientContainer> concreateObject, ClientContainer parent, List<? extends ContainerMovable<ClientContainer>> objects) {
        // вставляем контейнер после предыдущего
        int groupIndex = objects.indexOf(concreateObject);
        int index = -1;
        if (groupIndex > 0) {
            index = client.mainContainer.children.indexOf(objects.get(groupIndex - 1).getClientComponent(client.mainContainer));
            if (index != -1) {
                index++;
            } else {
                index = client.mainContainer.children.size();
            }
        } else {
            index = 0;
        }

        client.mainContainer.add(index, parent);
    }
}
