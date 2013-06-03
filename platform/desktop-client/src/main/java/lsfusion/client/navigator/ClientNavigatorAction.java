package lsfusion.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorAction extends ClientNavigatorElement {
    public ClientNavigatorAction() {
    }

    public ClientNavigatorAction(int ID, String sID, String caption) {
        super(ID, sID, caption, false);
    }

    public ClientNavigatorAction(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
