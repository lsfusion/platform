package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.interop.form.RemoteFormInterface;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectEntity extends ArrayList<ObjectEntity> implements Instantiable<GroupObjectInstance>, ServerIdentitySerializable {
    private int ID;
    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public GroupObjectEntity() {
    }
    
    public GroupObjectEntity(int iID) {
        ID = iID;
        assert (ID < RemoteFormInterface.GID_SHIFT);
    }

    @Override
    public boolean add(ObjectEntity objectEntity) {
        objectEntity.groupTo = this;
        return super.add(objectEntity);
    }

    public byte initClassView = ClassViewType.GRID;
    public byte banClassView = 0;
    public int pageSize = 10;

    public PropertyObjectEntity propertyHighlight;

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        outStream.writeByte(initClassView);
        outStream.writeByte(banClassView);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);
        initClassView = inStream.readByte();
        banClassView = inStream.readByte();
        propertyHighlight = (PropertyObjectEntity) pool.deserializeObject(inStream);
    }
}
