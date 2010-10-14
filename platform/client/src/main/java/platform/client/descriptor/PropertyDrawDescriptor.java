package platform.client.descriptor;

import platform.client.logics.ClientPropertyDraw;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.descriptor.increment.IncrementDependency;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collections;

public class PropertyDrawDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        IncrementDependency.update(this, "caption");
    }
    public String getCaption() {
        return client.caption;
    }

    @Override
    public String toString() {
        return client.caption;
    }

    public ClientPropertyDraw client;

    public PropertyObjectDescriptor propertyObject;
    public void setPropertyObject(PropertyObjectDescriptor propertyObject) { // usage через reflection
        this.propertyObject = propertyObject;
        IncrementDependency.update(this, "propertyObject");        
    }
    public PropertyObjectDescriptor getPropertyObject() {
        return propertyObject;
    }

    private GroupObjectDescriptor toDraw;
    public void setToDraw(GroupObjectDescriptor toDraw) { // usage через reflection
        this.toDraw = toDraw;
        IncrementDependency.update(this, "toDraw");
    }
    public GroupObjectDescriptor getToDraw() {
        return toDraw;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if(toDraw!=null)
            return toDraw;
        else
            return propertyObject.getGroupObject(groupList);
    }
    public List<GroupObjectDescriptor> getUpGroupObjects(List<GroupObjectDescriptor> groupList) {
        List<GroupObjectDescriptor> groupObjects = getPropertyObject().getGroupObjects(groupList);
        if(toDraw==null)
            return groupObjects.subList(0, groupObjects.size()-1);
        else
            return BaseUtils.removeList(groupObjects, Collections.singleton(toDraw));
    }
    
    private List<GroupObjectDescriptor> columnGroupObjects;
    public List<GroupObjectDescriptor> getColumnGroupObjects() { // usage через reflection
        return columnGroupObjects;
    }
    public void setColumnGroupObjects(List<GroupObjectDescriptor> columnGroupObjects) {
        this.columnGroupObjects = columnGroupObjects;
        IncrementDependency.update(this, "columnGroupObjects");
    }

    private PropertyObjectDescriptor propertyCaption;
    public PropertyObjectDescriptor getPropertyCaption() { // usage через reflection
        return propertyCaption;
    }
    public void setPropertyCaption(PropertyObjectDescriptor propertyCaption) {
        this.propertyCaption = propertyCaption;
        IncrementDependency.update(this, "propertyCaption");
    }

    private boolean shouldBeLast;
    private Byte forceViewType;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
        pool.serializeObject(outStream, propertyCaption);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null) {
            outStream.writeByte(forceViewType);
        }
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        
        propertyObject = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        toDraw = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = inStream.readByte();
        }

        client = pool.context.getProperty(ID);
    }
}
