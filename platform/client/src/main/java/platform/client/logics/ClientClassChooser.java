package platform.client.logics;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
        
    }

    public ClientObject object;

    public ClientClassChooser(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        object = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "Дерево классов (" + object.toString() + ")";
    }
}
