package lsfusion.client.classes;

import lsfusion.interop.classes.DataType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    private final long ID;

    private final boolean concreate;
    private final List<ClientObjectClass> children;
    private final String caption;

    private ClientObjectClass(long ID, String sID, String caption, boolean concreate, List<ClientObjectClass> children) {
        this.ID = ID;
        this.concreate = concreate;
        this.children = children;
        this.caption = caption;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(DataType.OBJECT);
        outStream.writeLong(ID);
    }

    public long getID() {
        return ID;
    }

    public boolean isConcreate() {
        return concreate;
    }

    public String getCaption() {
        return caption;
    }

    public List<ClientObjectClass> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean equals(Object o) {
        return this == o || o instanceof ClientObjectClass && ID == ((ClientObjectClass) o).ID;
    }

    @Override
    public int hashCode() {
        return ((Long)ID).hashCode();
    }

    public String toString() {
        return caption;
    }

    public static ClientObjectClass deserialize(DataInputStream inStream) throws IOException {
        boolean concreate = inStream.readBoolean();
        String caption = inStream.readUTF();
        long ID = inStream.readLong();
        String sID = inStream.readUTF();

        int count = inStream.readInt();
        List<ClientObjectClass> children = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            children.add(ClientTypeSerializer.deserializeClientObjectClass(inStream));
        }

        return new ClientObjectClass(ID, sID, caption, concreate, children);
    }
}
