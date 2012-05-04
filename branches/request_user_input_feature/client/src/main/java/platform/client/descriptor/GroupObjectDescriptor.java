package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.base.context.ApplicationContextHolder;
import platform.base.context.ContextIdentityObject;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupObjectDescriptor extends ContextIdentityObject implements ClientIdentitySerializable,
        ContainerMovable<ClientContainer>,
        ApplicationContextHolder, CustomConstructible {

    public ClientGroupObject client;

    private ClassViewType initClassView = ClassViewType.GRID;
    private List<ClassViewType> banClassViewList = new ArrayList<ClassViewType>();
    private PropertyObjectDescriptor propertyBackground;
    private PropertyObjectDescriptor propertyForeground;
    private PropertyObjectDescriptor reportPathProperty;
    private PropertyDrawDescriptor filterProperty;

    public List<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();

    private Map<ObjectDescriptor, PropertyObjectDescriptor> isParent = new HashMap<ObjectDescriptor, PropertyObjectDescriptor>();
    private TreeGroupDescriptor parent;
    private Integer pageSize;

    public void setIsParent(Map<ObjectDescriptor, PropertyObjectDescriptor> isParent) {
        this.isParent = isParent;
        getContext().updateDependency(this, "isParent");
    }

    public Map<ObjectDescriptor, PropertyObjectDescriptor> getIsParent() {
        return isParent;
    }

    public TreeGroupDescriptor getParent() {
        return parent;
    }

    public void setParent(TreeGroupDescriptor parent) {
        this.parent = parent;
    }

    public List<ClassViewType> getBanClassViewList() {
        return banClassViewList;
    }

    public void setPageSize(String pageSize) {
        if (pageSize.equals("")) {
            this.pageSize = null;
        } else {
            this.pageSize = Integer.parseInt(pageSize);
        }
    }

    public String getPageSize() {
        if (pageSize != null) {
            return String.valueOf(pageSize);
        } else {
            return "";
        }
    }

    public void setBanClassViewList(List<ClassViewType> banClassViewList) {
        this.banClassViewList.clear();
        this.banClassViewList.addAll(banClassViewList);

        updateDependency(this, "banClassViewList");

        setBanClassView(this.banClassViewList);
    }

    public ClassViewType getInitClassView() {
        return initClassView;
    }

    public void setInitClassView(ClassViewType initClassView) {
        this.initClassView = initClassView;
        updateDependency(this, "initClassView");
    }

    public List<ClassViewType> getBanClassView() {
        return client.banClassView;
    }

    public void setBanClassView(List<ClassViewType> banClassView) {
        client.banClassView = banClassView;
        updateDependency(this, "banClassView");
    }

    public PropertyObjectDescriptor getPropertyBackground() {
        return propertyBackground;
    }

    public void setPropertyBackground(PropertyObjectDescriptor propertyBackground) {
        this.propertyBackground = propertyBackground;
        updateDependency(this, "propertyBackground");
    }

    public PropertyObjectDescriptor getPropertyForeground() {
        return propertyForeground;
    }

    public void setPropertyForeground(PropertyObjectDescriptor propertyForeground) {
        this.propertyForeground = propertyForeground;
        updateDependency(this, "propertyForeground");
    }

    public PropertyDrawDescriptor getFilterProperty() {
        return filterProperty;
    }

    public void setFilterProperty(PropertyDrawDescriptor filterProperty) {
        this.filterProperty = filterProperty;
        this.client.filterProperty = filterProperty == null ? null : filterProperty.client;
        updateDependency(this, "filterProperty");
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassViewList);
        pool.serializeObject(outStream, parent);
        pool.serializeObject(outStream, propertyBackground);
        pool.serializeObject(outStream, propertyForeground);
        pool.serializeObject(outStream, filterProperty);
        outStream.writeBoolean(!isParent.isEmpty());
        if (!isParent.isEmpty()) {
            pool.serializeMap(outStream, isParent);
        }
        pool.writeObject(outStream, pageSize);
        pool.serializeObject(outStream, reportPathProperty);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(objects, inStream);
        initClassView = ClassViewType.values()[pool.readInt(inStream)];
        banClassViewList = pool.readObject(inStream);
        parent = pool.deserializeObject(inStream);
        propertyBackground = pool.deserializeObject(inStream);
        propertyForeground = pool.deserializeObject(inStream);
        filterProperty = pool.deserializeObject(inStream);
        if (inStream.readBoolean()) {
            isParent = pool.deserializeMap(inStream);
        }
        pageSize = pool.readObject(inStream);
        reportPathProperty = pool.deserializeObject(inStream);

        client = pool.context.getGroupObject(ID);
    }

    public void customConstructor() {
        client = new ClientGroupObject(getID(), getContext());
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public boolean moveObject(ObjectDescriptor objectFrom, ObjectDescriptor objectTo) {
        return moveObject(objectFrom, objects.indexOf(objectTo) + (objects.indexOf(objectFrom) > objects.indexOf(objectTo) ? 0 : 1));
    }

    public boolean moveObject(ObjectDescriptor objectFrom, int index) {
        BaseUtils.moveElement(objects, objectFrom, index);
        BaseUtils.moveElement(client.objects, objectFrom.client, index);

        updateDependency(this, "objects");
        return true;
    }

    public boolean addToObjects(ObjectDescriptor object) {
        objects.add(object);
        object.groupTo = this;
        client.objects.add(object.client);
        object.client.groupObject = client;

        updateDependency(this, "objects");
        return true;
    }

    public boolean removeFromObjects(ObjectDescriptor object) {
        client.objects.remove(object.client);
        objects.remove(object);

        updateDependency(this, "objects");
        return true;
    }

    public ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects) {
        return parent;
    }

    public ClientContainer getClientComponent(ClientContainer parent) {
        return client.getClientComponent(parent);
    }

    public String getVariableName() {
        if (objects.size() == 1) {
            return objects.get(0).getVariableName() + ".groupTo";
        }
        String name = getClassNames();
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return "grObj" + name;
    }

    public String getClassNames() {
        StringBuilder result = new StringBuilder("");
        for (ObjectDescriptor obj : objects) {
            result.append(obj.getBaseClass().getSID());
        }
        return result.toString();
    }

}
