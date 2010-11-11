package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.IdentityObject;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientObject extends IdentityObject implements Serializable, ClientIdentitySerializable {

    public String caption;
    public boolean addOnTransaction;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public ClientClassChooser classChooser = new ClientClassChooser();

    public ClientObject() {

    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, classChooser);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupObject = pool.deserializeObject(inStream);

        caption = pool.readString(inStream);

        addOnTransaction = inStream.readBoolean();

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);

        classChooser = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return !BaseUtils.isRedundantString(caption)
                ? caption
                : !BaseUtils.isRedundantString(baseClass)
                ? baseClass.toString()
                : "Неопределённый объект";
    }
}
