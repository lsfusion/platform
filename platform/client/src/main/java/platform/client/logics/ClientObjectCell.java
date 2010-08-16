package platform.client.logics;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public abstract class ClientObjectCell extends ClientCell {

    public ClientObject object;

    ClientObjectCell(DataInputStream inStream, Collection<ClientContainer> containers, ClientObject object) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        this.object = object;
    }

    public int getID() {
        return object.getID();
    }

    public ClientGroupObject getGroupObject() {
        return object.groupObject;
    }

    public int getMaximumWidth(JComponent comp) {
        return getPreferredWidth(comp);
    }

}
