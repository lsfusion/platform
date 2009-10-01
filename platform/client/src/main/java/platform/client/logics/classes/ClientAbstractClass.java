package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientAbstractClass extends ClientObjectClass {

    public ClientAbstractClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
