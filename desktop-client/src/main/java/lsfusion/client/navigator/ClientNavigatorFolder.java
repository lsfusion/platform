package lsfusion.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorFolder extends ClientNavigatorElement {
    public ClientNavigatorFolder(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
