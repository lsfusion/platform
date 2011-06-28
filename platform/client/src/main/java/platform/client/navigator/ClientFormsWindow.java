package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientFormsWindow extends ClientAbstractWindow {
    public ClientFormsWindow(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
