package platform.server.form.entity;

import platform.interop.serialization.SerializationPool;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class CurrentComputerEntity implements PropertyObjectInterfaceEntity {

    private CurrentComputerEntity() {
    }
    public static final CurrentComputerEntity instance = new CurrentComputerEntity();

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
