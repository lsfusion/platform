package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientConcreteClass extends ClientObjectClass {

    public ClientConcreteClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }
}
