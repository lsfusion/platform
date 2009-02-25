package platform.client.interop.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientDoubleClass extends ClientIntegralClass {

    public ClientDoubleClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Double.class;
    }
}
