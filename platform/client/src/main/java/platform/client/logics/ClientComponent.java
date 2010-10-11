package platform.client.logics;

import platform.client.descriptor.nodes.ComponentNode;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.tree.MutableTreeNode;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClientComponent implements Serializable, ClientIdentitySerializable {

    public int compID; // ID есть и у свойст и у объектов, так что чтобы не путаться

    public ComponentDesign design;

    public ClientContainer container;
    public SimplexConstraints<Integer> constraints;

    public boolean defaultComponent = false;

    public boolean show;

    public ClientComponent() {
    }

    public int getID() {
        return compID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);
        pool.writeObject(outStream, constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<Integer, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            outStream.writeInt(intersect.getKey());
            pool.writeObject(outStream, intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);
        outStream.writeBoolean(show);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        compID = iID;
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        constraints = pool.readObject(inStream);

        constraints.intersects = new HashMap<Integer, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            constraints.intersects.put(inStream.readInt(), (DoNotIntersectSimplexConstraint) pool.readObject(inStream));
        }
        constraints.ID = compID;

        defaultComponent = inStream.readBoolean();
        show = inStream.readBoolean();
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }
}
