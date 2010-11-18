package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.descriptor.context.ContextIdentityDescriptor;
import platform.interop.context.ApplicationContextHolder;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupObjectDescriptor extends ContextIdentityDescriptor implements ClientIdentitySerializable,
                                                                          ContainerMovable<ClientContainer>,
                                                                          ApplicationContextHolder, CustomConstructible {

    private ClassViewType initClassView = ClassViewType.GRID;
    private List<ClassViewType> banClassViewList = new ArrayList<ClassViewType>();
    private PropertyObjectDescriptor propertyHighlight;

    public List<ObjectDescriptor> objects = new ArrayList<ObjectDescriptor>();

    public ClientGroupObject client;

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

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassViewList);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(objects, inStream);
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
}
