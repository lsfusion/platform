package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientLogWindow extends ClientAbstractWindow {
    public ClientLogWindow(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
