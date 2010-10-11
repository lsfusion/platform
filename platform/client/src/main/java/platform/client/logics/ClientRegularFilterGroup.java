package platform.client.logics;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientRegularFilterGroup extends ClientComponent {
    
    public int ID;
    public List<ClientRegularFilter> filters = new ArrayList<ClientRegularFilter>();

    public int defaultFilter = -1;

    public ClientRegularFilterGroup() {

    }
    
    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeInt(ID);
        pool.serializeCollection(outStream, filters);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        ID = inStream.readInt();

        filters = pool.deserializeList(inStream);

        defaultFilter = inStream.readInt();
    }

    @Override
    public String toString() {
        return filters.toString();
    }
}
