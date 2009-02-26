package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientIntegerClass extends ClientIntegralClass {

    public ClientIntegerClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public Class getJavaClass() {
        return Integer.class;
    }
}
