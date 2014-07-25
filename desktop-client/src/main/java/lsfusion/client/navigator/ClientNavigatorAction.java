package lsfusion.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorAction extends ClientNavigatorElement {

    public ClientNavigatorAction(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
