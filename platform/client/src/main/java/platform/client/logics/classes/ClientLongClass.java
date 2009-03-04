package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientLongClass extends ClientIntegralClass {

    public ClientLongClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Long.class;
    }
}
