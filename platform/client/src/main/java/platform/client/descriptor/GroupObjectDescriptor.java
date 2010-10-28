package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.ClassViewTypeEnum;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupObjectDescriptor extends ArrayList<ObjectDescriptor> implements ClientIdentitySerializable {
    private int ID;
    
    private ClassViewTypeEnum initClassView;
    private List<ClassViewTypeEnum> banClassViewList = new ArrayList<ClassViewTypeEnum>();
    private PropertyObjectDescriptor propertyHighlight;


    public ClientGroupObject client = new ClientGroupObject();

    public List<ClassViewTypeEnum> getBanClassViewList() {
        return banClassViewList;
    }

    public void setBanClassViewList(List<String> banClassViewList) {
        this.banClassViewList.clear();
        for(String one : banClassViewList){
            this.banClassViewList.add(ClassViewTypeEnum.valueOf(one));
        }
        IncrementDependency.update(this, "banClassViewList");

        setBanClassView(this.banClassViewList);
    }

    public ClassViewTypeEnum getInitClassView() {
        return initClassView;
    }

    public void setInitClassView(String initClassView) {
        this.initClassView = ClassViewTypeEnum.valueOf(initClassView);
        IncrementDependency.update(this, "initClassView");
    }

    public List<ClassViewTypeEnum> getBanClassView() {
        return client.banClassView;
    }

    public void setBanClassView(List<ClassViewTypeEnum> banClassView) {
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
            if(initClassView.ordinal() == 0){
                outStream.writeByte(ClassViewType.PANEL);
            }
            else if(initClassView.ordinal() == 1){
                outStream.writeByte(ClassViewType.GRID);
            }
            else if(initClassView.ordinal() == 2){
                outStream.writeByte(ClassViewType.HIDE);
            }
        }
        byte banViews = 0;
        if (banClassViewList != null) {
            for (ClassViewTypeEnum one : banClassViewList) {
                if(one == ClassViewTypeEnum.valueOf("Panel")){
                    banViews |= ClassViewType.PANEL;
                }
                if(one == ClassViewTypeEnum.valueOf("Grid")){
                    banViews |= ClassViewType.GRID;
                }
                if(one == ClassViewTypeEnum.valueOf("Hide")){
                    banViews |= ClassViewType.HIDE;
                }
            }
        }
        outStream.writeByte(banViews);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);
        byte type = inStream.readByte();
        if(type == ClassViewType.PANEL){
            initClassView = ClassViewTypeEnum.valueOf("Panel");
        }
        if(type == ClassViewType.GRID){
            initClassView = ClassViewTypeEnum.valueOf("Grid");
        }
        if(type == ClassViewType.HIDE){
            initClassView = ClassViewTypeEnum.valueOf("Hide");
        }

        type = inStream.readByte();
        if((type & ClassViewType.PANEL) != 0){
            banClassViewList.add(ClassViewTypeEnum.valueOf("Panel"));
        }
        if((type & ClassViewType.GRID) != 0){
            banClassViewList.add(ClassViewTypeEnum.valueOf("Grid"));
        }
        if((type & ClassViewType.HIDE) != 0){
            banClassViewList.add(ClassViewTypeEnum.valueOf("Hide"));
        }
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
