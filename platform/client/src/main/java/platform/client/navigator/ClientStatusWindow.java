package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientStatusWindow extends ClientAbstractWindow {
    public ClientStatusWindow(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
