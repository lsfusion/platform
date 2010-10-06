package platform.server.form.entity;

import platform.base.IdentityObject;
import platform.server.classes.ClassSerializer;
import platform.server.classes.ValueClass;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, ServerIdentitySerializable {

    public GroupObjectEntity groupTo;

    public String caption;

    public boolean addOnTransaction = false;
    public boolean resetOnApply = false;

    public ValueClass baseClass;

    public ObjectEntity() {

    }
    
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

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
    }

    public void customDeserialize(ServerSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectEntity) pool.deserializeObject(inStream);
        baseClass = ClassSerializer.deserialize(pool.context, inStream);
        caption = inStream.readUTF();
    }
}
