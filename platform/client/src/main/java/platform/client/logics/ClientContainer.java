package platform.client.logics;

import platform.client.descriptor.nodes.ComponentNode;
import platform.client.descriptor.nodes.ContainerNode;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientContainer extends ClientComponent implements ClientIdentitySerializable {
    public String title;
    public String description;

    public List<ClientComponent> children;

    public ClientContainer() {

    }

    public ClientContainer(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        if (inStream.readBoolean()) {
            title = inStream.readUTF();
        }
        
        if (!inStream.readBoolean()) {
            description = inStream.readUTF();
        }
    }

    @Override
    public String toString() {
        String result = title == null ? "" : title;
        if (description == null)
            result += " (";
        else
            result += (result.isEmpty() ? "" : " ") + "(" + description + ",";
        result += getID();
        result += ")";
        return result;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, title);
        pool.writeString(outStream, description);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);
    }

    @Override
    public ComponentNode getNode() {
        return new ContainerNode(this);
    }
}
