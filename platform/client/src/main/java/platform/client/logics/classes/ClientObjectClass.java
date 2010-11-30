package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    public int ID;
    
    private String sID;

    @Override
    public String getSID(){
        return sID;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClientObjectClass && ID == ((ClientObjectClass) o).ID;
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
        sID = inStream.readUTF(); 

        int count = inStream.readByte();
        for (int i = 0; i < count; i++) {
            children.add(ClientTypeSerializer.deserializeClientClass(inStream));
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

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeInt(ID);
    }

    public ClientType getType() {
        return type;
    }
}
