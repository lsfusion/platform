package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupObjectDescriptor extends ArrayList<ObjectDescriptor> implements ClientIdentitySerializable {
    private int ID;
    
    private ClassViewType initClassView;
    private List<ClassViewType> banClassViewList = new ArrayList<ClassViewType>();
    private PropertyObjectDescriptor propertyHighlight;


    public ClientGroupObject client = new ClientGroupObject();

    public List<ClassViewType> getBanClassViewList() {
        return banClassViewList;
    }

    public void setBanClassViewList(List<String> banClassViewList) {
        this.banClassViewList.clear();
        for(String one : banClassViewList){
            this.banClassViewList.add(ClassViewType.valueOf(one));
        }
        IncrementDependency.update(this, "banClassViewList");

        setBanClassView(this.banClassViewList);
    }

    public ClassViewType getInitClassView() {
        return initClassView;
    }

    public void setInitClassView(String initClassView) {
        this.initClassView = ClassViewType.valueOf(initClassView);
        IncrementDependency.update(this, "initClassView");
    }

    public List<ClassViewType> getBanClassView() {
        return client.banClassView;
    }

    public void setBanClassView(List<ClassViewType> banClassView) {
        client.banClassView = banClassView;
        IncrementDependency.update(this, "banClassView");
    }

    public PropertyObjectDescriptor getPropertyHighlight() {
        return propertyHighlight;
    }

    public void setPropertyHighlight(PropertyObjectDescriptor propertyHighlight) {
        this.propertyHighlight = propertyHighlight;
        IncrementDependency.update(this, "propertyHighlight");
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        if (initClassView != null) {
            pool.writeString(outStream, initClassView.name());
        }
        if(banClassViewList != null){
            pool.writeObject(outStream, banClassViewList);
        }
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);
        initClassView = ClassViewType.valueOf(pool.readString(inStream));
        banClassViewList = (List<ClassViewType>)pool.readObject(inStream);
        propertyHighlight = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getGroupObject(ID);
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public boolean moveObject(ObjectDescriptor objectFrom, ObjectDescriptor objectTo) {
        return moveObject(objectFrom, indexOf(objectTo) + (indexOf(objectFrom) > indexOf(objectTo) ? 0 : 1));
    }

    public boolean moveObject(ObjectDescriptor objectFrom, int index) {
        BaseUtils.moveElement(this, objectFrom, index);
        BaseUtils.moveElement(client, objectFrom.client, index);
        IncrementDependency.update(this, "objects");

        IncrementDependency.update(this, "objects");
        return true;
    }

    public boolean addToObjects(ObjectDescriptor object) {
        add(object);
        object.groupTo = this;
        client.add(object.client);
        object.client.groupObject = client;

        IncrementDependency.update(this, "objects");
        return true;
    }

    public boolean removeFromObjects(ObjectDescriptor object) {
        client.remove(object.client);
        remove(object);

        IncrementDependency.update(this, "objects");
        return true;
    }
}
