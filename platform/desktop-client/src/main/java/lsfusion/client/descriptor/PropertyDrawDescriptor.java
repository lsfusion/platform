package lsfusion.client.descriptor;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.ContextIdentityObject;
import lsfusion.base.context.ApplicationContext;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertyDrawDescriptor extends ContextIdentityObject implements ClientIdentitySerializable, ContainerMovable<ClientComponent>, CustomConstructible {
    public ClientPropertyDraw client;

    private PropertyObjectDescriptor propertyObject;
    private CalcPropertyObjectDescriptor propertyCaption;
    private CalcPropertyObjectDescriptor propertyReadOnly;
    private CalcPropertyObjectDescriptor propertyFooter;
    private CalcPropertyObjectDescriptor propertyBackground;
    private CalcPropertyObjectDescriptor propertyForeground;

    private PropertyEditType editType;

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
            return parent.findContainerBySID(groupObject.getSID() + GroupObjectContainerSet.PANEL_CONTAINER);
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

    public CalcPropertyObjectDescriptor getPropertyCaption() { // usage через reflection
        return propertyCaption;
    }

    public void setPropertyCaption(CalcPropertyObjectDescriptor propertyCaption) {
        this.propertyCaption = propertyCaption;
        updateDependency(this, "propertyCaption");
    }

    public CalcPropertyObjectDescriptor getPropertyBackground() { // usage через reflection
        return propertyBackground;
    }

    public void setPropertyBackground(CalcPropertyObjectDescriptor propertyBackground) {
        this.propertyBackground = propertyBackground;
        updateDependency(this, "propertyBackground");
    }

    public CalcPropertyObjectDescriptor getPropertyForeground() { // usage через reflection
        return propertyForeground;
    }

    public void setPropertyForeground(CalcPropertyObjectDescriptor propertyForeground) {
        this.propertyForeground = propertyForeground;
        updateDependency(this, "propertyForeground");
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

    public void setEditType(String editType) { // usage через reflection
        this.editType = PropertyEditType.valueOf(editType);
        client.editType = PropertyEditType.valueOf(editType);
        getContext().updateDependency(this, "editType");
    }

    public PropertyEditType getEditType() {
        return editType;
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
        pool.serializeObject(outStream, propertyForeground);

        outStream.writeBoolean(shouldBeLast);

        outStream.writeBoolean(editType != null);
        if (editType != null)
            outStream.writeByte(editType.serialize());

        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null)
            pool.writeString(outStream, forceViewType.name());
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        propertyObject = pool.deserializeObject(inStream);
        toDraw = pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = pool.deserializeObject(inStream);
        propertyReadOnly = pool.deserializeObject(inStream);
        propertyFooter = pool.deserializeObject(inStream);
        propertyBackground = pool.deserializeObject(inStream);
        propertyForeground = pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean())
            editType = PropertyEditType.deserialize(inStream.readByte());
        if (inStream.readBoolean())
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));

        client = pool.context.getProperty(ID);
        client.setDescriptor(this);
    }

    public void customConstructor() {
        client = new ClientPropertyDraw(getID(), getContext());
        client.setDescriptor(this);
    }
}
