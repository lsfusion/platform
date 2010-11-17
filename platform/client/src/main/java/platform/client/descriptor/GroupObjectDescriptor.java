package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextHolder;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupObjectDescriptor extends ArrayList<ObjectDescriptor> implements ClientIdentitySerializable,
                                                                          ContainerMovable<ClientContainer>,
                                                                          ApplicationContextHolder, CustomConstructible {
    private int ID;
    
    private ClassViewType initClassView = ClassViewType.GRID;
    private List<ClassViewType> banClassViewList = new ArrayList<ClassViewType>();
    private PropertyObjectDescriptor propertyHighlight;

    private ApplicationContext context;
    public ApplicationContext getContext() {
        return context;
    }
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public void updateDependency(Object object, String field) {
        context.updateDependency(object, field);
    }

    public ClientGroupObject client;

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ID;
        return result;
    }

    public List<ClassViewType> getBanClassViewList() {
        return banClassViewList;
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

    public PropertyObjectDescriptor getPropertyHighlight() {
        return propertyHighlight;
    }

    public void setPropertyHighlight(PropertyObjectDescriptor propertyHighlight) {
        this.propertyHighlight = propertyHighlight;
        updateDependency(this, "propertyHighlight");
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassViewList);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);
        initClassView = ClassViewType.values()[pool.readInt(inStream)];
        banClassViewList = (List<ClassViewType>)pool.readObject(inStream);
        propertyHighlight = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

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
        return moveObject(objectFrom, indexOf(objectTo) + (indexOf(objectFrom) > indexOf(objectTo) ? 0 : 1));
    }

    public boolean moveObject(ObjectDescriptor objectFrom, int index) {
        BaseUtils.moveElement(this, objectFrom, index);
        BaseUtils.moveElement(client, objectFrom.client, index);

        updateDependency(this, "objects");
        return true;
    }

    public boolean addToObjects(ObjectDescriptor object) {
        add(object);
        object.groupTo = this;
        client.add(object.client);
        object.client.groupObject = client;

        updateDependency(this, "objects");
        return true;
    }

    public boolean removeFromObjects(ObjectDescriptor object) {
        client.remove(object.client);
        remove(object);

        updateDependency(this, "objects");
        return true;
    }

    public ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects) {
        return parent;
    }

    public ClientContainer getClientComponent(ClientContainer parent) {
        return client.getClientComponent(parent);
    }
}
