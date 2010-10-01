package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.interop.form.RemoteFormInterface;
import platform.interop.serialization.IdentitySerializable;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectEntity extends ArrayList<ObjectEntity> implements Instantiable<GroupObjectInstance>, IdentitySerializable {
    private final int ID;
    public int getID() {
        return ID;
    }

    public GroupObjectEntity(int iID) {
        ID = iID;
        assert (ID < RemoteFormInterface.GID_SHIFT);
    }

    @Override
    public boolean add(ObjectEntity objectEntity) {
        objectEntity.groupTo = this;
        return super.add(objectEntity);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public byte initClassView = ClassViewType.GRID;
    public byte banClassView = 0;
    public int pageSize = 10;

    public PropertyObjectEntity propertyHighlight;

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        outStream.writeByte(initClassView);
        outStream.writeByte(banClassView);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
