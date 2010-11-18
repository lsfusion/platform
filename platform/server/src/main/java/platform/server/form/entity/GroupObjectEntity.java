package platform.server.form.entity;

import platform.base.IdentityObject;
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
import java.util.List;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance>, ServerIdentitySerializable {

    public List<ObjectEntity> objects = new ArrayList<ObjectEntity>();

    public GroupObjectEntity() {
    }
    
    public GroupObjectEntity(int ID) {
        super(ID);
        assert (ID < RemoteFormInterface.GID_SHIFT);
    }

    public boolean add(ObjectEntity objectEntity) {
        objectEntity.groupTo = this;
        return objects.add(objectEntity);
    }

    public ClassViewType initClassView = ClassViewType.GRID;
    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();
    public int pageSize = 10;

    public PropertyObjectEntity propertyHighlight;

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassView);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(objects, inStream);
        initClassView = ClassViewType.values()[pool.readInt(inStream)];
        banClassView = (List<ClassViewType>)pool.readObject(inStream);
        propertyHighlight = (PropertyObjectEntity) pool.deserializeObject(inStream);
    }
}
