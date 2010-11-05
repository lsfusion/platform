package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.Main;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertyDrawDescriptor extends IdentityDescriptor implements ClientIdentitySerializable, ContainerMovable {
    public ClientPropertyDraw client = new ClientPropertyDraw();

    private PropertyObjectDescriptor propertyObject;

    public PropertyDrawDescriptor() {
    }

    public PropertyDrawDescriptor(PropertyObjectDescriptor propertyObject) {
        setID(Main.generateNewID());
        setPropertyObject(propertyObject);
    }

    //todo: временно public...
    public GroupObjectDescriptor toDraw;
    private boolean shouldBeLast;
    private ClassViewType forceViewType;

    private PropertyObjectDescriptor propertyCaption;

    private List<GroupObjectDescriptor> columnGroupObjects = new ArrayList<GroupObjectDescriptor>();

    public void setPropertyObject(PropertyObjectDescriptor propertyObject) { // usage через reflection
        this.propertyObject = propertyObject;
        IncrementDependency.update(this, "propertyObject");
    }

    public PropertyObjectDescriptor getPropertyObject() {
        return propertyObject;
    }

    public void setToDraw(GroupObjectDescriptor toDraw) { // usage через reflection
        this.toDraw = toDraw;
        IncrementDependency.update(this, "toDraw");
    }

    public GroupObjectDescriptor getToDraw() {
        return toDraw;
    }

    public void setShouldBeLast(boolean shouldBeLast) {
        this.shouldBeLast = shouldBeLast;
    }

    public boolean getShouldBeLast() {
        return shouldBeLast;
    }

    public void setForceViewType(String forceViewType) {
        this.forceViewType = ClassViewType.valueOf(forceViewType);
        IncrementDependency.update(this, "forceViewType");
    }

    public ClassViewType getForceViewType() {
        return forceViewType;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (toDraw != null) {
            return toDraw;
        } else {
            return propertyObject != null
                   ? propertyObject.getGroupObject(groupList)
                   : null;
        }
    }

    public ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects) {
        GroupObjectDescriptor groupObject = getGroupObject(groupObjects);
        if (groupObject != null) {
            return parent.findContainerBySID(GroupObjectContainerSet.PANEL_CONTAINER + groupObject.getID());
        } else
            return null;
    }

    public ClientComponent getClientComponent(ClientContainer parent) {
        return client;
    }

    public List<GroupObjectDescriptor> getUpGroupObjects(List<GroupObjectDescriptor> groupList) {
        if (getPropertyObject() == null) {
            return new ArrayList<GroupObjectDescriptor>();
        }

        List<GroupObjectDescriptor> groupObjects = getPropertyObject().getGroupObjects(groupList);
        if (toDraw == null) {
            if (groupObjects.size() > 0) {
                return groupObjects.subList(0, groupObjects.size() - 1);
            } else {
                return groupObjects;
            }
        } else {
            return BaseUtils.removeList(groupObjects, Collections.singleton(toDraw));
        }
    }

    public List<GroupObjectDescriptor> getColumnGroupObjects() { // usage через reflection
        return columnGroupObjects;
    }

    public void setColumnGroupObjects(List<GroupObjectDescriptor> columnGroupObjects) {
        this.columnGroupObjects = columnGroupObjects;

        client.columnGroupObjects = new ArrayList<ClientGroupObject>();
        for (GroupObjectDescriptor group : columnGroupObjects) {
            client.columnGroupObjects.add(group.client);
        }
        
        IncrementDependency.update(this, "columnGroupObjects");
    }

    public PropertyObjectDescriptor getPropertyCaption() { // usage через reflection
        return propertyCaption;
    }

    public void setPropertyCaption(PropertyObjectDescriptor propertyCaption) {
        this.propertyCaption = propertyCaption;
        IncrementDependency.update(this, "propertyCaption");
    }

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        IncrementDependency.update(this, "caption");
    }

    public String getCaption() {
        return client.caption;
    }

    @Override
    public String toString() {
        return !BaseUtils.isRedundantString(client.caption)
               ? client.caption
               : propertyObject != null
                 ? propertyObject.property.caption
                 : "Неопределённое свойство";
    }

    @Override
    public void setID(int ID) {
        super.setID(ID);
        client.setID(ID);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
        pool.serializeObject(outStream, propertyCaption);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null) {
            pool.writeString(outStream, forceViewType.name());
        }
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        propertyObject = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        toDraw = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));
        }

        client = pool.context.getProperty(ID);
    }
}
