package platform.client.logics;

import platform.interop.form.layout.SimplexConstraints;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;

public class ClientComponentView implements Serializable {

    public int ID;
    public ClientContainerView container;
    public SimplexConstraints constraints;

    ClientComponentView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {

        ID = inStream.readInt();
        
        if(!inStream.readBoolean()) {
            int containerID = inStream.readInt();
            for(ClientContainerView parent : containers)
                if(parent.getID()==containerID) {
                    container = parent;
                    break;
                }
        }

        constraints = (SimplexConstraints) new ObjectInputStream(inStream).readObject();
    }
}
