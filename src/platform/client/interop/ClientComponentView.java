package platform.client.interop;

import platform.interop.form.layout.SimplexConstraints;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;

public class ClientComponentView implements Serializable {

    public ClientContainerView container;
    public SimplexConstraints constraints;

    public String outName = "";

    public ClientComponentView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {

        if(!inStream.readBoolean()) {
            int containerID = inStream.readInt();
            for(ClientContainerView parent : containers)
                if(parent.ID==containerID) {
                    container = parent;
                    break;
                }
        }

        constraints = (SimplexConstraints) new ObjectInputStream(inStream).readObject();
    }
}
