package platform.server.form.entity;

import platform.base.IdentityObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;
import platform.server.classes.ValueClass;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, IdentitySerializable {

    public GroupObjectEntity groupTo;

    public String caption;

    public boolean addOnTransaction = false;
    public boolean resetOnApply = false;

    public final ValueClass baseClass;

    public ObjectEntity(int ID, ValueClass baseClass, String caption) {
        super(ID);
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
