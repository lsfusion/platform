package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    private final int ID;
    private final String sID;

    private final boolean concreate;
    private final List<ClientObjectClass> children = new ArrayList<ClientObjectClass>();
    private final String caption;

    ClientObjectClass(DataInputStream inStream) throws IOException {
        super(inStream);

        concreate = inStream.readBoolean();
        caption = inStream.readUTF();
        ID = inStream.readInt();
        sID = inStream.readUTF();

        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            children.add(ClientTypeSerializer.deserializeClientObjectClass(inStream));
        }
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeInt(ID);
    }

    public int getID() {
        return ID;
    }

    @Override
    public String getSID(){
        return sID;
    }

    public boolean isConcreate() {
        return concreate;
    }

    public String getCaption() {
        return caption;
    }

    @Override
    public String getCode() {
        return sID;
    }

    public List<ClientObjectClass> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ClientType getType() {
        return type;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof ClientObjectClass && ID == ((ClientObjectClass) o).ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    public String toString() {
        return caption;
    }

    public static ClientObjectClass deserialize(DataInputStream inStream) throws IOException {
        return new ClientObjectClass(inStream);
    }
}
