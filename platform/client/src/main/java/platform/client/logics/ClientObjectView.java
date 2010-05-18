package platform.client.logics;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.awt.*;

public abstract class ClientObjectView extends ClientCellView {

    public ClientObjectImplementView object;

    ClientObjectView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView object) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        this.object = object;
    }

    public int getID() {
        return object.getID();
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return object.groupObject;
    }

    public int getMaximumWidth(JComponent comp) {
        return getPreferredWidth(comp);
    }

}
