package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    public int ID;
    private String caption;
    public String toString() { return caption; }
    
    ClientObjectClass(DataInputStream inStream) throws IOException {
        super(inStream);
        caption = inStream.readUTF();
        ID = inStream.readInt();
        hasChilds = inStream.readBoolean();
    }

    public static ClientClass deserializeObject(DataInputStream inStream) throws IOException {
        boolean concrete = inStream.readBoolean();
        if(concrete)
            return new ClientConcreteClass(inStream);
        else
            return new ClientAbstractClass(inStream); 
    }

    private boolean hasChilds;
    public boolean hasChilds() {
        return hasChilds;
    }
}
