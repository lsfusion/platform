package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.base.context.ContextIdentityObject;
import platform.base.context.ApplicationContext;
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

public class PropertyDrawDescriptor extends ContextIdentityObject implements ClientIdentitySerializable, ContainerMovable<ClientComponent>, CustomConstructible {
    public ClientPropertyDraw client;

    private PropertyObjectDescriptor propertyObject;
    private PropertyObjectDescriptor propertyCaption;
    private PropertyObjectDescriptor propertyReadOnly;
    private PropertyObjectDescriptor propertyFooter;
    private PropertyObjectDescriptor propertyBackground;

    private boolean readOnly;

    public PropertyDrawDescriptor() {
    }

    public PropertyDrawDescriptor(ApplicationContext context, PropertyObjectDescriptor propertyObject) {
        super(context);

        customConstructor();
        setPropertyObject(propertyObject);
    }

    //todo: временно public...
    public GroupObjectDescriptor toDraw;
    private boolean shouldBeLast;
    private ClassViewType forceViewType;

    private List<GroupObjectDescriptor> columnGroupObjects = new ArrayList<GroupObjectDescriptor>();

    public void setPropertyObject(PropertyObjectDescriptor propertyObject) { // usage через reflection
        this.propertyObject = propertyObject;
        updateDependency(this, "propertyObject");
    }

    public PropertyObjectDescriptor getPropertyObject() {
        return propertyObject;
    }

    public GroupObjectDescriptor addGroup = null;

    public void setToDraw(GroupObjectDescriptor toDraw) { // usage через reflection
        this.toDraw = toDraw;
        client.groupObject = toDraw==null?null:toDraw.client;

        updateDependency(this, "toDraw");
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
        updateDependency(this, "forceViewType");
    }

    public ClassViewType getForceViewType() {
        return forceViewType;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (getToDraw() != null) {
            return getToDraw();
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
        if (getToDraw() == null) {
            if (groupObjects.size() > 0) {
                return groupObjects.subList(0, groupObjects.size() - 1);
            } else {
                return groupObjects;
            }
        } else {
            return BaseUtils.removeList(groupObjects, Collections.singleton(getToDraw()));
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
        
        updateDependency(this, "columnGroupObjects");
    }

    public PropertyObjectDescriptor getPropertyCaption() { // usage через reflection
        return propertyCaption;
    }

    public void setPropertyCaption(PropertyObjectDescriptor propertyCaption) {
        this.propertyCaption = propertyCaption;
        updateDependency(this, "propertyCaption");
    }

    public PropertyObjectDescriptor getPropertyBackground() { // usage через reflection
        return propertyBackground;
    }

    public void setPropertyBackground(PropertyObjectDescriptor propertyBackground) {
        this.propertyBackground = propertyBackground;
        updateDependency(this, "propertyBackground");
    }

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        updateDependency(this, "caption");
    }

    public void setFocusable(Boolean focusable) {
        client.focusable = focusable;
        getContext().updateDependency(this, "focusable");
    }

    public Boolean getFocusable() {
        return client.focusable;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        client.readOnly = readOnly;
        getContext().updateDependency(this, "readOnly");
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public String getCaption() {
        return client.caption;
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
        pool.serializeObject(outStream, propertyCaption);
        pool.serializeObject(outStream, propertyReadOnly);
        pool.serializeObject(outStream, propertyFooter);
        pool.serializeObject(outStream, propertyBackground);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(readOnly);
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
        propertyReadOnly = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        propertyFooter = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        propertyBackground = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        readOnly = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));
        }

        client = pool.context.getProperty(ID);
        client.setDescriptor(this);
    }

    public void customConstructor() {
        client = new ClientPropertyDraw(getID(), getContext());
        client.setDescriptor(this);
    }
}
