package platform.client.logics;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

public class ClientContainerView extends ClientComponentView {

    int ID;

    public String title;

    public ClientContainerView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        if(!inStream.readBoolean())
            title = inStream.readUTF();
    }
}
