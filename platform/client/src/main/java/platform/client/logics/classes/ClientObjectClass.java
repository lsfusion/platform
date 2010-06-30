package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    public int ID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientObjectClass)) return false;

        ClientObjectClass that = (ClientObjectClass) o;

        if (ID != that.ID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    private String caption;
    public String toString() { return caption; }

    private List<ClientClass> children = new ArrayList<ClientClass>();
    public List<ClientClass> getChildren() {
        return children;
    }

    ClientObjectClass(DataInputStream inStream) throws IOException {
        super(inStream);
        caption = inStream.readUTF();
        ID = inStream.readInt();

        int count = inStream.readByte();
        for (int i = 0; i < count; i++) {
            children.add(ClientClass.deserialize(inStream));
        }
    }

    public static ClientObjectClass deserializeObject(DataInputStream inStream) throws IOException {
        boolean concrete = inStream.readBoolean();
        if(concrete)
            return new ClientConcreteClass(inStream);
        else
            return new ClientAbstractClass(inStream); 
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
